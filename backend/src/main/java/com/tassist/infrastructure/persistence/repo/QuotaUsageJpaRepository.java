package com.tassist.infrastructure.persistence.repo;

import com.tassist.infrastructure.persistence.entity.QuotaUsageEntity;
import com.tassist.infrastructure.persistence.entity.QuotaUsageId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotaUsageJpaRepository extends JpaRepository<QuotaUsageEntity, QuotaUsageId> {
}
