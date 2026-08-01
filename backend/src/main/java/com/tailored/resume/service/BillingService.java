package com.tailored.resume.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailored.resume.billing.RazorpayClient;
import com.tailored.resume.billing.RazorpaySignature;
import com.tailored.resume.config.AppProperties;
import com.tailored.resume.dto.billing.CheckoutSessionResponse;
import com.tailored.resume.entity.Subscription;
import com.tailored.resume.entity.User;
import com.tailored.resume.exception.NotFoundException;
import com.tailored.resume.exception.UnauthorizedException;
import com.tailored.resume.repository.SubscriptionRepository;
import com.tailored.resume.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Subscription lifecycle. Checkout starts here; entitlement changes only ever come from a
 * signed Razorpay webhook, never from the browser telling us a payment succeeded.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final AppProperties props;
    private final RazorpayClient razorpayClient;
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ObjectMapper mapper;

    @Transactional
    public CheckoutSessionResponse startCheckout(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        String subscriptionId = razorpayClient.createSubscription(user.getEmail());

        subscriptionRepository.save(Subscription.builder()
                .userId(userId)
                .razorpaySubscriptionId(subscriptionId)
                .status("created")
                .build());

        return new CheckoutSessionResponse(
                subscriptionId,
                props.getRazorpay().getKeyId(),
                49900,
                "INR",
                user.getEmail(),
                user.getFullName());
    }

    /**
     * Applies a webhook. Rejects anything not signed with the configured secret.
     *
     * @param rawBody   the exact bytes Razorpay sent — re-serializing breaks the signature
     * @param signature value of the X-Razorpay-Signature header
     */
    @Transactional
    public void handleWebhook(String rawBody, String signature) {
        String secret = props.getRazorpay().getWebhookSecret();
        if (!RazorpaySignature.isValid(rawBody, secret, signature)) {
            log.warn("Rejected Razorpay webhook with invalid signature");
            throw new UnauthorizedException("Invalid webhook signature");
        }

        JsonNode root;
        try {
            root = mapper.readTree(rawBody);
        } catch (Exception e) {
            log.warn("Razorpay webhook body was not JSON");
            return;
        }

        String event = root.path("event").asText("");
        JsonNode sub = root.path("payload").path("subscription").path("entity");
        String subscriptionId = sub.path("id").asText("");
        if (subscriptionId.isEmpty()) {
            log.info("Ignoring Razorpay event {} with no subscription payload", event);
            return;
        }

        Optional<Subscription> existing = subscriptionRepository.findByRazorpaySubscriptionId(subscriptionId);
        if (existing.isEmpty()) {
            log.warn("Razorpay event {} for unknown subscription {}", event, subscriptionId);
            return;
        }

        Subscription subscription = existing.get();
        String status = sub.path("status").asText(statusFromEvent(event));
        subscription.setStatus(status);

        long periodEnd = sub.path("current_end").asLong(0);
        if (periodEnd > 0) {
            subscription.setCurrentPeriodEnd(Instant.ofEpochSecond(periodEnd));
        }
        subscriptionRepository.save(subscription);

        userRepository.findById(subscription.getUserId()).ifPresent(user -> {
            User.Plan plan = Subscription.grantsAccess(status) ? User.Plan.PRO : User.Plan.FREE;
            if (user.getPlan() != plan) {
                user.setPlan(plan);
                userRepository.save(user);
                log.info("User {} moved to {} on event {}", user.getId(), plan, event);
            }
        });
    }

    /** Fallback when the payload omits status, which some event types do. */
    private String statusFromEvent(String event) {
        return switch (event) {
            case "subscription.activated", "subscription.charged", "subscription.authenticated" -> "active";
            case "subscription.halted" -> "halted";
            case "subscription.cancelled" -> "cancelled";
            case "subscription.completed" -> "completed";
            default -> "created";
        };
    }
}
