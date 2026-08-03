package com.tassist.domain.error;

/** Request conflicts with current state (e.g. duplicate, illegal state transition). HTTP 409 CONFLICT. */
public final class ConflictError extends RuntimeException implements TassistError {
    public ConflictError(String message) { super(message); }
    @Override public String message() { return getMessage(); }
}
