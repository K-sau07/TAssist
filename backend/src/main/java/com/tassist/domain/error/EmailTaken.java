package com.tassist.domain.error;

/** Signup with an email already in use. HTTP 409 EMAIL_TAKEN. */
public final class EmailTaken extends RuntimeException implements AuthError {
    public EmailTaken(String message) { super(message); }
    @Override public String message() { return getMessage(); }
}
