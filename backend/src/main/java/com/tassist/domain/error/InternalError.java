package com.tassist.domain.error;

/** Unexpected internal failure — fallback. HTTP 500 INTERNAL. */
public final class InternalError extends RuntimeException implements TassistError {
    public InternalError(String message) { super(message); }
    public InternalError(String message, Throwable cause) { super(message, cause); }
    @Override public String message() { return getMessage(); }
}
