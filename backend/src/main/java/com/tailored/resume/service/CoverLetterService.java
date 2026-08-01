package com.tailored.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailored.resume.ai.AiClient;
import com.tailored.resume.ai.AiResponses;
import com.tailored.resume.ai.PromptBuilder;
import com.tailored.resume.ai.UnsupportedTermDetector;
import com.tailored.resume.dto.coverletter.CoverLetterRequest;
import com.tailored.resume.dto.coverletter.CoverLetterResponse;
import com.tailored.resume.dto.resume.ParsedResume;
import com.tailored.resume.entity.CoverLetter;
import com.tailored.resume.entity.Generation;
import com.tailored.resume.entity.JobDescription;
import com.tailored.resume.exception.AiServiceException;
import com.tailored.resume.exception.NotFoundException;
import com.tailored.resume.repository.CoverLetterRepository;
import com.tailored.resume.repository.GenerationRepository;
import com.tailored.resume.repository.JobDescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoverLetterService {

    private final GenerationRepository generationRepository;
    private final JobDescriptionRepository jdRepository;
    private final CoverLetterRepository coverLetterRepository;
    private final AiClient aiClient;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper mapper;

    /** Generates a letter for a generation the caller owns, replacing any previous one. */
    @Transactional
    public CoverLetterResponse generate(UUID userId, UUID generationId, CoverLetterRequest req) {
        Generation gen = generationRepository.findByIdAndUserId(generationId, userId)
                .orElseThrow(() -> new NotFoundException("Generation not found"));
        JobDescription jd = jdRepository.findById(gen.getJobDescriptionId())
                .orElseThrow(() -> new NotFoundException("Job description not found"));
        ParsedResume tailored = readTailored(gen);

        CoverLetterRequest safeReq = req == null ? new CoverLetterRequest(null, null) : req;
        String tone = safeReq.toneOrDefault();

        String body = invokeAi(tailored, jd, tone, safeReq.notes());

        // The model reliably borrows technologies from the job description and attributes them
        // to the candidate. Prompting alone did not stop it, so re-ask once with the specifics.
        String evidence = gen.getTailoredJson();
        List<String> unsupported = UnsupportedTermDetector.find(body, evidence);
        if (!unsupported.isEmpty()) {
            log.info("Cover letter cited unsupported terms {}, requesting correction", unsupported);
            String corrected = retryAi(body, unsupported);
            body = corrected;
            unsupported = UnsupportedTermDetector.find(body, evidence);
            if (!unsupported.isEmpty()) {
                log.warn("Cover letter still cites unsupported terms after correction: {}", unsupported);
            }
        }

        CoverLetter letter = coverLetterRepository.findByGenerationIdAndUserId(generationId, userId)
                .orElseGet(() -> CoverLetter.builder()
                        .userId(userId)
                        .generationId(generationId)
                        .build());
        letter.setBody(body);
        letter.setTone(tone);
        letter.setUnsupportedTerms(unsupported.isEmpty() ? null : String.join(",", unsupported));

        return toResponse(coverLetterRepository.save(letter));
    }

    @Transactional(readOnly = true)
    public CoverLetterResponse get(UUID userId, UUID generationId) {
        return coverLetterRepository.findByGenerationIdAndUserId(generationId, userId)
                .map(CoverLetterService::toResponse)
                .orElseThrow(() -> new NotFoundException("No cover letter for this generation yet"));
    }

    private String retryAi(String previousBody, List<String> unsupported) {
        String raw = aiClient.complete(
                promptBuilder.coverLetterSystemPrompt(),
                promptBuilder.coverLetterCorrectionPrompt(previousBody, unsupported));
        return parseBody(raw);
    }

    private ParsedResume readTailored(Generation gen) {
        try {
            return mapper.readValue(gen.getTailoredJson(), ParsedResume.class);
        } catch (JsonProcessingException e) {
            throw new AiServiceException("Stored tailored JSON is corrupt", e);
        }
    }

    private String invokeAi(ParsedResume tailored, JobDescription jd, String tone, String notes) {
        String raw = aiClient.complete(
                promptBuilder.coverLetterSystemPrompt(),
                promptBuilder.coverLetterUserPrompt(
                        tailored, jd.getRawText(), jd.getTitle(), jd.getCompany(), tone, notes));
        return parseBody(raw);
    }

    private String parseBody(String raw) {
        try {
            JsonNode node = mapper.readTree(AiResponses.extractJson(raw));
            String body = node.path("body").asText("").strip();
            if (body.isEmpty()) {
                throw new AiServiceException("AI returned an empty cover letter");
            }
            return body;
        } catch (JsonProcessingException e) {
            throw new AiServiceException("AI returned malformed JSON: " + e.getOriginalMessage(), e);
        }
    }

    private static CoverLetterResponse toResponse(CoverLetter c) {
        String terms = c.getUnsupportedTerms();
        return new CoverLetterResponse(
                c.getId(), c.getGenerationId(), c.getBody(), c.getTone(),
                terms == null || terms.isBlank() ? List.of() : List.of(terms.split(",")),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}
