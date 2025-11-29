package com.openforum.datalake.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant; // Changed from LocalDateTime
import java.util.UUID;

@Entity
@Table(name = "dim_member_health")
public class DimMemberHealth {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "health_score")
    private Integer healthScore;

    @Column(name = "churn_risk")
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private ChurnRisk churnRisk;

    @Column(name = "engagement_level")
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private EngagementLevel engagementLevel;

    @Column(name = "calculated_at")
    private Instant calculatedAt; // Changed from LocalDateTime

    public DimMemberHealth() {
    }

    public DimMemberHealth(UUID userId, String tenantId, Integer healthScore, ChurnRisk churnRisk,
            EngagementLevel engagementLevel,
            Instant calculatedAt) { // Changed from LocalDateTime
        this.userId = userId;
        this.tenantId = tenantId;
        this.healthScore = healthScore;
        this.churnRisk = churnRisk;
        this.engagementLevel = engagementLevel;
        this.calculatedAt = calculatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Integer getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(Integer healthScore) {
        this.healthScore = healthScore;
    }

    public ChurnRisk getChurnRisk() {
        return churnRisk;
    }

    public void setChurnRisk(ChurnRisk churnRisk) {
        this.churnRisk = churnRisk;
    }

    public EngagementLevel getEngagementLevel() {
        return engagementLevel;
    }

    public void setEngagementLevel(EngagementLevel engagementLevel) {
        this.engagementLevel = engagementLevel;
    }

    public Instant getCalculatedAt() { // Changed from LocalDateTime
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) { // Changed from LocalDateTime
        this.calculatedAt = calculatedAt;
    }

    public static class Builder {
        private UUID userId;
        private String tenantId;
        private Integer healthScore;
        private ChurnRisk churnRisk;
        private EngagementLevel engagementLevel;
        private Instant calculatedAt; // Changed from LocalDateTime

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder healthScore(Integer healthScore) {
            this.healthScore = healthScore;
            return this;
        }

        public Builder churnRisk(ChurnRisk churnRisk) {
            this.churnRisk = churnRisk;
            return this;
        }

        public Builder engagementLevel(EngagementLevel engagementLevel) {
            this.engagementLevel = engagementLevel;
            return this;
        }

        public Builder calculatedAt(Instant calculatedAt) { // Changed from LocalDateTime
            this.calculatedAt = calculatedAt;
            return this;
        }

        public DimMemberHealth build() {
            return new DimMemberHealth(userId, tenantId, healthScore, churnRisk, engagementLevel, calculatedAt);
        }
    }
}
