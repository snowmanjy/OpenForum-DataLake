# OpenForum Data Lake - Product Requirement & Design

## 1. Overview
The Data Lake Service is the "Memory" of the platform. It consumes the stream of changes from the Core Engine (via the Outbox Pattern) and restructures them into query-optimized formats for Analytics.

## 2. Architecture Constraints
* **Input:** Kafka (Topic: `forum-events-v1`).
* **Storage:** PostgreSQL 16 (with partitioning) OR ClickHouse (Future).
    * *Decision:* Start with **Postgres** using a "Star Schema" (Fact tables + Dimension tables) for simplicity in Phase 2.
* **API:** Read-Only REST API (consumed by SaaS Control Plane).
* **Tech Stack:** Java 21, Spring Boot 3.4, Kafka Streams (or simple Consumer).

## 3. Data Model (The "Read" Model)
Unlike the Core (which is normalized for ACID), the Lake is denormalized for speed.

### 3.1 Fact Tables (Events)
* **`fact_activity`**
    * `time_bucket` (Hour/Day)
    * `tenant_id`
    * `user_id`
    * `activity_type` (POST_CREATED, THREAD_VIEW, REACTION_ADDED)
    * `metadata` (JSONB - e.g., "sentiment_score")

### 3.2 Dimension Tables (State Snapshots)
* **`dim_threads`**
    * `thread_id`
    * `title`
    * `status`
    * `tags` (Array)
    * `created_at`
    * `is_answered` (Boolean)

## 4. Key Analytics Features (The APIs)

### 4.1 Community Health
* `GET /analytics/v1/activity?metric=dau&interval=day`
    * Returns: Daily Active Users count.
* `GET /analytics/v1/retention`
    * Returns: Cohort analysis (User joined in Jan -> % active in Feb).

### 4.2 ROI & Value
* `GET /analytics/v1/deflection`
    * Logic: Count threads marked "Solved" where the answer came from a Non-Admin user.
    * Value: `Count * $CostPerTicket` (Configurable).

### 4.3 Operational Queues (For SaaS Workflows)
* `GET /analytics/v1/stale-threads`
    * Returns: Threads with `status=OPEN` and `last_reply > 7 days`.
    * *Used By:* SaaS Automation Engine (to trigger "Archive" jobs).

## 5. Implementation Plan

### Step 1: The Ingestor
* Build `KafkaEventConsumer`.
* Listen to `ThreadCreatedEvent`, `PostCreatedEvent`, `ThreadImportedEvent`.
* Map events to `fact_activity` table.

### Step 2: The Aggregator
* Create Materialized Views in Postgres for "Daily Stats" (to make charts fast).
    * `view_daily_stats_per_tenant`
* Write a Scheduled Job (`@Scheduled`) to refresh views every hour.

### Step 3: The API
* Implement `AnalyticsController`.
* Secure with JWT (signed by SaaS Platform RSA Key).