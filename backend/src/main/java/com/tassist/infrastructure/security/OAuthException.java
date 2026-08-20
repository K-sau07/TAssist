package com.tassist.infrastructure.security;

/** OAuth flow failure (state mismatch, code exchange failure). Mapped to 401 by the web layer. */
public class OAuthException extends RuntimeException {
    public OAuthException(String message) { super(message); }
    public OAuthException(String message, Throwable cause) { super(message, cause); }
}
