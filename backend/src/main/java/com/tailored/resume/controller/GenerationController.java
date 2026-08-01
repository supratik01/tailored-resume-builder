package com.tailored.resume.controller;

import com.tailored.resume.dto.coverletter.CoverLetterRequest;
import com.tailored.resume.dto.coverletter.CoverLetterResponse;
import com.tailored.resume.dto.generation.GenerateRequest;
import com.tailored.resume.dto.generation.GenerationResponse;
import com.tailored.resume.dto.generation.GenerationSummary;
import com.tailored.resume.dto.resume.ParsedResume;
import com.tailored.resume.export.DocxExporter;
import com.tailored.resume.export.PdfExporter;
import com.tailored.resume.security.CurrentUser;
import com.tailored.resume.service.CoverLetterService;
import com.tailored.resume.service.GenerationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/generations")
@RequiredArgsConstructor
@Tag(name = "Generations")
public class GenerationController {

    private final GenerationService generationService;
    private final CoverLetterService coverLetterService;
    private final PdfExporter pdfExporter;
    private final DocxExporter docxExporter;

    @PostMapping
    public ResponseEntity<GenerationResponse> create(@Valid @RequestBody GenerateRequest req) {
        return ResponseEntity.ok(generationService.generate(CurrentUser.id(), req));
    }

    @GetMapping
    public ResponseEntity<List<GenerationSummary>> list() {
        return ResponseEntity.ok(generationService.list(CurrentUser.id()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GenerationResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(generationService.get(CurrentUser.id(), id));
    }

    @PostMapping("/{id}/cover-letter")
    public ResponseEntity<CoverLetterResponse> generateCoverLetter(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) CoverLetterRequest req) {
        return ResponseEntity.ok(coverLetterService.generate(CurrentUser.id(), id, req));
    }

    @GetMapping("/{id}/cover-letter")
    public ResponseEntity<CoverLetterResponse> getCoverLetter(@PathVariable UUID id) {
        return ResponseEntity.ok(coverLetterService.get(CurrentUser.id(), id));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        ParsedResume resume = generationService.getTailoredForExport(CurrentUser.id(), id);
        byte[] bytes = pdfExporter.export(resume);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resume-" + id + ".pdf\"")
                .body(bytes);
    }

    @GetMapping("/{id}/docx")
    public ResponseEntity<byte[]> downloadDocx(@PathVariable UUID id) {
        ParsedResume resume = generationService.getTailoredForExport(CurrentUser.id(), id);
        byte[] bytes = docxExporter.export(resume);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resume-" + id + ".docx\"")
                .body(bytes);
    }
}
