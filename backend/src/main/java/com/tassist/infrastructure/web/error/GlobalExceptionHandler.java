package com.tassist.infrastructure.web.error;

import com.tassist.domain.error.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.UUID;

/** Maps domain errors to §17.4 envelopes with the §17 status/code table. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationError.class)
    public ResponseEntity<ApiError> validation(ValidationError e) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", e.getMessage(), e.details());
    }

    @ExceptionHandler(EmailTaken.class)
    public ResponseEntity<ApiError> emailTaken(EmailTaken e) {
        return build(HttpStatus.CONFLICT, "EMAIL_TAKEN", e.getMessage(), null);
    }

    @ExceptionHandler(InvalidCredentials.class)
    public ResponseEntity<ApiError> invalidCreds(InvalidCredentials e) {
        return build(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e.getMessage(), null);
    }

    @ExceptionHandler(Unauthenticated.class)
    public ResponseEntity<ApiError> unauth(Unauthenticated e) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", e.getMessage(), null);
    }

    @ExceptionHandler(Forbidden.class)
    public ResponseEntity<ApiError> forbidden(Forbidden e) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage(), null);
    }

    @ExceptionHandler(NotFoundError.class)
    public ResponseEntity<ApiError> notFound(NotFoundError e) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), null);
    }

    @ExceptionHandler(ConflictError.class)
    public ResponseEntity<ApiError> conflict(ConflictError e) {
        return build(HttpStatus.CONFLICT, "CONFLICT", e.getMessage(), null);
    }

    @ExceptionHandler(UploadExceptions.UnsupportedMediaType.class)
    public ResponseEntity<ApiError> unsupportedMedia(UploadExceptions.UnsupportedMediaType e) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE", e.getMessage(), null);
    }

    @ExceptionHandler(UploadExceptions.PayloadTooLarge.class)
    public ResponseEntity<ApiError> payloadTooLarge(UploadExceptions.PayloadTooLarge e) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", e.getMessage(), null);
    }

    @ExceptionHandler(QuotaError.QuotaExceeded.class)
    public ResponseEntity<ApiError> quotaExceeded(QuotaError.QuotaExceeded e) {
        return build(HttpStatus.TOO_MANY_REQUESTS, "QUOTA_EXCEEDED", e.getMessage(), null);
    }

    @ExceptionHandler(QuotaError.RateLimited.class)
    public ResponseEntity<ApiError> rateLimited(QuotaError.RateLimited e) {
        // e.getMessage() carries the retry-after seconds (set by the rate-limit filter).
        String retryAfter = e.getMessage() == null ? "1" : e.getMessage();
        Map<String, String> details = Map.of("retryAfterSeconds", retryAfter);
        String correlationId = UUID.randomUUID().toString();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", retryAfter)
            .body(ApiError.of("RATE_LIMITED",
                "Too many requests. Try again in " + retryAfter + " seconds.", details, correlationId));
    }

    @ExceptionHandler(com.tassist.infrastructure.security.OAuthException.class)
    public ResponseEntity<ApiError> oauth(com.tassist.infrastructure.security.OAuthException e) {
        return build(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Google sign-in failed.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> fallback(Exception e) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", "Something went wrong.", null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String msg, Map<String, String> details) {
        String correlationId = UUID.randomUUID().toString();
        return ResponseEntity.status(status).body(ApiError.of(code, msg, details, correlationId));
    }
}
