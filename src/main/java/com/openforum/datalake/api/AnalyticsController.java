package com.openforum.datalake.api;

import com.openforum.datalake.domain.DimMemberHealth;
import com.openforum.datalake.domain.DimThread;
import com.openforum.datalake.dto.DailyActiveUsersPoint;
import com.openforum.datalake.dto.DeflectionSavingsMetric;
import com.openforum.datalake.dto.ResponsivenessMetric;
import com.openforum.datalake.repository.DimMemberHealthRepository;
import com.openforum.datalake.repository.DimThreadRepository;
import com.openforum.datalake.repository.FactActivityRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analytics/v1")
public class AnalyticsController {

    private final FactActivityRepository factActivityRepository;
    private final DimThreadRepository dimThreadRepository;
    private final DimMemberHealthRepository dimMemberHealthRepository;

    public AnalyticsController(FactActivityRepository factActivityRepository,
            DimThreadRepository dimThreadRepository,
            DimMemberHealthRepository dimMemberHealthRepository) {
        this.factActivityRepository = factActivityRepository;
        this.dimThreadRepository = dimThreadRepository;
        this.dimMemberHealthRepository = dimMemberHealthRepository;
    }

    private String getTenantId(Jwt jwt) {
        return (String) jwt.getClaim("tenant_id");
    }

    // 4.1 Community Health

    @GetMapping("/activity")
    public ResponseEntity<List<DailyActiveUsersPoint>> getActivity(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "dau") String metric,
            @RequestParam(defaultValue = "day") String interval) {
        String tenantId = getTenantId(jwt);
        // Only DAU supported for now
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        return ResponseEntity.ok(factActivityRepository.countDailyActiveUsers(tenantId, startDate));
    }

    @GetMapping("/retention")
    public ResponseEntity<Map<String, Object>> getRetention(
            @AuthenticationPrincipal Jwt jwt) {
        // Placeholder for complex cohort analysis
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Retention analysis not yet implemented");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/responsiveness")
    public ResponseEntity<ResponsivenessMetric> getResponsiveness(
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = getTenantId(jwt);
        long totalThreads = dimThreadRepository.countTotalThreads(tenantId);
        long answeredThreads = dimThreadRepository.countAnsweredThreads(tenantId);
        double answerRate = totalThreads > 0 ? (double) answeredThreads / totalThreads : 0.0;

        // Median time calculation would require a more complex DB query or fetching all
        // data
        int medianResponseTimeMinutes = 0; // Placeholder

        return ResponseEntity
                .ok(new ResponsivenessMetric(answerRate, medianResponseTimeMinutes));
    }

    // 4.2 ROI & Value

    @GetMapping("/deflection-savings")
    public ResponseEntity<DeflectionSavingsMetric> getDeflectionSavings(
            @AuthenticationPrincipal Jwt jwt) {
        String tenantId = getTenantId(jwt);
        // Logic: Solved Threads * CostPerTicket (Assuming $50 per ticket as default)
        long solvedThreads = dimThreadRepository.countAnsweredThreads(tenantId); // Approximation
        double costPerTicket = 50.0;
        double savings = solvedThreads * costPerTicket;

        return ResponseEntity.ok(new DeflectionSavingsMetric(solvedThreads, savings));
    }

    @GetMapping("/champions")
    public ResponseEntity<List<DimMemberHealth>> getChampions(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "80") Integer minScore) {
        String tenantId = getTenantId(jwt);
        return ResponseEntity.ok(dimMemberHealthRepository.findByTenantIdAndEngagementLevel(tenantId, "CHAMPION"));
    }

    @GetMapping("/churn-risk")
    public ResponseEntity<List<DimMemberHealth>> getChurnRisk(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "high") String threshold) {
        String tenantId = getTenantId(jwt);
        return ResponseEntity
                .ok(dimMemberHealthRepository.findByTenantIdAndChurnRisk(tenantId, threshold.toUpperCase()));
    }

    // 4.3 Operational Queues

    @GetMapping("/stale-threads")
    public ResponseEntity<List<DimThread>> getStaleThreads(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "7") Integer days) {
        String tenantId = getTenantId(jwt);
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return ResponseEntity.ok(dimThreadRepository.findStaleThreads(tenantId, cutoff));
    }

    @GetMapping("/unanswered-threads")
    public ResponseEntity<List<DimThread>> getUnansweredThreads(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "24") Integer hours) {
        String tenantId = getTenantId(jwt);
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        return ResponseEntity.ok(dimThreadRepository.findUnansweredThreads(tenantId, cutoff));
    }
}
