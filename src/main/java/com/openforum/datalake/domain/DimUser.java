package com.openforum.datalake.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dim_users")
public class DimUser {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "join_date")
    private Instant joinDate;

    private Integer reputation;

    @Column(name = "is_bot")
    private Boolean isBot = false;

    public DimUser() {
    }

    public DimUser(UUID userId, String tenantId, Instant joinDate, Integer reputation, Boolean isBot) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.joinDate = joinDate;
        this.reputation = reputation;
        this.isBot = isBot;
    }

    // Getters and Setters

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

    public Instant getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(Instant joinDate) {
        this.joinDate = joinDate;
    }

    public Integer getReputation() {
        return reputation;
    }

    public void setReputation(Integer reputation) {
        this.reputation = reputation;
    }

    public Boolean getIsBot() {
        return isBot;
    }

    public void setIsBot(Boolean isBot) {
        this.isBot = isBot;
    }

    // Builder Pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID userId;
        private String tenantId;
        private Instant joinDate;
        private Integer reputation;
        private Boolean isBot = false;

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder joinDate(Instant joinDate) {
            this.joinDate = joinDate;
            return this;
        }

        public Builder reputation(Integer reputation) {
            this.reputation = reputation;
            return this;
        }

        public Builder isBot(Boolean isBot) {
            this.isBot = isBot;
            return this;
        }

        public DimUser build() {
            return new DimUser(userId, tenantId, joinDate, reputation, isBot);
        }
    }
}
