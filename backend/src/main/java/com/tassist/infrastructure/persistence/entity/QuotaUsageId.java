package com.tassist.infrastructure.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class QuotaUsageId implements Serializable {
    private UUID userId;
    private LocalDate period;
    public QuotaUsageId() {}
    public QuotaUsageId(UUID userId, LocalDate period) { this.userId = userId; this.period = period; }
    public UUID getUserId() { return userId; } public void setUserId(UUID v) { this.userId = v; }
    public LocalDate getPeriod() { return period; } public void setPeriod(LocalDate v) { this.period = v; }
    @Override public boolean equals(Object o) {
        if (this == o) return true; if (!(o instanceof QuotaUsageId t)) return false;
        return Objects.equals(userId, t.userId) && Objects.equals(period, t.period);
    }
    @Override public int hashCode() { return Objects.hash(userId, period); }
}
