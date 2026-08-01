package com.tailored.resume.dto.generation;

import java.time.Instant;
import java.util.UUID;

public record GenerationSummary(
        UUID id,
        UUID resumeId,
        String jobTitle,
        String company,
        int atsScore,
        Instant createdAt
) {}
