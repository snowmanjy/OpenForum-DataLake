package com.openforum.datalake.repository;

import com.openforum.datalake.domain.DimMemberHealth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DimMemberHealthRepository extends JpaRepository<DimMemberHealth, UUID> {

    List<DimMemberHealth> findByTenantIdAndEngagementLevel(String tenantId, String engagementLevel);

    List<DimMemberHealth> findByTenantIdAndChurnRisk(String tenantId, String churnRisk);
}
