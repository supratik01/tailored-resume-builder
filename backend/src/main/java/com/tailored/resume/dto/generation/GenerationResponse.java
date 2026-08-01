package com.tailored.resume.dto.generation;

import com.tailored.resume.dto.resume.ParsedResume;

import java.time.Instant;
import java.util.UUID;

/** @param baselineScore the source resume's overall score against the same posting, before
 *                        tailoring. Null for generations scored before this field existed. */
public record GenerationResponse(
        UUID id,
        UUID resumeId,
        ParsedResume tailored,
        AtsScore ats,
        Integer baselineScore,
        Instant createdAt
) {}
