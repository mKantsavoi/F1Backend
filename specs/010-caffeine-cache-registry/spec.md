# Feature Specification: Caffeine Cache Registry

**Feature Branch**: `010-caffeine-cache-registry`
**Created**: 2026-03-28
**Status**: Draft
**Input**: User description: "Replace all custom in-memory caching with Caffeine library using a centralized CacheRegistry pattern and Kotlin Coroutines-native suspending caches."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - All Endpoints Continue Working Identically (Priority: P1)

As an API consumer, all existing endpoints (schedules, race results, qualifying, sprint, standings, circuits) must return the same data with the same freshness behavior. This is an internal refactoring — no external behavior changes.

**Why this priority**: This is the core guarantee of the refactoring. If any endpoint returns different data or behaves differently, the migration has failed. Every other story depends on this one.

**Independent Test**: Can be tested by running the full existing test suite and manually hitting every endpoint before and after the migration, comparing response bodies, status codes, and Warning headers.

**Acceptance Scenarios**:

1. **Given** the system is running with the new caching implementation, **When** a user requests any existing endpoint (e.g., GET /api/v1/schedule/2025), **Then** the response body, status code, and headers are identical to the previous implementation.
2. **Given** a cache entry has expired and the upstream data source is unavailable, **When** a user requests that data, **Then** the system returns the last known data with a Warning header (stale-on-error behavior preserved).
3. **Given** a cache entry has expired and the upstream fetch fails, **When** a second request arrives within the retry throttle window, **Then** the system serves stale data without reattempting the upstream call.
4. **Given** the system has been running for an extended period, **When** many unique cache keys have been created, **Then** the system automatically evicts entries beyond the configured maximum size (bounded memory).

---

### User Story 2 - Centralized Cache Configuration (Priority: P2)

As a developer maintaining the system, all cache configuration (freshness duration, maximum entries) is defined in a single location. No cache parameters are scattered across use case files.

**Why this priority**: The primary developer-facing benefit of this refactoring. Centralizing configuration eliminates duplicated TTL logic, reduces the chance of misconfiguration, and makes it trivial to audit or adjust cache behavior.

**Independent Test**: Can be verified by inspecting the codebase for any hardcoded cache durations or sizes outside the central configuration, and by changing a single configuration value and confirming it takes effect.

**Acceptance Scenarios**:

1. **Given** a developer wants to change the freshness duration for race results, **When** they modify the single central cache configuration, **Then** the change applies to all race result caching without editing any other file.
2. **Given** the central configuration defines a maximum cache size, **When** the cache reaches that limit, **Then** the least-recently-used entries are evicted automatically.
3. **Given** the system defines nine distinct cache categories (schedule, next race, race results, historical race results, qualifying, sprint, driver standings, constructor standings, circuits), **When** a developer reviews the configuration, **Then** all nine are visible in one place with their freshness duration and size limit.

---

### User Story 3 - Non-Blocking Cache Operations (Priority: P2)

As the system under load, cache population (fetching data from the upstream data source) must not block application threads. Cache loaders must execute as non-blocking coroutine operations.

**Why this priority**: The application is fully coroutine-based. Blocking a thread during cache population degrades throughput under concurrent requests. This is tied with P2 because it's a correctness improvement that eliminates a class of performance issues.

**Independent Test**: Can be tested by searching the codebase for any thread-blocking cache operations (e.g., runBlocking) and confirming none exist, and by load-testing concurrent requests to verify no thread starvation.

**Acceptance Scenarios**:

1. **Given** multiple users request data simultaneously for an uncached key, **When** the cache begins populating, **Then** the data fetch executes as a non-blocking operation without holding a platform thread.
2. **Given** the system is under concurrent load, **When** cache misses trigger upstream fetches, **Then** no thread starvation or request timeouts occur due to blocking cache operations.

---

### Edge Cases

