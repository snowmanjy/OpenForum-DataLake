package com.openforum.datalake.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
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
    private String churnRisk;

    @Column(name = "engagement_level")
    private String engagementLevel;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    public DimMemberHealth() {
    }

    public DimMemberHealth(UUID userId, String tenantId, Integer healthScore, String churnRisk, String engagementLevel,
            LocalDateTime calculatedAt) {
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

    public String getChurnRisk() {
        return churnRisk;
    }

    public void setChurnRisk(String churnRisk) {
        this.churnRisk = churnRisk;
    }

    public String getEngagementLevel() {
        return engagementLevel;
    }

    public void setEngagementLevel(String engagementLevel) {
        this.engagementLevel = engagementLevel;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public static class Builder {
        private UUID userId;
        private String tenantId;
        private Integer healthScore;
        private String churnRisk;
        private String engagementLevel;
        private LocalDateTime calculatedAt;

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

        public Builder churnRisk(String churnRisk) {
            this.churnRisk = churnRisk;
            return this;
        }

        public Builder engagementLevel(String engagementLevel) {
            this.engagementLevel = engagementLevel;
            return this;
        }

        public Builder calculatedAt(LocalDateTime calculatedAt) {
            this.calculatedAt = calculatedAt;
            return this;
        }

        public DimMemberHealth build() {
            return new DimMemberHealth(userId, tenantId, healthScore, churnRisk, engagementLevel, calculatedAt);
        }
    }
}
