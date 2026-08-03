package com.tassist.domain.error;

/**
 * Root of the sealed domain error hierarchy (spec §17.3).
 *
 * <p>Domain and application code throw these; the web layer's {@code @ControllerAdvice}
 * maps each concrete type to an HTTP status + stable code string. Being sealed, the
 * mapping is exhaustive and the compiler enforces that every error kind is handled.
 *
 * <p>This is a {@link RuntimeException} so services need not declare it, but it carries
 * no framework dependencies.
 */
public sealed interface TassistError
        permits AuthError, ValidationError, NotFoundError, ConflictError,
                QuotaError, UpstreamError, InternalError {
    String message();
}
