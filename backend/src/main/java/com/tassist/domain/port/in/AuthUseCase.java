package com.tassist.domain.port.in;

import com.tassist.domain.model.User;
import com.tassist.domain.vo.UserId;

/**
 * Inbound port: authentication use cases (spec §12.1). Implemented by the application
 * auth service in Step 3. Signup/login return the authenticated {@link User}; token
 * minting is a separate concern ({@link TokenUseCase}).
 */
public interface AuthUseCase {

    /** Register a new email+password user. Throws EmailTaken / ValidationError. */
    User signupWithPassword(String email, String displayName, String rawPassword);

    /** Verify email+password. Throws InvalidCredentials on mismatch. */
    User loginWithPassword(String email, String rawPassword);

    /** Find-or-create a Google user by subject claim (used by the OAuth callback). */
    User upsertGoogleUser(String googleSubject, String email, String displayName);

    /** Load the current user by id (for GET /api/me). Throws NotFoundError. */
    User getById(UserId id);
}
