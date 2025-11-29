package com.openforum.datalake.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openforum.datalake.ingestor.EventEnvelope;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "fact_activity")
public class FactActivity {

    @EmbeddedId
    private FactActivityId id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "activity_type")
    private String activityType;

    @Column(name = "target_id")
    private UUID targetId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    public FactActivity() {
    }

    public FactActivity(FactActivityId id, UUID eventId, String tenantId, UUID userId, String activityType,
            UUID targetId, Map<String, Object> metadata) {
        this.id = id;
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.activityType = activityType;
        this.targetId = targetId;
        this.metadata = metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public FactActivityId getId() {
        return id;
    }

    public void setId(FactActivityId id) {
        this.id = id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public void setTargetId(UUID targetId) {
        this.targetId = targetId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public static class Builder {
        private UUID id;
        private UUID eventId;
        private String tenantId;
        private UUID userId;
        private String activityType;
        private UUID targetId;
        private Instant occurredAt;
        private Map<String, Object> metadata;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder eventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder activityType(String activityType) {
            this.activityType = activityType;
            return this;
        }

        public Builder targetId(UUID targetId) {
            this.targetId = targetId;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public FactActivity build() {
            return new FactActivity(new FactActivityId(id, occurredAt), eventId, tenantId, userId, activityType,
                    targetId, metadata);
        }
    }

    public static FactActivity from(EventEnvelope event, String activityType, UUID targetId) {
        FactActivity fact = new FactActivity();
        fact.setId(new FactActivityId(UUID.randomUUID(), event.occurredAt()));
        fact.setEventId(event.eventId());
        fact.setTenantId(event.tenantId());
        fact.setUserId(extractUserId(event));
        fact.setActivityType(activityType);
        fact.setTargetId(targetId);
        fact.setMetadata(convertJsonNodeToMap(event.payload()));
        return fact;
    }

    private static UUID extractUserId(EventEnvelope event) {
        if (event.payload().has("authorId")) {
            return UUID.fromString(event.payload().get("authorId").asText());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> convertJsonNodeToMap(JsonNode jsonNode) {
        return new ObjectMapper().convertValue(jsonNode, Map.class);
    }

    @Embeddable
    public static class FactActivityId implements Serializable {
        @Column(name = "id")
        private UUID id;

        @Column(name = "occurred_at")
        private Instant occurredAt;

        public FactActivityId() {
        }

        public FactActivityId(UUID id, Instant occurredAt) {
            this.id = id;
            this.occurredAt = occurredAt;
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public Instant getOccurredAt() {
            return occurredAt;
        }

        public void setOccurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            FactActivityId that = (FactActivityId) o;
            return Objects.equals(id, that.id) && Objects.equals(occurredAt, that.occurredAt);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, occurredAt);
        }
    }
}