- What happens when the upstream data source fails on the very first request (no stale data exists)? The system throws an appropriate error indicating the data source is unavailable.
- What happens when the cache reaches its maximum size? Least-recently-used entries are automatically evicted; no manual intervention needed.
- What happens when two concurrent requests trigger a cache miss for the same key? Only one upstream fetch occurs (thundering herd prevention preserved); the second request waits for the first to complete.
- What happens when the system restarts? All caches start empty (in-memory only); the first request for each key triggers a fresh fetch.
- What happens if a cache loader throws an unexpected exception type? The stale-on-error fallback still activates if stale data is available; otherwise the error propagates as an appropriate HTTP error.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST replace all custom hand-written cache implementations with a single production-grade caching library.
- **FR-002**: System MUST define all cache configurations (freshness duration and maximum entry count) in a single centralized location.
- **FR-003**: System MUST support fourteen distinct cache categories: schedule, next race, race results, historical race results, qualifying results, historical qualifying results, sprint results, historical sprint results, driver standings, historical driver standings, constructor standings, historical constructor standings, circuits, and personalized feed.
- **FR-004**: System MUST preserve existing freshness durations for each cache category: schedule (configurable, currently sourced from application config), next race (1 hour), race/qualifying/sprint results for current season (5 minutes), historical results (effectively permanent), driver standings current season (1 hour), constructor standings current season (1 hour), circuits (effectively permanent), and personalized feed (30 seconds).
- **FR-005**: System MUST enforce maximum entry limits per cache category to bound memory usage.
- **FR-006**: System MUST automatically evict expired entries without manual intervention.
- **FR-007**: System MUST serve stale cached data with a Warning header when the upstream data source is unavailable and a previously fetched value exists (stale-on-error fallback).
- **FR-008**: System MUST execute cache population (data fetching) as non-blocking coroutine operations, with no thread-blocking calls in cache loaders.
- **FR-009**: System MUST remove all custom cache interfaces, in-memory cache implementations, and manual TTL-checking domain models that are replaced by the centralized solution.
- **FR-010**: System MUST preserve thundering herd prevention — concurrent requests for the same uncached key must not trigger duplicate upstream fetches.
- **FR-011**: System MUST preserve retry throttling — failed upstream fetches must not be retried within the existing throttle window (60 seconds).
- **FR-012**: All existing endpoint response formats, status codes, and header behavior MUST remain unchanged.

### Key Entities

- **Cache Category**: A named cache with a defined freshness duration and maximum entry count. Fourteen categories exist (see FR-003). Each category is independently configured.
- **Cache Entry**: A stored value associated with a key within a cache category. Automatically expires after the category's configured freshness duration.
- **Stale Fallback**: A secondary copy of the last successfully fetched value per key, used when a fresh fetch fails and the primary cache entry has expired.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All existing API endpoints return identical responses (body, status code, headers) before and after the migration — zero regressions.
- **SC-002**: Zero custom cache implementations remain in the codebase — all caching is handled by the centralized solution.
- **SC-003**: All cache configurations (freshness durations and size limits) are defined in exactly one location — no scattered TTL constants across use case files.
- **SC-004**: Zero thread-blocking calls (e.g., runBlocking) exist in cache-related code paths.
- **SC-005**: Cache memory usage is bounded — every cache category has a defined maximum entry count, and entries beyond the limit are automatically evicted.
- **SC-006**: All existing automated tests pass without modification (beyond cache wiring changes).
- **SC-007**: Stale-on-error behavior is preserved — when the upstream source fails and stale data exists, the system returns the stale data with a Warning header.
- **SC-008**: All code quality checks (linting, static analysis, tests) pass cleanly.

## Assumptions

- This is a pure internal refactoring — no new external-facing features are added.
- In-memory caching is sufficient — distributed caching (e.g., Redis) is out of scope.
- The existing retry throttle window of 60 seconds is preserved as-is.
- The "effectively permanent" TTL for historical/static data (past season results, circuits) can be represented as a very long duration (e.g., 365 days) rather than literal infinity.
- The Schedule cache TTL continues to be sourced from application configuration (JolpicaConfig.cacheTtlHours) rather than hardcoded.
- SeasonCache<T> wrapper may be preserved if it serves a purpose beyond caching (i.e., carrying the resolved season string to the response layer); only the caching-related aspects are removed.
