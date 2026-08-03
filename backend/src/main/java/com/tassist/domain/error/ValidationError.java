package com.tassist.domain.error;

import java.util.Map;

/** Input failed validation. HTTP 422 VALIDATION_ERROR, carries per-field details (spec §17.4). */
public final class ValidationError extends RuntimeException implements TassistError {
    private final transient Map<String, String> details;

    public ValidationError(String message, Map<String, String> details) {
        super(message);
        this.details = details == null ? Map.of() : Map.copyOf(details);
    }

    public ValidationError(String message) { this(message, Map.of()); }

    /** Immutable per-field detail map (field name -> human-readable reason). */
    public Map<String, String> details() { return details; }

    @Override public String message() { return getMessage(); }
}
