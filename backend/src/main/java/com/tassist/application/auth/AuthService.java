package com.tassist.application.auth;

import com.tassist.domain.error.EmailTaken;
import com.tassist.domain.error.InvalidCredentials;
import com.tassist.domain.error.NotFoundError;
import com.tassist.domain.error.ValidationError;
import com.tassist.domain.model.User;
import com.tassist.domain.port.in.AuthUseCase;
import com.tassist.domain.port.out.UserRepository;
import com.tassist.domain.vo.AuthProvider;
import com.tassist.domain.vo.UserId;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Email+password and Google auth use cases (§10). Application layer: domain ports only. */
@Service
public class AuthService implements AuthUseCase {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public User signupWithPassword(String email, String displayName, String rawPassword) {
        String normEmail = normalizeEmail(email);
        validateSignup(normEmail, displayName, rawPassword);
        if (users.existsByEmail(normEmail)) {
            throw new EmailTaken("email already registered");
        }
        Instant now = Instant.now();
        User user = new User(
            UserId.newId(),
            normEmail,
            displayName.trim(),
            Optional.of(encoder.encode(rawPassword)),
            AuthProvider.PASSWORD,
            Optional.empty(),
            now, now
        );
        return users.save(user);
    }

    @Override
    public User loginWithPassword(String email, String rawPassword) {
        String normEmail = normalizeEmail(email);
        User user = users.findByEmail(normEmail)
            .orElseThrow(() -> new InvalidCredentials("invalid email or password"));
        String hash = user.passwordHash()
            .orElseThrow(() -> new InvalidCredentials("invalid email or password"));
        if (rawPassword == null || !encoder.matches(rawPassword, hash)) {
            throw new InvalidCredentials("invalid email or password");
        }
        return user;
    }

    @Override
    @Transactional
    public User upsertGoogleUser(String googleSubject, String email, String displayName) {
        return users.findByGoogleSubject(googleSubject).orElseGet(() -> {
            Instant now = Instant.now();
            User user = new User(
                UserId.newId(),
                normalizeEmail(email),
                (displayName == null || displayName.isBlank()) ? "User" : displayName.trim(),
                Optional.empty(),
                AuthProvider.GOOGLE,
                Optional.of(googleSubject),
                now, now
            );
            return users.save(user);
        });
    }

    @Override
    public User getById(UserId id) {
        return users.findById(id)
            .orElseThrow(() -> new NotFoundError("user not found"));
    }

    private String normalizeEmail(String email) {
        if (email == null) throw new ValidationError("email is required", Map.of("email", "required"));
        return email.trim().toLowerCase();
    }

    private void validateSignup(String email, String displayName, String rawPassword) {
        Map<String, String> errors = new HashMap<>();
        if (email.isBlank() || !EMAIL.matcher(email).matches()) {
            errors.put("email", "must be a valid email address");
        }
        if (displayName == null || displayName.trim().isEmpty() || displayName.trim().length() > 80) {
            errors.put("displayName", "required, 1-80 characters");
        }
        if (rawPassword == null || rawPassword.length() < 10
                || !rawPassword.chars().anyMatch(Character::isLetter)
                || !rawPassword.chars().anyMatch(Character::isDigit)) {
            errors.put("password", "min 10 chars, must contain a letter and a digit");
        }
        if (!errors.isEmpty()) {
            throw new ValidationError("signup validation failed", errors);
        }
    }
}
