package com.example.ArtConnect.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ArtConnect.dto.UserDtos.AuthResponse;
import com.example.ArtConnect.dto.UserDtos.LoginRequest;
import com.example.ArtConnect.dto.UserDtos.RegisterRequest;
import com.example.ArtConnect.dto.UserDtos.UserProfile;
import com.example.ArtConnect.exception.ApiException;
import com.example.ArtConnect.model.AuthToken;
import com.example.ArtConnect.model.Role;
import com.example.ArtConnect.model.User;
import com.example.ArtConnect.repository.AuthTokenRepository;
import com.example.ArtConnect.repository.UserRepository;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final AuthTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration tokenValidity;

    public AuthService(UserRepository userRepository, AuthTokenRepository tokenRepository,
                       @Value("${app.auth.token-validity-days:7}") long tokenValidityDays) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.tokenValidity = Duration.ofDays(tokenValidityDays);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw ApiException.badRequest("Choose either an artist or a collector account");
        }
        String username = normalize(request.username());
        if (userRepository.existsByUsername(username)) {
            throw ApiException.conflict("That username is already taken");
        }
        String displayName = request.displayName() == null || request.displayName().isBlank()
                ? request.username().trim() : request.displayName().trim();
        User user = userRepository.save(new User(username, passwordEncoder.encode(request.password()),
                request.role(), displayName));
        return issueToken(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(normalize(request.username()))
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));
        return issueToken(user);
    }

    @Transactional
    public void logout(String rawToken) {
        tokenRepository.deleteById(hash(rawToken));
    }

    @Transactional(readOnly = true)
    public Optional<User> authenticate(String rawToken) {
        return tokenRepository.findActiveUser(hash(rawToken), Instant.now());
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        tokenRepository.deleteExpired(Instant.now());
    }

    private AuthResponse issueToken(User user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenRepository.save(new AuthToken(hash(rawToken), user, Instant.now().plus(tokenValidity)));
        return new AuthResponse(rawToken, UserProfile.from(user));
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
