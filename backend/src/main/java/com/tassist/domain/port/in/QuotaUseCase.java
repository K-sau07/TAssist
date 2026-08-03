package com.tassist.domain.port.in;

import com.tassist.domain.model.QuotaUsage;
import com.tassist.domain.vo.UserId;

/**
 * Inbound port: monthly quota tracking + enforcement (spec 12.8, 16.2). Built in Step 13.
 * Enforcement methods throw QuotaError.QuotaExceeded when a limit would be crossed.
 */
public interface QuotaUseCase {

    QuotaUsage getCurrent(UserId actingUser);

    void checkQuestionAllowed(UserId actingUser);

    void checkUploadAllowed(UserId actingUser, long sizeBytes);

    void recordQuestion(UserId actingUser, long tokensConsumed);

    void recordUpload(UserId actingUser, long sizeBytes);
}
