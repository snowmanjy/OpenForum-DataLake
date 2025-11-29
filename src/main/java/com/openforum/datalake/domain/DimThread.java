package com.openforum.datalake.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.openforum.datalake.ingestor.EventEnvelope;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dim_threads")
public class DimThread {

    @Id
    @Column(name = "thread_id")
    private UUID threadId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(name = "author_id")
    private UUID authorId;

    private String title;

    private String status; // OPEN, CLOSED, SOLVED

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> tags;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "response_time_minutes")
    private Integer responseTimeMinutes;

    @Column(name = "is_answered")
    private Boolean isAnswered = false;

    @Column(name = "reply_count")
    private Integer replyCount = 0;

    public DimThread() {
    }

    public DimThread(UUID threadId, String tenantId, UUID categoryId, UUID authorId, String title, String status,
            List<String> tags, Instant createdAt, Instant lastActivityAt, Integer responseTimeMinutes,
            Boolean isAnswered, Integer replyCount) {
        this.threadId = threadId;
        this.tenantId = tenantId;
        this.categoryId = categoryId;
        this.authorId = authorId;
        this.title = title;
        this.status = status;
        this.tags = tags;
        this.createdAt = createdAt;
        this.lastActivityAt = lastActivityAt;
        this.responseTimeMinutes = responseTimeMinutes;
        this.isAnswered = isAnswered;
        this.replyCount = replyCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getThreadId() {
        return threadId;
    }

    public void setThreadId(UUID threadId) {
        this.threadId = threadId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID authorId) {
        this.authorId = authorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public Integer getResponseTimeMinutes() {
        return responseTimeMinutes;
    }

    public void setResponseTimeMinutes(Integer responseTimeMinutes) {
        this.responseTimeMinutes = responseTimeMinutes;
    }

    public Boolean getIsAnswered() {
        return isAnswered;
    }

    public void setIsAnswered(Boolean isAnswered) {
        this.isAnswered = isAnswered;
    }

    public Integer getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(Integer replyCount) {
        this.replyCount = replyCount;
    }

    public static DimThread from(EventEnvelope event) {
        return from(event.payload(), event.tenantId(), event.occurredAt());
    }

    // Static Factory Method for Mapping
    public static DimThread from(JsonNode json, String tenantId, Instant occurredAt) {
        UUID threadId = UUID.fromString(json.get("threadId").asText());
        UUID categoryId = json.has("categoryId") ? UUID.fromString(json.get("categoryId").asText()) : null;
        UUID authorId = UUID.fromString(json.get("authorId").asText());
        String title = json.get("title").asText();
        String status = "OPEN"; // Default status

        // Parse tags
        List<String> tags = new ArrayList<>();
        if (json.has("tags")) {
            json.get("tags").forEach(t -> tags.add(t.asText()));
        }

        Instant createdAt = Instant.parse(json.get("createdAt").asText());

        return new DimThread.Builder()
                .threadId(threadId)
                .tenantId(tenantId)
                .categoryId(categoryId)
                .authorId(authorId)
                .title(title)
                .status(status)
                .tags(tags)
                .createdAt(createdAt)
                .lastActivityAt(occurredAt) // Initially last activity is creation time
                .build();
    }

    // Builder Pattern
    public static class Builder {
        private UUID threadId;
        private String tenantId;
        private UUID categoryId;
        private UUID authorId;
        private String title;
        private String status;
        private List<String> tags;
        private Instant createdAt;
        private Instant lastActivityAt;
        private Integer responseTimeMinutes;
        private Boolean isAnswered = false;
        private Integer replyCount = 0;

        public Builder threadId(UUID threadId) {
            this.threadId = threadId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder categoryId(UUID categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public Builder authorId(UUID authorId) {
            this.authorId = authorId;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastActivityAt(Instant lastActivityAt) {
            this.lastActivityAt = lastActivityAt;
            return this;
        }

        public Builder responseTimeMinutes(Integer responseTimeMinutes) {
            this.responseTimeMinutes = responseTimeMinutes;
            return this;
        }

        public Builder isAnswered(Boolean isAnswered) {
            this.isAnswered = isAnswered;
            return this;
        }

        public Builder replyCount(Integer replyCount) {
            this.replyCount = replyCount;
            return this;
        }

        public DimThread build() {
            return new DimThread(threadId, tenantId, categoryId, authorId, title, status, tags, createdAt,
                    lastActivityAt, responseTimeMinutes, isAnswered, replyCount);
        }
    }
}
