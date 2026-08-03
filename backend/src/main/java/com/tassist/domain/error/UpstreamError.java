package com.tassist.domain.error;

/** Failures talking to external providers (spec §17.3). */
public sealed interface UpstreamError extends TassistError
        permits UpstreamError.LlmFailure, UpstreamError.EmbeddingFailure, UpstreamError.Timeout {

    final class LlmFailure extends RuntimeException implements UpstreamError {
        public LlmFailure(String message) { super(message); }
        public LlmFailure(String message, Throwable cause) { super(message, cause); }
        @Override public String message() { return getMessage(); }
    }

    final class EmbeddingFailure extends RuntimeException implements UpstreamError {
        public EmbeddingFailure(String message) { super(message); }
        public EmbeddingFailure(String message, Throwable cause) { super(message, cause); }
        @Override public String message() { return getMessage(); }
    }

    final class Timeout extends RuntimeException implements UpstreamError {
        public Timeout(String message) { super(message); }
        public Timeout(String message, Throwable cause) { super(message, cause); }
        @Override public String message() { return getMessage(); }
    }
}
