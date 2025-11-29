package com.openforum.datalake.repository;

import com.openforum.datalake.domain.FactActivity;
import com.openforum.datalake.domain.FactActivity.FactActivityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface FactActivityRepository extends JpaRepository<FactActivity, FactActivityId> {
    boolean existsByEventId(UUID eventId);

    @Query("SELECT CAST(f.id.occurredAt AS date) as date, COUNT(DISTINCT f.userId) as count " +
            "FROM FactActivity f " +
            "WHERE f.tenantId = :tenantId AND f.id.occurredAt >= :startDate " +
            "GROUP BY CAST(f.id.occurredAt AS date) " +
            "ORDER BY date")
    List<Object[]> countDailyActiveUsers(@Param("tenantId") String tenantId,
            @Param("startDate") LocalDateTime startDate);

    @Query("SELECT f.userId, f.tenantId, COUNT(f) FROM FactActivity f WHERE f.id.occurredAt >= :startDate GROUP BY f.userId, f.tenantId")
    List<Object[]> findUserActivityStats(@Param("startDate") LocalDateTime startDate);
}
