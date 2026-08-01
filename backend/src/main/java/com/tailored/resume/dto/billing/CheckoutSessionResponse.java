package com.tailored.resume.dto.billing;

/**
 * Everything the browser needs to open Razorpay Checkout. The key id is public by design;
 * the key secret never leaves the server.
 *
 * @param amountPaise display amount only — Razorpay charges what the plan says
 */
public record CheckoutSessionResponse(
        String subscriptionId,
        String keyId,
        int amountPaise,
        String currency,
        String prefillEmail,
        String prefillName
) {}
