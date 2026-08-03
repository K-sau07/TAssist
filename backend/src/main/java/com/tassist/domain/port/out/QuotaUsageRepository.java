package com.tassist.domain.port.out;

import com.tassist.domain.model.QuotaUsage;
import com.tassist.domain.vo.UserId;
import java.time.YearMonth;
import java.util.Optional;

/** Persistence port for {@link QuotaUsage} (spec §7, §16.2). */
public interface QuotaUsageRepository {
    QuotaUsage save(QuotaUsage usage);
    Optional<QuotaUsage> find(UserId userId, YearMonth period);
}
