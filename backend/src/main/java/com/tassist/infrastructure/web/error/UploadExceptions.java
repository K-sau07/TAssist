package com.tassist.infrastructure.web.error;

/** Web-layer upload failures mapped to HTTP 415 / 413 (§11.1 step 1). Not domain errors. */
public final class UploadExceptions {
    private UploadExceptions() {}

    /** 415 UNSUPPORTED_MEDIA_TYPE — content-type not one of the 7 supported. */
    public static final class UnsupportedMediaType extends RuntimeException {
        public UnsupportedMediaType(String message) { super(message); }
    }

    /** 413 PAYLOAD_TOO_LARGE — file exceeds the 25 MB limit. */
    public static final class PayloadTooLarge extends RuntimeException {
        public PayloadTooLarge(String message) { super(message); }
    }
}
