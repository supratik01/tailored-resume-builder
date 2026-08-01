package com.tailored.resume.dto.coverletter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @param unsupportedTerms things the letter claims that the resume does not back up. Empty in the
 *                         normal case; non-empty means the UI should ask the candidate to check.
 */
public record CoverLetterResponse(
        UUID id,
        UUID generationId,
        String body,
        String tone,
        List<String> unsupportedTerms,
        Instant createdAt,
        Instant updatedAt
) {}
