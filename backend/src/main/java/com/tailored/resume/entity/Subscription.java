package com.tailored.resume.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Mirrors a Razorpay subscription. The source of truth is Razorpay; this row is what the
 * app reads so a page load does not need an API round trip.
 */
@Entity
@Table(name = "subscriptions", indexes = {
        @Index(columnList = "user_id"),
        @Index(columnList = "razorpay_subscription_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "razorpay_subscription_id", nullable = false, unique = true)
    private String razorpaySubscriptionId;

    /** Razorpay's own status string: created, authenticated, active, halted, cancelled, completed. */
    @Column(nullable = false)
    private String status;

    /** End of the paid period, when Razorpay tells us. Null before the first charge. */
    private Instant currentPeriodEnd;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** Statuses that entitle the user to PRO access. */
    public static boolean grantsAccess(String status) {
        return "active".equals(status) || "authenticated".equals(status);
    }
}
