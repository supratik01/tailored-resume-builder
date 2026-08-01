package com.tailored.resume.dto.billing;

import java.time.Instant;

/**
 * @param plan      FREE or PRO
 * @param used      tailoring runs started in the current calendar month
 * @param limit     runs allowed this month, or null when the plan is unlimited
 * @param remaining runs left, or null when the plan is unlimited
 * @param resetsAt  instant the current month's counter rolls over
 */
public record UsageResponse(
        String plan,
        long used,
        Integer limit,
        Integer remaining,
        Instant resetsAt
) {}
