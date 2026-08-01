package com.tailored.resume.dto.resume;

import java.time.Instant;
import java.util.UUID;

public record ResumeResponse(
        UUID id,
        String originalFilename,
        String fileType,
        ParsedResume parsed,
        Instant createdAt
) {}
