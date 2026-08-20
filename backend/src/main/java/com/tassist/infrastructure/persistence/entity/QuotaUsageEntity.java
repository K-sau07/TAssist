package com.tassist.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "quota_usage")
@IdClass(QuotaUsageId.class)
public class QuotaUsageEntity {
    @Id @Column(name = "user_id", nullable = false) private UUID userId;
    @Id @Column(name = "period", nullable = false) private LocalDate period;
    @Column(name = "questions_asked", nullable = false) private long questionsAsked;
    @Column(name = "files_uploaded", nullable = false) private long filesUploaded;
    @Column(name = "bytes_stored", nullable = false) private long bytesStored;
    @Column(name = "tokens_consumed", nullable = false) private long tokensConsumed;

    public QuotaUsageEntity() {}
    public UUID getUserId() { return userId; } public void setUserId(UUID v) { this.userId = v; }
    public LocalDate getPeriod() { return period; } public void setPeriod(LocalDate v) { this.period = v; }
    public long getQuestionsAsked() { return questionsAsked; } public void setQuestionsAsked(long v) { this.questionsAsked = v; }
    public long getFilesUploaded() { return filesUploaded; } public void setFilesUploaded(long v) { this.filesUploaded = v; }
    public long getBytesStored() { return bytesStored; } public void setBytesStored(long v) { this.bytesStored = v; }
    public long getTokensConsumed() { return tokensConsumed; } public void setTokensConsumed(long v) { this.tokensConsumed = v; }
}
