package com.tailored.resume.controller;

import com.tailored.resume.dto.resume.ResumeResponse;
import com.tailored.resume.security.CurrentUser;
import com.tailored.resume.service.ResumeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
@RequiredArgsConstructor
@Tag(name = "Resumes")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ResumeResponse> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(resumeService.upload(CurrentUser.id(), file));
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponse>> list() {
        return ResponseEntity.ok(resumeService.list(CurrentUser.id()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(resumeService.get(CurrentUser.id(), id));
    }
}
