package com.tailored.resume.service;

import com.tailored.resume.config.AppProperties;
import com.tailored.resume.dto.billing.UsageResponse;
import com.tailored.resume.entity.User;
import com.tailored.resume.exception.NotFoundException;
import com.tailored.resume.exception.QuotaExceededException;
import com.tailored.resume.repository.GenerationRepository;
import com.tailored.resume.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Free-tier metering. A "run" is one tailoring generation; cover letters written against an
 * existing generation are not metered separately, since the run they belong to was already spent.
 */
@Service
@RequiredArgsConstructor
public class QuotaService {

    private final AppProperties props;
    private final UserRepository userRepository;
    private final GenerationRepository generationRepository;

    @Transactional(readOnly = true)
    public void assertCanGenerate(UUID userId) {
        UsageResponse usage = usage(userId);
        if (usage.remaining() != null && usage.remaining() <= 0) {
            throw new QuotaExceededException(
                    "You've used all %d free tailoring runs this month. Upgrade to keep going."
                            .formatted(usage.limit()));
        }
    }

    @Transactional(readOnly = true)
    public UsageResponse usage(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        ZoneId zone = ZoneId.of(props.getQuota().getZone());
        Instant monthStart = YearMonth.now(zone).atDay(1).atStartOfDay(zone).toInstant();
        Instant monthEnd = YearMonth.now(zone).plusMonths(1).atDay(1).atStartOfDay(zone).toInstant();

        long used = generationRepository.countByUserIdAndCreatedAtGreaterThanEqual(userId, monthStart);

        if (user.getPlan() == User.Plan.PRO) {
            return new UsageResponse("PRO", used, null, null, monthEnd);
        }
        int limit = props.getQuota().getFreeRunsPerMonth();
        return new UsageResponse("FREE", used, limit, (int) Math.max(0, limit - used), monthEnd);
    }
}
