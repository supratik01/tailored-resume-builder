package com.tailored.resume.service;

import com.tailored.resume.config.AppProperties;
import com.tailored.resume.dto.billing.UsageResponse;
import com.tailored.resume.entity.User;
import com.tailored.resume.exception.QuotaExceededException;
import com.tailored.resume.repository.GenerationRepository;
import com.tailored.resume.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QuotaServiceTest {

    private final UUID userId = UUID.randomUUID();
    private final UserRepository userRepository = mock(UserRepository.class);
    private final GenerationRepository generationRepository = mock(GenerationRepository.class);

    private QuotaService service(User.Plan plan, int limit, long usedThisMonth) {
        AppProperties props = new AppProperties();
        props.getQuota().setFreeRunsPerMonth(limit);
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(User.builder().id(userId).email("a@b.com")
                        .passwordHash("x").fullName("A B").plan(plan).build()));
        when(generationRepository.countByUserIdAndCreatedAtGreaterThanEqual(eq(userId), any(Instant.class)))
                .thenReturn(usedThisMonth);
        return new QuotaService(props, userRepository, generationRepository);
    }

    @Test
    void freeUserBelowLimitMayGenerate() {
        QuotaService svc = service(User.Plan.FREE, 3, 1);

        UsageResponse usage = svc.usage(userId);
        assertThat(usage.plan()).isEqualTo("FREE");
        assertThat(usage.used()).isEqualTo(1);
        assertThat(usage.limit()).isEqualTo(3);
        assertThat(usage.remaining()).isEqualTo(2);
        assertThatCode(() -> svc.assertCanGenerate(userId)).doesNotThrowAnyException();
    }

    @Test
    void freeUserAtLimitIsBlocked() {
        QuotaService svc = service(User.Plan.FREE, 3, 3);

        assertThat(svc.usage(userId).remaining()).isZero();
        assertThatThrownBy(() -> svc.assertCanGenerate(userId))
                .isInstanceOf(QuotaExceededException.class)
                .hasMessageContaining("3 free tailoring runs");
    }

    @Test
    void remainingNeverGoesNegativeIfLimitWasLowered() {
        QuotaService svc = service(User.Plan.FREE, 2, 5);

        assertThat(svc.usage(userId).remaining()).isZero();
        assertThatThrownBy(() -> svc.assertCanGenerate(userId)).isInstanceOf(QuotaExceededException.class);
    }

    @Test
    void paidUserIsUnmeteredButStillCounted() {
        QuotaService svc = service(User.Plan.PRO, 3, 42);

        UsageResponse usage = svc.usage(userId);
        assertThat(usage.plan()).isEqualTo("PRO");
        assertThat(usage.used()).isEqualTo(42);
        assertThat(usage.limit()).isNull();
        assertThat(usage.remaining()).isNull();
        assertThatCode(() -> svc.assertCanGenerate(userId)).doesNotThrowAnyException();
    }

    @Test
    void resetInstantIsInTheFuture() {
        assertThat(service(User.Plan.FREE, 3, 0).usage(userId).resetsAt()).isAfter(Instant.now());
    }
}
