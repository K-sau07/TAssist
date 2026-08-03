package com.tassist.domain.error;

/** No valid credentials / not logged in. HTTP 401 UNAUTHENTICATED. */
public final class Unauthenticated extends RuntimeException implements AuthError {
    public Unauthenticated(String message) { super(message); }
    @Override public String message() { return getMessage(); }
}
