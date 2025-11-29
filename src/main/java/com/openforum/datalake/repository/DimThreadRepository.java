package com.openforum.datalake.repository;

import com.openforum.datalake.domain.DimThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DimThreadRepository extends JpaRepository<DimThread, UUID> {

    @Query("SELECT t FROM DimThread t WHERE t.tenantId = :tenantId AND t.status = 'OPEN' AND t.lastActivityAt < :cutoff")
    List<DimThread> findStaleThreads(@Param("tenantId") String tenantId, @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT t FROM DimThread t WHERE t.tenantId = :tenantId AND t.status = 'OPEN' AND t.replyCount = 0 AND t.createdAt < :cutoff")
    List<DimThread> findUnansweredThreads(@Param("tenantId") String tenantId, @Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT COUNT(t) FROM DimThread t WHERE t.tenantId = :tenantId AND t.isAnswered = true")
    long countAnsweredThreads(@Param("tenantId") String tenantId);

    @Query("SELECT COUNT(t) FROM DimThread t WHERE t.tenantId = :tenantId")
    long countTotalThreads(@Param("tenantId") String tenantId);
}
