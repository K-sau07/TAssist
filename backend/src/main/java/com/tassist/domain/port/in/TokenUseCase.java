package com.tassist.domain.port.in;

import com.tassist.domain.vo.UserId;

/**
 * Inbound port: JWT mint/verify (spec §10). Implementation (Step 3) lives in application/auth
 * and delegates signing to an infrastructure detail; the port stays framework-free.
 */
public interface TokenUseCase {
    /** Mint a signed session token for a user. */
    String issueToken(UserId userId);
    /** Verify a token and return the subject user id. Throws Unauthenticated if invalid/expired. */
    UserId verifyToken(String token);
}
