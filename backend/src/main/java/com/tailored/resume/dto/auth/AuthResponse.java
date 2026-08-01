package com.tailored.resume.dto.auth;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        UserSummary user
) {
    public record UserSummary(UUID id, String email, String fullName, String role) {}
}
