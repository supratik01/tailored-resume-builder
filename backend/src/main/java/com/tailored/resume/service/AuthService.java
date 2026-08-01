package com.tailored.resume.service;

import com.tailored.resume.config.AppProperties;
import com.tailored.resume.dto.auth.*;
import com.tailored.resume.entity.RefreshToken;
import com.tailored.resume.entity.User;
import com.tailored.resume.exception.BadRequestException;
import com.tailored.resume.exception.ConflictException;
import com.tailored.resume.exception.UnauthorizedException;
import com.tailored.resume.repository.RefreshTokenRepository;
import com.tailored.resume.repository.UserRepository;
import com.tailored.resume.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RNG = new SecureRandom();

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .fullName(req.fullName().trim())
                .role(User.Role.USER)
                .build();
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest req) {
        String hash = hash(req.refreshToken());
        RefreshToken token = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
        token.setRevoked(true);
        refreshTokenRepository.save(token);
        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.issueAccessToken(user);
        String refreshTokenPlain = generateOpaqueToken();
        RefreshToken stored = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash(refreshTokenPlain))
                .expiresAt(Instant.now().plus(appProperties.getJwt().getRefreshTtlDays(), ChronoUnit.DAYS))
                .revoked(false)
                .build();
        refreshTokenRepository.save(stored);
        return new AuthResponse(
                accessToken,
                refreshTokenPlain,
                jwtService.accessTtlSeconds(),
                new AuthResponse.UserSummary(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name())
        );
    }

    private static String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        RNG.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new BadRequestException("SHA-256 unavailable");
        }
    }
}
