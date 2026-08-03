package com.tassist.domain.model;

import com.tassist.domain.vo.UserId;
import java.time.YearMonth;

/** Per-user, per-month usage counters (spec §8 / §16.2). */
public record QuotaUsage(
        UserId userId,
        YearMonth period,
        long questionsAsked,
        long filesUploaded,
        long bytesStored,
        long tokensConsumed
) {
    public QuotaUsage {
        if (userId == null) throw new IllegalArgumentException("QuotaUsage.userId must not be null");
        if (period == null) throw new IllegalArgumentException("QuotaUsage.period must not be null");
        if (questionsAsked < 0 || filesUploaded < 0 || bytesStored < 0 || tokensConsumed < 0)
            throw new IllegalArgumentException("QuotaUsage counters must not be negative");
    }
}
