package com.tailored.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailored.resume.ai.AiClient;
import com.tailored.resume.ai.PromptBuilder;
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
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CoverLetterServiceTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID generationId = UUID.randomUUID();
    private final UUID jdId = UUID.randomUUID();

    private final GenerationRepository generationRepository = mock(GenerationRepository.class);
    private final JobDescriptionRepository jdRepository = mock(JobDescriptionRepository.class);
    private final CoverLetterRepository coverLetterRepository = mock(CoverLetterRepository.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final PromptBuilder promptBuilder = new PromptBuilder(mapper);

    private CoverLetterService serviceReturning(String rawAiOutput) {
        AiClient ai = (system, user) -> rawAiOutput;
        return new CoverLetterService(
                generationRepository, jdRepository, coverLetterRepository, ai, promptBuilder, mapper);
    }

    /** Returns each canned output in turn, so the correction retry can be exercised. */
    private CoverLetterService serviceReturningInTurn(String... rawAiOutputs) {
        AtomicInteger call = new AtomicInteger();
        AiClient ai = (system, user) ->
                rawAiOutputs[Math.min(call.getAndIncrement(), rawAiOutputs.length - 1)];
        return new CoverLetterService(
                generationRepository, jdRepository, coverLetterRepository, ai, promptBuilder, mapper);
    }

    private void givenOwnedGeneration() {
        String tailoredJson = """
                {"contact":{"fullName":"Jane Doe","email":"jane@example.com"},
                 "summary":"Backend engineer.","skills":["Java"],
                 "experience":[],"education":[],"projects":[],"certifications":[]}
                """;
        when(generationRepository.findByIdAndUserId(generationId, userId))
                .thenReturn(Optional.of(Generation.builder()
                        .id(generationId).userId(userId).resumeId(UUID.randomUUID())
                        .jobDescriptionId(jdId).tailoredJson(tailoredJson).atsAnalysisJson("{}").atsScore(72)
                        .build()));
        when(jdRepository.findById(jdId))
                .thenReturn(Optional.of(JobDescription.builder()
                        .id(jdId).userId(userId).title("Backend Engineer").company("Acme")
                        .rawText("We need a Java engineer fluent in Spring Boot.")
                        .build()));
        when(coverLetterRepository.save(any(CoverLetter.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void generateParsesLetterBodyAndDefaultsTone() {
        givenOwnedGeneration();
        when(coverLetterRepository.findByGenerationIdAndUserId(generationId, userId)).thenReturn(Optional.empty());

        CoverLetterResponse res = serviceReturning("{\"body\":\"Dear Hiring Team,\\n\\nI build backends.\"}")
                .generate(userId, generationId, null);

        assertThat(res.body()).startsWith("Dear Hiring Team,").contains("I build backends.");
        assertThat(res.tone()).isEqualTo("professional");
        assertThat(res.generationId()).isEqualTo(generationId);
    }

    @Test
    void generateOverwritesExistingLetterForSameGeneration() {
        givenOwnedGeneration();
        CoverLetter existing = CoverLetter.builder()
                .id(UUID.randomUUID()).userId(userId).generationId(generationId)
                .body("Old letter.").tone("professional").build();
        when(coverLetterRepository.findByGenerationIdAndUserId(generationId, userId)).thenReturn(Optional.of(existing));

        CoverLetterResponse res = serviceReturning("{\"body\":\"Fresh letter.\"}")
                .generate(userId, generationId, new CoverLetterRequest("direct", null));

        assertThat(res.id()).isEqualTo(existing.getId());
        assertThat(res.body()).isEqualTo("Fresh letter.");
        assertThat(res.tone()).isEqualTo("direct");
        verify(coverLetterRepository).save(existing);
    }

    @Test
    void generateRejectsEmptyModelOutput() {
        givenOwnedGeneration();
        when(coverLetterRepository.findByGenerationIdAndUserId(generationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceReturning("{\"body\":\"   \"}").generate(userId, generationId, null))
                .isInstanceOf(AiServiceException.class)
                .hasMessageContaining("empty cover letter");
    }

    @Test
    void generateStripsMarkdownFencesAroundJson() {
        givenOwnedGeneration();
        when(coverLetterRepository.findByGenerationIdAndUserId(generationId, userId)).thenReturn(Optional.empty());

        CoverLetterResponse res = serviceReturning("```json\n{\"body\":\"Fenced letter.\"}\n```")
                .generate(userId, generationId, null);

        assertThat(res.body()).isEqualTo("Fenced letter.");
    }

    @Test
    void generateRejectsGenerationOwnedBySomeoneElse() {
        when(generationRepository.findByIdAndUserId(generationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceReturning("{\"body\":\"x\"}").generate(userId, generationId, null))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(coverLetterRepository);
    }

    @Test
    void retriesOnceWhenLetterCitesSomethingTheResumeLacks() {
        givenOwnedGeneration();
        when(coverLetterRepository.findByGenerationIdAndUserId(generationId, userId)).thenReturn(Optional.empty());

        CoverLetterResponse res = serviceReturningInTurn(
                "{\"body\":\"I ran services on AWS for years.\"}",
                "{\"body\":\"I ran services in Java for years.\"}")
                .generate(userId, generationId, null);

        assertThat(res.body()).isEqualTo("I ran services in Java for years.");
        assertThat(res.unsupportedTerms()).isEmpty();
    }

    @Test
    void reportsTermsThatSurviveTheCorrectionRetry() {
        givenOwnedGeneration();
        when(coverLetterRepository.findByGenerationIdAndUserId(generationId, userId)).thenReturn(Optional.empty());

        CoverLetterResponse res = serviceReturning("{\"body\":\"I ran services on AWS and Kubernetes.\"}")
                .generate(userId, generationId, null);

        assertThat(res.unsupportedTerms()).containsExactly("AWS", "Kubernetes");
    }

    @Test
    void getThrowsWhenNoLetterExistsYet() {
        when(coverLetterRepository.findByGenerationIdAndUserId(generationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceReturning("{}").get(userId, generationId))
                .isInstanceOf(NotFoundException.class);
    }
}
