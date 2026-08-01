package com.tailored.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailored.resume.ai.AiClient;
import com.tailored.resume.ai.AiResponses;
import com.tailored.resume.ai.PromptBuilder;
import com.tailored.resume.dto.generation.AtsScore;
import com.tailored.resume.dto.generation.GenerateRequest;
import com.tailored.resume.dto.generation.GenerationResponse;
import com.tailored.resume.dto.generation.GenerationSummary;
import com.tailored.resume.dto.resume.ParsedResume;
import com.tailored.resume.entity.Generation;
import com.tailored.resume.entity.JobDescription;
import com.tailored.resume.entity.Resume;
import com.tailored.resume.exception.AiServiceException;
import com.tailored.resume.exception.NotFoundException;
import com.tailored.resume.repository.GenerationRepository;
import com.tailored.resume.repository.JobDescriptionRepository;
import com.tailored.resume.scoring.AtsScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GenerationService {

    private final ResumeService resumeService;
    private final QuotaService quotaService;
    private final JobDescriptionRepository jdRepository;
    private final GenerationRepository generationRepository;
    private final AiClient aiClient;
    private final PromptBuilder promptBuilder;
    private final AtsScoringService scoringService;
    private final ObjectMapper mapper;

    @Transactional
    public GenerationResponse generate(UUID userId, GenerateRequest req) {
        quotaService.assertCanGenerate(userId);

        Resume resume = resumeService.loadOwned(userId, req.resumeId());
        ParsedResume source = resumeService.parsedFor(resume);

        JobDescription jd = jdRepository.save(JobDescription.builder()
                .userId(userId)
                .title(req.jobTitle())
                .company(req.company())
                .rawText(req.jobDescription())
                .build());

        ParsedResume tailored = invokeAi(source, req.jobDescription(), resume.getRawText());
        AtsScore ats = scoringService.score(tailored, req.jobDescription());
        // Same formula, run against the untouched source, so the results screen can show a
        // real before/after rather than inventing one.
        int baseline = scoringService.score(source, req.jobDescription()).overall();

        Generation gen = Generation.builder()
                .userId(userId)
                .resumeId(resume.getId())
                .jobDescriptionId(jd.getId())
                .tailoredJson(toJson(tailored))
                .atsAnalysisJson(toJson(ats))
                .atsScore(ats.overall())
                .baselineScore(baseline)
                .build();
        gen = generationRepository.save(gen);

        return new GenerationResponse(gen.getId(), resume.getId(), tailored, ats, baseline, gen.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<GenerationSummary> list(UUID userId) {
        return generationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(g -> {
                    JobDescription jd = jdRepository.findById(g.getJobDescriptionId()).orElse(null);
                    return new GenerationSummary(
                            g.getId(),
                            g.getResumeId(),
                            jd == null ? null : jd.getTitle(),
                            jd == null ? null : jd.getCompany(),
                            g.getAtsScore(),
                            g.getCreatedAt());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public GenerationResponse get(UUID userId, UUID id) {
        Generation g = loadOwned(userId, id);
        return new GenerationResponse(g.getId(), g.getResumeId(), fromTailored(g), fromAts(g), g.getBaselineScore(), g.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public ParsedResume getTailoredForExport(UUID userId, UUID id) {
        return fromTailored(loadOwned(userId, id));
    }

    private Generation loadOwned(UUID userId, UUID id) {
        return generationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Generation not found"));
    }

    private ParsedResume invokeAi(ParsedResume source, String jd, String rawText) {
        String system = promptBuilder.systemPrompt();
        String user = promptBuilder.tailoringUserPrompt(source, jd, rawText);
        String raw = aiClient.complete(system, user);
        try {
            return mapper.readValue(AiResponses.extractJson(raw), ParsedResume.class);
        } catch (JsonProcessingException e) {
            throw new AiServiceException("AI returned malformed JSON: " + e.getOriginalMessage(), e);
        }
    }

    private String toJson(Object o) {
        try { return mapper.writeValueAsString(o); }
        catch (JsonProcessingException e) { throw new AiServiceException("Failed to serialize generation"); }
    }

    private ParsedResume fromTailored(Generation g) {
        try { return mapper.readValue(g.getTailoredJson(), ParsedResume.class); }
        catch (JsonProcessingException e) { throw new AiServiceException("Stored tailored JSON is corrupt"); }
    }

    private AtsScore fromAts(Generation g) {
        try { return mapper.readValue(g.getAtsAnalysisJson(), AtsScore.class); }
        catch (JsonProcessingException e) { throw new AiServiceException("Stored ATS JSON is corrupt"); }
    }
}
