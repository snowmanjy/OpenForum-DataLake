package com.openforum.datalake.repository;

import com.openforum.datalake.domain.FactActivity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.openforum.datalake.dto.DailyActiveUsersPoint;

@Repository
public interface FactActivityRepository extends JpaRepository<FactActivity, FactActivity.FactActivityId> {

        boolean existsByEventId(UUID eventId);

        @Query("SELECT new com.openforum.datalake.dto.DailyActiveUsersPoint(CAST(fa.id.occurredAt AS LocalDate), COUNT(DISTINCT fa.userId)) "
                        +
                        "FROM FactActivity fa " +
                        "WHERE fa.tenantId = :tenantId " +
                        "AND fa.id.occurredAt >= :startDate " +
                        "GROUP BY CAST(fa.id.occurredAt AS LocalDate) " +
                        "ORDER BY CAST(fa.id.occurredAt AS LocalDate)")
        List<DailyActiveUsersPoint> countDailyActiveUsers(@Param("tenantId") String tenantId,
                        @Param("startDate") Instant startDate);

        @Query("SELECT fa.userId, COUNT(fa) FROM FactActivity fa WHERE fa.id.occurredAt >= :startDate GROUP BY fa.userId")
        List<Object[]> findUserActivityStats(@Param("startDate") Instant startDate);
}
