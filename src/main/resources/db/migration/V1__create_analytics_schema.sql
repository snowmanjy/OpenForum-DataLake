-- Dimension Tables

CREATE TABLE dim_users (
    user_id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    join_date TIMESTAMP,
    reputation INTEGER,
    is_bot BOOLEAN DEFAULT FALSE
);

CREATE TABLE dim_categories (
    category_id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE dim_threads (
    thread_id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    category_id UUID,
    author_id UUID,
    title VARCHAR(255),
    status VARCHAR(50),
    tags JSONB,
    created_at TIMESTAMP,
    last_activity_at TIMESTAMP,
    response_time_minutes INTEGER,
    is_answered BOOLEAN DEFAULT FALSE,
    reply_count INTEGER DEFAULT 0
);

CREATE INDEX idx_dim_threads_last_activity ON dim_threads(last_activity_at);

CREATE TABLE dim_member_health (
    user_id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    health_score INTEGER,
    churn_risk VARCHAR(50), -- LOW, MEDIUM, HIGH
    engagement_level VARCHAR(50), -- LURKER, CONTRIBUTOR, CHAMPION
    calculated_at TIMESTAMP
);

-- Fact Table with Partitioning

CREATE TABLE fact_activity (
    id UUID,
    event_id UUID, -- For Idempotency
    tenant_id VARCHAR(255) NOT NULL,
    user_id UUID,
    activity_type VARCHAR(50), -- POST_CREATED, THREAD_VIEW, etc.
    target_id UUID,
    occurred_at TIMESTAMP NOT NULL,
    metadata JSONB,
    PRIMARY KEY (id, occurred_at), -- Partition key must be part of PK
    UNIQUE (event_id, occurred_at) -- Unique constraint must include partition key
) PARTITION BY RANGE (occurred_at);

-- Default partition for data that doesn't fit into other partitions (optional but good practice)
CREATE TABLE fact_activity_default PARTITION OF fact_activity DEFAULT;

-- Indexes
CREATE INDEX idx_fact_activity_tenant ON fact_activity(tenant_id);
CREATE INDEX idx_fact_activity_brin_occurred_at ON fact_activity USING BRIN(occurred_at);
