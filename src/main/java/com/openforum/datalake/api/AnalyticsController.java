package com.openforum.datalake.api;

import com.openforum.datalake.domain.DimMemberHealth;
import com.openforum.datalake.domain.DimThread;
import com.openforum.datalake.repository.DimMemberHealthRepository;
import com.openforum.datalake.repository.DimThreadRepository;
import com.openforum.datalake.repository.FactActivityRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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

    // 4.1 Community Health

    @GetMapping("/activity")
    public ResponseEntity<List<Object[]>> getActivity(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "dau") String metric,
            @RequestParam(defaultValue = "day") String interval) {
        // Only DAU supported for now
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        return ResponseEntity.ok(factActivityRepository.countDailyActiveUsers(tenantId, startDate));
    }

    @GetMapping("/retention")
    public ResponseEntity<Map<String, Object>> getRetention(@RequestParam String tenantId) {
        // Placeholder for complex cohort analysis
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Retention analysis not yet implemented");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/responsiveness")
    public ResponseEntity<Map<String, Object>> getResponsiveness(@RequestParam String tenantId) {
        long totalThreads = dimThreadRepository.countTotalThreads(tenantId);
        long answeredThreads = dimThreadRepository.countAnsweredThreads(tenantId);
        double answerRate = totalThreads > 0 ? (double) answeredThreads / totalThreads : 0.0;

        Map<String, Object> response = new HashMap<>();
        response.put("answerRate", answerRate);
        // Median time calculation would require a more complex DB query or fetching all
        // data
        response.put("medianResponseTimeMinutes", 0); // Placeholder
        return ResponseEntity.ok(response);
    }

    // 4.2 ROI & Value

    @GetMapping("/deflection-savings")
    public ResponseEntity<Map<String, Object>> getDeflectionSavings(@RequestParam String tenantId) {
        // Logic: Solved Threads * CostPerTicket (Assuming $50 per ticket as default)
        long solvedThreads = dimThreadRepository.countAnsweredThreads(tenantId); // Approximation
        double costPerTicket = 50.0;
        double savings = solvedThreads * costPerTicket;

        Map<String, Object> response = new HashMap<>();
        response.put("deflectionCount", solvedThreads);
        response.put("estimatedSavings", savings);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/champions")
    public ResponseEntity<List<DimMemberHealth>> getChampions(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "80") Integer minScore) {
        return ResponseEntity.ok(dimMemberHealthRepository.findByTenantIdAndEngagementLevel(tenantId, "CHAMPION"));
    }

    @GetMapping("/churn-risk")
    public ResponseEntity<List<DimMemberHealth>> getChurnRisk(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "high") String threshold) {
        return ResponseEntity
                .ok(dimMemberHealthRepository.findByTenantIdAndChurnRisk(tenantId, threshold.toUpperCase()));
    }

    // 4.3 Operational Queues

    @GetMapping("/stale-threads")
    public ResponseEntity<List<DimThread>> getStaleThreads(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "7") Integer days) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return ResponseEntity.ok(dimThreadRepository.findStaleThreads(tenantId, cutoff));
    }

    @GetMapping("/unanswered-threads")
    public ResponseEntity<List<DimThread>> getUnansweredThreads(
            @RequestParam String tenantId,
            @RequestParam(defaultValue = "24") Integer hours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        return ResponseEntity.ok(dimThreadRepository.findUnansweredThreads(tenantId, cutoff));
    }
}
