package com.tailored.resume.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailored.resume.config.AppProperties;
import com.tailored.resume.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/** Thin wrapper over the Razorpay REST API. Only the calls v1 needs. */
@Component
@Slf4j
public class RazorpayClient {

    private final AppProperties.Razorpay cfg;
    private final WebClient webClient;
    private final ObjectMapper mapper;

    public RazorpayClient(AppProperties props, ObjectMapper mapper) {
        this.cfg = props.getRazorpay();
        this.mapper = mapper;
        String basic = Base64.getEncoder().encodeToString(
                (cfg.getKeyId() + ":" + cfg.getKeySecret()).getBytes(StandardCharsets.UTF_8));
        this.webClient = WebClient.builder()
                .baseUrl(cfg.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Creates a monthly subscription against the configured plan.
     *
     * @return the Razorpay subscription id, which the browser hands to Checkout
     */
    public String createSubscription(String userEmail) {
        if (!cfg.isConfigured()) {
            throw new BadRequestException("Billing is not configured on this server yet");
        }
        Map<String, Object> body = Map.of(
                "plan_id", cfg.getPlanId(),
                "total_count", 12,
                "customer_notify", 1,
                "notes", Map.of("email", userEmail));
        try {
            String raw = webClient.post()
                    .uri("/subscriptions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(cfg.getTimeoutSeconds()))
                    .block();
            JsonNode node = mapper.readTree(raw == null ? "{}" : raw);
            String id = node.path("id").asText("");
            if (id.isEmpty()) {
                throw new BadRequestException("Razorpay did not return a subscription id");
            }
            return id;
        } catch (WebClientResponseException e) {
            log.error("Razorpay subscription create failed: status={} body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BadRequestException("Could not start checkout. Please try again.");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Razorpay subscription create failed", e);
            throw new BadRequestException("Could not start checkout. Please try again.");
        }
    }
}
