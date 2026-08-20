package com.tassist.infrastructure.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** §17.4 client-facing error envelope. details only for VALIDATION_ERROR; correlationId always. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(Body error) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Body(String code, String message, Map<String, String> details, String correlationId) {}

    public static ApiError of(String code, String message, Map<String, String> details, String correlationId) {
        return new ApiError(new Body(code, message,
            (details == null || details.isEmpty()) ? null : details, correlationId));
    }
}
