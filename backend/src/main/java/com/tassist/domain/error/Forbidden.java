package com.tassist.domain.error;

/** Authenticated but not allowed to act on this resource. HTTP 403 FORBIDDEN. */
public final class Forbidden extends RuntimeException implements AuthError {
    public Forbidden(String message) { super(message); }
    @Override public String message() { return getMessage(); }
}
