package com.tailored.resume.billing;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Razorpay signs webhook bodies with HMAC-SHA256 over the exact raw payload, hex encoded,
 * in the X-Razorpay-Signature header.
 */
public final class RazorpaySignature {

    private RazorpaySignature() {}

    public static String compute(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute webhook signature", e);
        }
    }

    /** Constant-time comparison — a timing-sensitive equals here would leak the secret. */
    public static boolean isValid(String payload, String secret, String providedSignature) {
        if (payload == null || secret == null || secret.isBlank() || providedSignature == null) {
            return false;
        }
        byte[] expected = compute(payload, secret).getBytes(StandardCharsets.UTF_8);
        byte[] provided = providedSignature.trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }
}
