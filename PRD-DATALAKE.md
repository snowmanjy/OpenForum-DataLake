# OpenForum Data Lake - Product Requirement & Design

## 1. Overview
**Goal:** The Data Lake Service is the "Memory" of the OpenForum platform. It decouples heavy analytics processing from the transactional Core Engine.
**Responsibility:**
1.  Consumes the high-velocity stream of domain events (`outbox_events`) from the Core.
2.  Transforms raw events into read-optimized "Fact" and "Dimension" tables.
3.  Exposes Read-Only APIs for the SaaS Control Plane to display charts (ROI, DAU/MAU, Health, Trending).

## 2. Architecture Constraints
* **Input:** Kafka (Topic: `forum-events-v1`).
* **Storage (Phase 1):** **PostgreSQL 16**.
    * *Schema:* Star Schema (Fact tables + Dimension tables).
* **API:** Read-Only REST API (consumed *only* by SaaS Control Plane).
* **Tech Stack:** Java 21, Spring Boot 3.4, Spring Kafka, Flyway.

## 3. Data Model (The "Read" Model)

### 3.1 Dimension Tables (State Snapshots)
* **`dim_users`**
    * `user_id` (PK)
    * `tenant_id`
    * `reputation`
    * `join_date`
* **`dim_categories`**
    * `category_id` (PK)
    * `tenant_id`
    * `name`
    * `slug`
* **`dim_tags`**
    * `tag_id` (PK)
    * `tenant_id`
    * `name`
* **`dim_threads`**
    * `thread_id` (PK)
    * `category_id` (FK)
    * `tenant_id`
    * `author_id`
    * `title`
    * `status` (OPEN, CLOSED)
    * `tags` (Text Array or JSONB for tag names)
    * `created_at`
    * `is_answered` (Boolean)

### 3.2 Fact Tables (Immutable Logs)
* **`fact_activity`** (The main timeline)
    * `id` (UUID, PK)
    * `tenant_id` (Indexed)
    * `user_id` (Indexed)
    * `activity_type` (Enum: POST_CREATED, THREAD_VIEW, SUBSCRIPTION_CREATED, REACTION_ADDED, SEARCH_PERFORMED)
    * `target_id` (UUID - ThreadID, CategoryID, or PostID)
    * `occurred_at` (Timestamp, Indexed)
    * `metadata` (JSONB)
        * *Search:* `{"query": "java error", "results_count": 0}`
        * *Subscription:* `{"target_type": "CATEGORY"}`

## 4. Key Analytics APIs (Consumed by SaaS Dashboard)

### 4.1 Community Health
* `GET /analytics/v1/activity?metric=dau&interval=day`
    * *Logic:* Count distinct `user_id` in `fact_activity` grouped by day.
* `GET /analytics/v1/retention`
    * *Logic:* Cohort analysis (Users who joined Month X vs. Users active Month X+1).

### 4.2 Content Insights (Categories & Tags)
* `GET /analytics/v1/categories/performance`
    * *Returns:* List of Categories with `post_count`, `view_count`, `active_users`.
    * *Usage:* Helps CMs decide which categories to merge or promote.
* `GET /analytics/v1/tags/trending?days=7`
    * *Returns:* Top 10 tags used in `fact_activity` (POST_CREATED) in the last 7 days.

### 4.3 Search Intelligence
* `GET /analytics/v1/search/queries?sort=no_results`
    * *Query:* `SELECT metadata->>'query', count(*) FROM fact_activity WHERE activity_type='SEARCH_PERFORMED' AND (metadata->>'results_count')::int = 0 GROUP BY 1`
    * *Value:* Shows CMs what content is missing (Data Gap Analysis).

### 4.4 ROI & Value
* `GET /analytics/v1/deflection`
    * *Logic:* Count threads marked "Solved" (from `dim_threads`) where the answer came from a non-admin user.

## 5. Implementation Strategy

### Step 1: The Ingestor (`KafkaEventConsumer`)
* **Map Events to Data Model:**
    * `CategoryCreated` -> Insert `dim_categories`.
    * `ThreadCreated` -> Insert `dim_threads` (lookup category name).
    * `PostCreated` -> 
        1. Insert `fact_activity` (Type: POST_CREATED).
        2. Parse `#hashtags` and upsert `dim_tags`? (Or rely on `TagCreated` event from Core).
        3. Check `mentionedUserIds` -> Insert `fact_activity` (Type: MENTION) for the target user?
    * `SubscriptionCreated` -> Insert `fact_activity` (Type: SUBSCRIPTION_CREATED).

### Step 2: The Aggregator
* **Materialized Views:**
    * `view_trending_tags`: Aggregates tag usage per day.
    * `view_category_stats`: Aggregates posts/views per category.
* **Refresh Job:** `@Scheduled` every hour to refresh Postgres Materialized Views.

### Step 3: Security
* **Auth:** Validates JWT signed by SaaS Control Plane.
* **Isolation:** `tenant_id` is mandatory in every SQL `WHERE` clause.