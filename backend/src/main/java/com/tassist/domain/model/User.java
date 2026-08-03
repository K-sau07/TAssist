package com.tassist.domain.model;

import com.tassist.domain.vo.AuthProvider;
import com.tassist.domain.vo.UserId;
import java.time.Instant;
import java.util.Optional;

/**
 * A person with an account (spec §8).
 *
 * <p>Invariants (enforced here):
 * <ul>
 *   <li>Exactly one credential is present, matching {@code authProvider}:
 *       {@code PASSWORD} ⇒ {@code passwordHash} present &amp; {@code googleSubject} empty;
 *       {@code GOOGLE} ⇒ {@code googleSubject} present &amp; {@code passwordHash} empty.</li>
 *   <li>Email is stored lowercased and non-blank (global uniqueness is enforced at the DB layer).</li>
 * </ul>
 */
public record User(
        UserId id,
        String email,
        String displayName,
        Optional<String> passwordHash,
        AuthProvider authProvider,
        Optional<String> googleSubject,
        Instant createdAt,
        Instant updatedAt
) {
    public User {
        if (id == null) throw new IllegalArgumentException("User.id must not be null");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("User.email must not be blank");
        if (!email.equals(email.toLowerCase())) throw new IllegalArgumentException("User.email must be lowercased");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("User.displayName must not be blank");
        if (authProvider == null) throw new IllegalArgumentException("User.authProvider must not be null");
        passwordHash = passwordHash == null ? Optional.empty() : passwordHash;
        googleSubject = googleSubject == null ? Optional.empty() : googleSubject;
        if (createdAt == null) throw new IllegalArgumentException("User.createdAt must not be null");
        if (updatedAt == null) throw new IllegalArgumentException("User.updatedAt must not be null");

        switch (authProvider) {
            case PASSWORD -> {
                if (passwordHash.isEmpty())
                    throw new IllegalArgumentException("PASSWORD user must have a passwordHash");
                if (googleSubject.isPresent())
                    throw new IllegalArgumentException("PASSWORD user must not have a googleSubject");
            }
            case GOOGLE -> {
                if (googleSubject.isEmpty())
                    throw new IllegalArgumentException("GOOGLE user must have a googleSubject");
                if (passwordHash.isPresent())
                    throw new IllegalArgumentException("GOOGLE user must not have a passwordHash");
            }
        }
    }
}
