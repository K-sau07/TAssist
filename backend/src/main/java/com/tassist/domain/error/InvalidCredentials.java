package com.tassist.domain.error;

/** Wrong email/password at login. HTTP 401 INVALID_CREDENTIALS. */
public final class InvalidCredentials extends RuntimeException implements AuthError {
    public InvalidCredentials(String message) { super(message); }
    @Override public String message() { return getMessage(); }
}
