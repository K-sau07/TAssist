package com.tassist.domain.error;

/** Requested resource does not exist (or is not visible to the actor). HTTP 404 NOT_FOUND. */
public final class NotFoundError extends RuntimeException implements TassistError {
    public NotFoundError(String message) { super(message); }
    @Override public String message() { return getMessage(); }
}
