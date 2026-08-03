package com.tassist.domain.error;

/** Rate-limit and quota failures (spec §17.3), both mapping to HTTP 429. */
public sealed interface QuotaError extends TassistError
        permits QuotaError.RateLimited, QuotaError.QuotaExceeded {

    /** Too many requests in a time window (Bucket4j). */
    final class RateLimited extends RuntimeException implements QuotaError {
        public RateLimited(String message) { super(message); }
        @Override public String message() { return getMessage(); }
    }

    /** Monthly quota exhausted (quota_usage). */
    final class QuotaExceeded extends RuntimeException implements QuotaError {
        public QuotaExceeded(String message) { super(message); }
        @Override public String message() { return getMessage(); }
    }
}
