package com.openforum.datalake.job;

import com.openforum.datalake.domain.DimMemberHealth;
import com.openforum.datalake.domain.EngagementLevel;
import com.openforum.datalake.domain.ChurnRisk;
import com.openforum.datalake.repository.DimMemberHealthRepository;
import com.openforum.datalake.repository.FactActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
public class MemberHealthCalculationJob {

    private static final Logger log = LoggerFactory.getLogger(MemberHealthCalculationJob.class);

    private final FactActivityRepository factActivityRepository;
    private final DimMemberHealthRepository dimMemberHealthRepository;

    public MemberHealthCalculationJob(FactActivityRepository factActivityRepository,
            DimMemberHealthRepository dimMemberHealthRepository) {
        this.factActivityRepository = factActivityRepository;
        this.dimMemberHealthRepository = dimMemberHealthRepository;
    }

    @Scheduled(cron = "0 0 1 * * ?") // Run daily at 1 AM
    public void calculateMemberHealth() {
        log.info("Starting Member Health Calculation Job");
        Instant startDate = Instant.now().minus(30, ChronoUnit.DAYS);

        List<Object[]> stats = factActivityRepository.findUserActivityStats(startDate);

        for (Object[] row : stats) {
            UUID userId = (UUID) row[0];
            // The original code had String tenantId = (String) row[1]; and Long
            // activityCount = (Long) row[2];
            // The provided change implies tenantId is no longer in row[1] and activityCount
            // is now row[1].
            // Also, tenantId is hardcoded to "default".
            Long activityCount = (Long) row[1]; // Assuming tenantId is no longer at row[1] and activityCount is now at
                                                // row[1]

            int score = calculateScore(activityCount);
            EngagementLevel engagementLevel = determineEngagementLevel(score);
            ChurnRisk churnRisk = determineChurnRisk(score);

            DimMemberHealth health = new DimMemberHealth();
            health.setUserId(userId);
            health.setTenantId("default"); // TODO: Handle multi-tenancy in job
            health.setHealthScore(score);
            health.setEngagementLevel(engagementLevel);
            health.setChurnRisk(churnRisk);
            health.setCalculatedAt(Instant.now());

            dimMemberHealthRepository.save(health);
        }
        log.info("Completed Member Health Calculation Job. Processed {} users.", stats.size());
    }

    private int calculateScore(Long activityCount) {
        // Simple logic: 1 activity = 1 point, max 100
        return (int) Math.min(activityCount, 100);
    }

    private EngagementLevel determineEngagementLevel(int score) {
        if (score >= 80)
            return EngagementLevel.CHAMPION;
        if (score >= 20)
            return EngagementLevel.CONTRIBUTOR;
        return EngagementLevel.LURKER;
    }

    private ChurnRisk determineChurnRisk(int score) {
        if (score < 10)
            return ChurnRisk.HIGH;
        if (score < 50)
            return ChurnRisk.MEDIUM;
        return ChurnRisk.LOW;
    }
}
