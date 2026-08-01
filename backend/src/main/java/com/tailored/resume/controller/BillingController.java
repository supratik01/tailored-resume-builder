package com.tailored.resume.controller;

import com.tailored.resume.dto.billing.CheckoutSessionResponse;
import com.tailored.resume.dto.billing.UsageResponse;
import com.tailored.resume.security.CurrentUser;
import com.tailored.resume.service.BillingService;
import com.tailored.resume.service.QuotaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
@Tag(name = "Billing")
public class BillingController {

    private final QuotaService quotaService;
    private final BillingService billingService;

    @GetMapping("/usage")
    public ResponseEntity<UsageResponse> usage() {
        return ResponseEntity.ok(quotaService.usage(CurrentUser.id()));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutSessionResponse> checkout() {
        return ResponseEntity.ok(billingService.startCheckout(CurrentUser.id()));
    }

    /**
     * Razorpay posts here. Authentication is the HMAC signature over the raw body, so take the
     * body as a String — binding it to an object would re-serialize it and break verification.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        billingService.handleWebhook(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
