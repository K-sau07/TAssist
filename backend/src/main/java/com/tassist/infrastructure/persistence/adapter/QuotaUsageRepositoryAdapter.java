package com.tassist.infrastructure.persistence.adapter;

import com.tassist.domain.model.QuotaUsage;
import com.tassist.domain.port.out.QuotaUsageRepository;
import com.tassist.domain.vo.UserId;
import com.tassist.infrastructure.persistence.entity.QuotaUsageId;
import com.tassist.infrastructure.persistence.mapper.QuotaUsageMapper;
import com.tassist.infrastructure.persistence.repo.QuotaUsageJpaRepository;
import org.springframework.stereotype.Repository;
import java.time.YearMonth;
import java.util.Optional;

@Repository
public class QuotaUsageRepositoryAdapter implements QuotaUsageRepository {
    private final QuotaUsageJpaRepository jpa;
    public QuotaUsageRepositoryAdapter(QuotaUsageJpaRepository jpa) { this.jpa = jpa; }

    @Override public QuotaUsage save(QuotaUsage q) { return QuotaUsageMapper.toDomain(jpa.save(QuotaUsageMapper.toEntity(q))); }
    @Override public Optional<QuotaUsage> find(UserId userId, YearMonth period) {
        return jpa.findById(new QuotaUsageId(userId.value(), period.atDay(1))).map(QuotaUsageMapper::toDomain);
    }
}
