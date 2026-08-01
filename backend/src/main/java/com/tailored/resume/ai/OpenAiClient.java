package com.tailored.resume.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailored.resume.config.AppProperties;
import com.tailored.resume.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class OpenAiClient implements AiClient {

    private static final int MAX_RETRIES = 3;
    private static final Duration RETRY_BACKOFF = Duration.ofSeconds(5);

    private final AppProperties.OpenAi cfg;
    private final WebClient webClient;
    private final ObjectMapper mapper;

    public OpenAiClient(AppProperties props, ObjectMapper mapper) {
        this.cfg = props.getOpenai();
        this.mapper = mapper;
        this.webClient = WebClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + cfg.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(8 * 1024 * 1024))
                .build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new AiServiceException("OPENAI_API_KEY is not configured");
        }
        Map<String, Object> body = Map.of(
                "model", cfg.getModel(),
                "temperature", 0.4,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );
        try {
            String raw = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(cfg.getTimeoutSeconds()))
                    .retryWhen(Retry.backoff(MAX_RETRIES, RETRY_BACKOFF)
                            .jitter(0.3)
                            .filter(this::isRetryable)
                            .doBeforeRetry(signal -> log.warn(
                                    "OpenAI rate-limited, retrying (attempt {}/{})",
                                    signal.totalRetries() + 1, MAX_RETRIES)))
                    .block();

            if (raw == null) throw new AiServiceException("Empty response from OpenAI");
            JsonNode root = mapper.readTree(raw);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new AiServiceException("Malformed OpenAI response: " + raw);
            }
            return content.asText();
        } catch (WebClientResponseException e) {
            log.error("OpenAI API error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiServiceException("OpenAI request failed: " + e.getStatusCode(), e);
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            // Unwrap reactor RetryExhaustedException
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof WebClientResponseException wce) {
                throw new AiServiceException("OpenAI request failed after retries: " + wce.getStatusCode(), wce);
            }
            throw new AiServiceException("OpenAI request failed: " + e.getMessage(), e);
        }
    }

    private boolean isRetryable(Throwable t) {
        if (t instanceof WebClientResponseException wce) {
            HttpStatus status = HttpStatus.resolve(wce.getStatusCode().value());
            // Retry on 429 (rate limit) and 5xx (server errors)
            return status == HttpStatus.TOO_MANY_REQUESTS || (status != null && status.is5xxServerError());
        }
        return false;
    }
}
