package com.tailored.resume.dto.generation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GenerateRequest(
        @NotNull UUID resumeId,
        @NotBlank @Size(min = 50, max = 20000) String jobDescription,
        String jobTitle,
        String company
) {}
