package com.tailored.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailored.resume.billing.RazorpayClient;
import com.tailored.resume.billing.RazorpaySignature;
import com.tailored.resume.config.AppProperties;
import com.tailored.resume.entity.Subscription;
import com.tailored.resume.entity.User;
import com.tailored.resume.exception.UnauthorizedException;
import com.tailored.resume.repository.SubscriptionRepository;
import com.tailored.resume.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BillingServiceTest {

    private static final String SECRET = "test-webhook-secret";
    private static final String SUB_ID = "sub_TEST123";

    private final UUID userId = UUID.randomUUID();
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
    private final RazorpayClient razorpayClient = mock(RazorpayClient.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private BillingService service() {
        AppProperties props = new AppProperties();
        props.getRazorpay().setWebhookSecret(SECRET);
        props.getRazorpay().setKeyId("rzp_test_key");
        return new BillingService(props, razorpayClient, userRepository, subscriptionRepository, mapper);
    }

    private User user(User.Plan plan) {
        return User.builder().id(userId).email("a@b.com").passwordHash("x").fullName("A B").plan(plan).build();
    }

    private String payload(String event, String status) {
        return """
                {"event":"%s","payload":{"subscription":{"entity":{"id":"%s","status":"%s","current_end":1790000000}}}}
                """.formatted(event, SUB_ID, status).strip();
    }

    private void givenKnownSubscription(User.Plan currentPlan) {
        when(subscriptionRepository.findByRazorpaySubscriptionId(SUB_ID))
                .thenReturn(Optional.of(Subscription.builder()
                        .id(UUID.randomUUID()).userId(userId)
                        .razorpaySubscriptionId(SUB_ID).status("created").build()));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(currentPlan)));
    }

    @Test
    void activationUpgradesTheUserToPro() {
        givenKnownSubscription(User.Plan.FREE);
        String body = payload("subscription.activated", "active");

        service().handleWebhook(body, RazorpaySignature.compute(body, SECRET));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPlan()).isEqualTo(User.Plan.PRO);
    }

    @Test
    void haltingDowngradesTheUserToFree() {
        givenKnownSubscription(User.Plan.PRO);
        String body = payload("subscription.halted", "halted");

        service().handleWebhook(body, RazorpaySignature.compute(body, SECRET));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPlan()).isEqualTo(User.Plan.FREE);
    }

    @Test
    void forgedSignatureIsRejectedAndChangesNothing() {
        String body = payload("subscription.activated", "active");

        assertThatThrownBy(() -> service().handleWebhook(body, "deadbeef"))
                .isInstanceOf(UnauthorizedException.class);

        verifyNoInteractions(userRepository, subscriptionRepository);
    }

    @Test
    void signatureForADifferentBodyIsRejected() {
        String signedBody = payload("subscription.activated", "active");
        String tamperedBody = signedBody.replace(SUB_ID, "sub_ATTACKER");

        assertThatThrownBy(() ->
                service().handleWebhook(tamperedBody, RazorpaySignature.compute(signedBody, SECRET)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void missingSignatureHeaderIsRejected() {
        String body = payload("subscription.activated", "active");
        assertThatThrownBy(() -> service().handleWebhook(body, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void eventForAnUnknownSubscriptionIsIgnored() {
        when(subscriptionRepository.findByRazorpaySubscriptionId(SUB_ID)).thenReturn(Optional.empty());
        String body = payload("subscription.activated", "active");

        service().handleWebhook(body, RazorpaySignature.compute(body, SECRET));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void alreadyProUserIsNotWrittenAgain() {
        givenKnownSubscription(User.Plan.PRO);
        String body = payload("subscription.charged", "active");

        service().handleWebhook(body, RazorpaySignature.compute(body, SECRET));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void checkoutCreatesASubscriptionRowAndReturnsPublicFields() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(User.Plan.FREE)));
        when(razorpayClient.createSubscription("a@b.com")).thenReturn(SUB_ID);
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        var res = service().startCheckout(userId);

        assertThat(res.subscriptionId()).isEqualTo(SUB_ID);
        assertThat(res.keyId()).isEqualTo("rzp_test_key");
        assertThat(res.amountPaise()).isEqualTo(49900);
        verify(subscriptionRepository).save(any(Subscription.class));
    }
}
