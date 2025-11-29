package com.openforum.datalake.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
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
    private LocalDateTime joinDate;

    @Column(name = "reputation")
    private Integer reputation;

    @Column(name = "is_bot")
    private Boolean isBot;

    public DimUser() {
    }

    public DimUser(UUID userId, String tenantId, LocalDateTime joinDate, Integer reputation, Boolean isBot) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.joinDate = joinDate;
        this.reputation = reputation;
        this.isBot = isBot;
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

    public LocalDateTime getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDateTime joinDate) {
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

    public static class Builder {
        private UUID userId;
        private String tenantId;
        private LocalDateTime joinDate;
        private Integer reputation;
        private Boolean isBot;

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder joinDate(LocalDateTime joinDate) {
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
