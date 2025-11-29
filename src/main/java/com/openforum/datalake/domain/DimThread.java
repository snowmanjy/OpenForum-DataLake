package com.openforum.datalake.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;
import com.openforum.datalake.ingestor.EventEnvelope;

import java.time.LocalDateTime;
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

    @Column(name = "title")
    private String title;

    @Column(name = "status")
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private List<String> tags;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "response_time_minutes")
    private Integer responseTimeMinutes;

    @Column(name = "is_answered")
    private Boolean isAnswered;

    @Column(name = "reply_count")
    private Integer replyCount;

    public DimThread() {
    }

    public DimThread(UUID threadId, String tenantId, UUID categoryId, UUID authorId, String title, String status,
            List<String> tags, LocalDateTime createdAt, LocalDateTime lastActivityAt, Integer responseTimeMinutes,
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(LocalDateTime lastActivityAt) {
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
        DimThread thread = new DimThread();
        thread.setThreadId(UUID.fromString(event.payload().get("threadId").asText()));
        thread.setTenantId(event.tenantId());
        thread.setCategoryId(UUID.fromString(event.payload().get("categoryId").asText()));
        thread.setAuthorId(UUID.fromString(event.payload().get("authorId").asText()));
        thread.setTitle(event.payload().get("title").asText());
        thread.setStatus("OPEN");

        List<String> tags = new ArrayList<>();
        if (event.payload().has("tags")) {
            event.payload().get("tags").forEach(t -> tags.add(t.asText()));
        }
        thread.setTags(tags);

        LocalDateTime createdAt = LocalDateTime.parse(event.payload().get("createdAt").asText());
        thread.setCreatedAt(createdAt);
        thread.setLastActivityAt(createdAt);
        thread.setReplyCount(0);
        thread.setIsAnswered(false);
        return thread;
    }

    public static class Builder {
        private UUID threadId;
        private String tenantId;
        private UUID categoryId;
        private UUID authorId;
        private String title;
        private String status;
        private List<String> tags;
        private LocalDateTime createdAt;
        private LocalDateTime lastActivityAt;
        private Integer responseTimeMinutes;
        private Boolean isAnswered;
        private Integer replyCount;

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

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder lastActivityAt(LocalDateTime lastActivityAt) {
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
