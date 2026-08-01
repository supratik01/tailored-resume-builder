package com.tailored.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailored.resume.dto.resume.ParsedResume;
import com.tailored.resume.dto.resume.ResumeResponse;
import com.tailored.resume.entity.Resume;
import com.tailored.resume.exception.BadRequestException;
import com.tailored.resume.exception.NotFoundException;
import com.tailored.resume.parser.ResumeStructureExtractor;
import com.tailored.resume.parser.TextExtractor;
import com.tailored.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final TextExtractor textExtractor;
    private final ResumeStructureExtractor structureExtractor;
    private final ObjectMapper mapper;

    @Transactional
    public ResumeResponse upload(UUID userId, MultipartFile file) {
        TextExtractor.Extracted extracted = textExtractor.extract(file);
        if (extracted.text().length() < 100) {
            throw new BadRequestException("Could not extract enough text from the file. Is it scanned or empty?");
        }
        ParsedResume parsed = structureExtractor.extract(extracted.text());
        Resume resume = Resume.builder()
                .userId(userId)
                .originalFilename(extracted.originalFilename())
                .fileType(extracted.type())
                .rawText(extracted.text())
                .parsedJson(toJson(parsed))
                .build();
        resume = resumeRepository.save(resume);
        return toResponse(resume, parsed);
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> list(UUID userId) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(r -> toResponse(r, fromJson(r.getParsedJson())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeResponse get(UUID userId, UUID id) {
        Resume r = resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
        return toResponse(r, fromJson(r.getParsedJson()));
    }

    Resume loadOwned(UUID userId, UUID resumeId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new NotFoundException("Resume not found"));
    }

    ParsedResume parsedFor(Resume resume) {
        return fromJson(resume.getParsedJson());
    }

    private ResumeResponse toResponse(Resume r, ParsedResume parsed) {
        return new ResumeResponse(r.getId(), r.getOriginalFilename(), r.getFileType().name(), parsed, r.getCreatedAt());
    }

    private String toJson(Object o) {
        try { return mapper.writeValueAsString(o); }
        catch (JsonProcessingException e) { throw new BadRequestException("Failed to serialize parsed resume"); }
    }

    private ParsedResume fromJson(String s) {
        try { return s == null ? null : mapper.readValue(s, ParsedResume.class); }
        catch (JsonProcessingException e) { throw new BadRequestException("Stored resume JSON is corrupt"); }
    }
}
