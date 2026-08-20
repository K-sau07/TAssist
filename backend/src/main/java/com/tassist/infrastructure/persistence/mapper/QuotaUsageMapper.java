package com.tassist.infrastructure.persistence.mapper;

import com.tassist.domain.model.QuotaUsage;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.QuotaUsageEntity;
import java.time.LocalDate;
import java.time.YearMonth;

public final class QuotaUsageMapper {
    private QuotaUsageMapper() {}

    public static QuotaUsageEntity toEntity(QuotaUsage q) {
        QuotaUsageEntity e = new QuotaUsageEntity();
        e.setUserId(q.userId().value());
        e.setPeriod(q.period().atDay(1));
        e.setQuestionsAsked(q.questionsAsked());
        e.setFilesUploaded(q.filesUploaded());
        e.setBytesStored(q.bytesStored());
        e.setTokensConsumed(q.tokensConsumed());
        return e;
    }

    public static QuotaUsage toDomain(QuotaUsageEntity e) {
        return new QuotaUsage(
            UserId.of(e.getUserId()),
            YearMonth.from(e.getPeriod()),
            e.getQuestionsAsked(),
            e.getFilesUploaded(),
            e.getBytesStored(),
            e.getTokensConsumed()
        );
    }
}
