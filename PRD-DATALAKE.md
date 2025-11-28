# OpenForum Data Lake - Product Requirement & Design

## 1. Overview
**Goal:** The Data Lake Service is the "Memory" of the OpenForum platform. It decouples heavy analytics processing from the transactional Core Engine.
**Responsibility:**
1.  Consumes the high-velocity stream of domain events (`outbox_events`) from the Core.
2.  Transforms raw events into read-optimized "Fact" and "Dimension" tables.
3.  Exposes Read-Only APIs for the SaaS Control Plane to display charts (ROI, DAU/MAU, Health).

## 2. Architecture Constraints
* **Input:** Kafka (Topic: `forum-events-v1`).
    * *Events:* `ThreadCreated`, `PostCreated`, `ThreadImported`, `PostImported`, `ReactionAdded`.
* **Storage (Phase 1):** **PostgreSQL 16**.
    * *Why:* Simplifies stack (same as Core/SaaS). Good enough for <100M rows.
    * *Schema:* Star Schema (Fact tables + Dimension tables).
    * *Future Upgrade:* ClickHouse (when row count > 100M).
* **API:** Read-Only REST API (consumed *only* by SaaS Control Plane).
* **Tech Stack:** Java 21, Spring Boot 3.4, Spring Kafka, Flyway.

## 3. Data Model (The "Read" Model)

### 3.1 Fact Tables (Immutable Logs)
* **`fact_activity`**
    * `id` (UUID, PK)
    * `tenant_id` (String, Indexed)
    * `user_id` (UUID)
    * `activity_type` (Enum: POST_CREATED, THREAD_VIEW, REACTION)
    * `occurred_at` (Timestamp, Indexed for time-series queries)
    * `metadata` (JSONB - stores sentiment score, tags, etc.)

### 3.2 Dimension Tables (Current State Snapshots)
* **`dim_threads`**
    * `thread_id` (PK)
    * `tenant_id`
    * `title`
    * `status` (OPEN, CLOSED)
    * `created_at`
    * `response_time_minutes` (Derived metric)
    * `is_answered` (Boolean)

## 4. Key Analytics APIs (Consumed by SaaS Dashboard)

### 4.1 Community Health
* `GET /analytics/v1/activity?metric=dau&interval=day&tenantId=...`
    * *Logic:* Count distinct `user_id` in `fact_activity` grouped by day.
* `GET /analytics/v1/retention?tenantId=...`
    * *Logic:* Cohort analysis (Users who joined Month X vs. Users active Month X+1).

### 4.2 ROI & Value (The "Money" Metrics)
* `GET /analytics/v1/deflection?tenantId=...`
    * *Logic:* Count threads marked "Solved" where the answer came from a non-admin user.
    * *Calculation:* `Deflection Count * CostPerTicket (Configured in SaaS)`.

### 4.3 Operational Queues (For SaaS Automation)
* `GET /analytics/v1/stale-threads?tenantId=...&days=7`
    * *Query:* Select from `dim_threads` where `status=OPEN` AND `last_activity < NOW() - 7 days`.
    * *Usage:* SaaS Automation Service calls this to find threads to auto-archive.

## 5. Implementation Strategy

### Step 1: The Ingestor
* **Component:** `KafkaEventConsumer`
* **Logic:**
    * Listen to `forum-events-v1`.
    * **Idempotency:** Check if `event_id` exists before inserting.
    * **Transformation:**
        * `ThreadImported` -> Upsert `dim_threads` + Insert `fact_activity`.
        * `PostCreated` -> Insert `fact_activity` + Update `dim_threads.last_activity`.

### Step 2: The Aggregator
* **Optimization:** Create **Materialized Views** in Postgres for expensive dashboards (e.g., `view_daily_stats`).
* **Refresh:** Spring `@Scheduled` task refreshes views every hour.

### Step 3: Security
* **Auth:** Validates JWT signed by SaaS Control Plane (RSA Public Key).
* **Isolation:** Every query **MUST** filter by `tenant_id`.