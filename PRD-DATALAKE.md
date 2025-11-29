# OpenForum Data Lake - Product Requirement & Design

## 1. Overview
**Goal:** The Data Lake Service is the "Memory" of the OpenForum platform. It decouples heavy analytics processing from the transactional Core Engine.
**Responsibility:**
1.  Consumes the high-velocity stream of domain events (`outbox_events`) from the Core.
2.  Transforms raw events into read-optimized "Fact" and "Dimension" tables.
3.  Exposes Read-Only APIs for the SaaS Control Plane to display charts (ROI, DAU/MAU, Health).
**Consistency:** The Data Lake is **Eventually Consistent**. Metrics may lag real-time by a few seconds to minutes depending on Kafka lag.

## 2. Architecture Constraints
* **Input:** Kafka (Topic: `forum-events-v1`).
    * *Events:* `ThreadCreated`, `PostCreated`, `ThreadImported`, `PostImported`, `ReactionAdded`, `SubscriptionCreated`.
* **Storage (Phase 1):** **PostgreSQL 16**.
    * *Strategy:* Star Schema with **Range Partitioning** (Monthly).
    * *Indexes:* **BRIN Index** on `occurred_at` for efficient time-range filtering.
    * *Scale:* Valid up to ~500M rows before migration to ClickHouse.
* **API:** Read-Only REST API (consumed *only* by SaaS Control Plane).
* **Tech Stack:** Java 21, Spring Boot 3.4, Spring Kafka, Flyway.

## 3. Data Model (The "Read" Model)

### 3.1 Fact Tables (Immutable Logs - Partitioned)
* **`fact_activity`**
    * `id` (UUID, PK)
    * `event_id` (UUID, Unique/Indexed) - From Core Outbox ID (Idempotency Key).
    * `tenant_id` (String, Indexed)
    * `user_id` (UUID)
    * `activity_type` (Enum: POST_CREATED, THREAD_VIEW, REACTION, THREAD_CREATED, SUBSCRIPTION_CREATED)
    * `target_id` (UUID)
    * `occurred_at` (Timestamp, Indexed - **Partition Key**)
    * `metadata` (JSONB - stores sentiment score, tags, etc.)
* **Partitioning:** `PARTITION BY RANGE (occurred_at)` (Create monthly partitions).

### 3.2 Dimension Tables (Current State Snapshots)
* **`dim_threads`**
    * `thread_id` (PK)
    * `tenant_id`
    * `category_id` (FK)
    * `author_id` (FK)
    * `title`
    * `status` (OPEN, CLOSED)
    * `tags` (JSONB Array)
    * `created_at`
    * `last_activity_at` (Timestamp, Indexed)
    * `response_time_minutes` (Integer, derived)
    * `is_answered` (Boolean)
    * `reply_count` (Integer)

* **`dim_users`**
    * `user_id` (PK)
    * `tenant_id`
    * `join_date`
    * `reputation`
    * `is_bot`

* **`dim_member_health`** (Derived Daily)
    * `user_id` (PK)
    * `tenant_id`
    * `health_score` (0-100)
    * `churn_risk` (LOW, MEDIUM, HIGH)
    * `engagement_level` (LURKER, CONTRIBUTOR, CHAMPION)
    * `calculated_at` (Timestamp)

## 4. Key Analytics APIs (Consumed by SaaS Dashboard)

### 4.1 Community Health
* `GET /analytics/v1/activity?metric=dau&interval=day&tenantId=...`
    * *Logic:* Count distinct `user_id` in `fact_activity` grouped by day.
* `GET /analytics/v1/retention?tenantId=...`
    * *Logic:* Cohort analysis (Users who joined Month X vs. Users active Month X+1).
* `GET /analytics/v1/responsiveness?tenantId=...`
    * *Returns:* Median `time_to_first_response` and `answer_rate`.

### 4.2 ROI & Value (B2B Metrics)
* `GET /analytics/v1/deflection-savings?tenantId=...`
    * *Logic:* `Solved Threads (Non-Admin) * CostPerTicket`.
    * *Returns:* `{ "deflectionCount": 150, "estimatedSavings": 7500.00 }`.
* `GET /analytics/v1/champions?tenantId=...&minScore=80`
    * *Logic:* Query `dim_member_health` for `engagement_level = CHAMPION`.
* `GET /analytics/v1/churn-risk?tenantId=...&threshold=high`
    * *Logic:* Query `dim_member_health` for `churn_risk = HIGH`.

### 4.3 Operational Queues (For SaaS Automation)
* `GET /analytics/v1/stale-threads?tenantId=...&days=7`
    * *Query:* Select from `dim_threads` where `status=OPEN` AND `last_activity_at < NOW() - 7 days`.
* `GET /analytics/v1/unanswered-threads?tenantId=...&hours=24`
    * *Query:* Select from `dim_threads` where `status=OPEN` AND `reply_count = 0` AND `created_at < NOW() - 24 hours`.

## 5. Implementation Strategy

### Step 1: The Ingestor (`KafkaEventConsumer`)
* **Idempotency:** `INSERT INTO fact_activity ... ON CONFLICT (event_id) DO NOTHING`.
* **Mapping:**
    * `ThreadCreated` -> Insert `dim_threads`, Insert `fact_activity`.
    * `PostCreated` -> 
        1. Insert `fact_activity` (POST_CREATED).
        2. Update `dim_threads`: set `last_activity_at = NOW()`, `reply_count++`.
        3. Calculate `response_time` if `reply_count` was 0.
    * `ThreadImported` -> Bulk Insert `dim_threads` + `fact_activity`.

### Step 2: The Aggregator (Scheduled Jobs)
* **Hourly:** Refresh Materialized Views for `view_daily_active_users`.
* **Daily:** Run `MemberHealthCalculationJob`.
    * *Logic:* Reads last 30 days `fact_activity` -> Calculates Score -> Upserts `dim_member_health`.

### Step 3: Security
* **Auth:** Validates JWT signed by SaaS Control Plane.
* **Isolation:** Mandatory `tenant_id` filter on ALL queries.