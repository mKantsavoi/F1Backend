# Tasks: F1 Drivers Endpoint

**Input**: Design documents from `/specs/002-f1-drivers-endpoint/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add Ktor HTTP client dependencies and Jolpica configuration

- [x] T001 Add ktor-client-core, ktor-client-cio, ktor-client-mock to gradle/libs.versions.toml and update build.gradle.kts (promote ktor-client-content-negotiation from testImplementation to implementation, add new client deps)
- [x] T002 Add Jolpica configuration section (baseUrl, requestTimeoutMs, connectTimeoutMs, cacheTtlHours) to src/main/resources/application.yaml and create JolpicaConfig data class in src/main/kotlin/com/blaizmiko/infrastructure/config/AppConfig.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Domain models, ports, and infrastructure implementations that ALL user stories depend on

**CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 [P] Create Driver domain entity (pure Kotlin data class, no framework imports) in src/main/kotlin/com/blaizmiko/domain/model/Driver.kt
- [x] T004 [P] Create generic CacheEntry<T> value object (data, fetchedAt, expiresAt, isFresh() helper) in src/main/kotlin/com/blaizmiko/domain/model/CacheEntry.kt
- [x] T005 [P] Create DriverDataSource port interface (suspend fun fetchDrivers(season: String): List<Driver>) in src/main/kotlin/com/blaizmiko/domain/port/DriverDataSource.kt
- [x] T006 [P] Create DriverCache port interface (get/put methods with CacheEntry) in src/main/kotlin/com/blaizmiko/domain/port/DriverCache.kt
- [x] T007 [P] Create ExternalServiceException in src/main/kotlin/com/blaizmiko/domain/model/Exceptions.kt for 502 error handling
- [x] T008 [P] Create Jolpica response @Serializable models (JolpicaResponse, MRData, DriverTable, JolpicaDriver) in src/main/kotlin/com/blaizmiko/infrastructure/external/JolpicaModels.kt
- [x] T009 Create JolpicaClient implementing DriverDataSource with Ktor HttpClient (CIO engine, ContentNegotiation, HttpRequestRetry with 2 retries + exponential backoff, HttpTimeout 10s/5s), response mapping from JolpicaDriver to Driver domain entity in src/main/kotlin/com/blaizmiko/infrastructure/external/JolpicaClient.kt
- [x] T010 Create InMemoryDriverCache implementing DriverCache with ConcurrentHashMap<String, CacheEntry<List<Driver>>> keyed by season year, plus per-key Mutex for concurrent request coalescing in src/main/kotlin/com/blaizmiko/infrastructure/cache/InMemoryDriverCache.kt
- [x] T011 Create DriversResponse and DriverDto @Serializable response DTOs in src/main/kotlin/com/blaizmiko/adapter/dto/DriverResponses.kt

**Checkpoint**: All building blocks ready — domain, ports, infrastructure, and DTOs in place

---

## Phase 3: User Story 1 - View Current Season Drivers (Priority: P1) MVP

**Goal**: Authenticated user can GET /api/v1/drivers and receive all current season F1 drivers

**Independent Test**: Make authenticated request to /api/v1/drivers, verify response contains season field and drivers array with all expected fields (id, number, code, firstName, lastName, nationality, dateOfBirth)

### Implementation for User Story 1

- [x] T012 [US1] Create GetDrivers use-case (takes DriverCache + DriverDataSource, orchestrates: check cache → if fresh return cached → else fetch from source → store in cache → return; no season parameter handling yet, defaults to "current") in src/main/kotlin/com/blaizmiko/usecase/GetDrivers.kt
- [x] T013 [US1] Create driverRoutes extension function with GET /drivers route (extract JWT principal, call GetDrivers use-case with "current" season, map Driver domain list to DriversResponse DTO) in src/main/kotlin/com/blaizmiko/adapter/route/DriverRoutes.kt
- [x] T014 [US1] Wire driver routes into configureRouting: instantiate JolpicaClient, InMemoryDriverCache, mount route at /api/v1 with authenticate block in src/main/kotlin/com/blaizmiko/Routing.kt
- [x] T015 [US1] Add ExternalServiceException handler to StatusPages (502 Bad Gateway with ErrorResponse) in src/main/kotlin/com/blaizmiko/Routing.kt

**Checkpoint**: GET /api/v1/drivers returns current season drivers for authenticated users

---

## Phase 4: User Story 5 - Unauthorized Access Prevention (Priority: P1)

**Goal**: Unauthenticated requests to /api/v1/drivers are rejected with 401

**Independent Test**: Request /api/v1/drivers without token → 401; with invalid token → 401

### Implementation for User Story 5

- [x] T016 [US5] Verify driver route is inside authenticate block and 401 challenge is returned for missing/invalid/expired tokens (this should already work from T014 wiring — verify and fix if needed) in src/main/kotlin/com/blaizmiko/adapter/route/DriverRoutes.kt

**Checkpoint**: 401 returned for all unauthenticated requests to /api/v1/drivers

---

## Phase 5: User Story 2 - View Drivers for a Specific Season (Priority: P2)

**Goal**: User can pass ?season=2024 to get drivers from a specific year

**Independent Test**: Request /api/v1/drivers?season=2024 → drivers for 2024; ?season=abc → 400 error

### Implementation for User Story 2

- [x] T017 [US2] Add season query parameter parsing and validation (must be 4-digit year between 1950 and current year, or absent for "current") to driverRoutes in src/main/kotlin/com/blaizmiko/adapter/route/DriverRoutes.kt
- [x] T018 [US2] Update GetDrivers use-case to accept season parameter and pass it to both DriverCache and DriverDataSource in src/main/kotlin/com/blaizmiko/usecase/GetDrivers.kt
- [x] T019 [US2] Update JolpicaClient to build URL dynamically: /f1/{season}/drivers.json where season is either a year or "current" in src/main/kotlin/com/blaizmiko/infrastructure/external/JolpicaClient.kt

**Checkpoint**: Season parameter works for both current and historical seasons with proper validation

---

## Phase 6: User Story 3 - Fast Response from Cached Data (Priority: P2)

**Goal**: Repeated requests within 24h are served from cache without hitting Jolpica

**Independent Test**: Make two consecutive requests for the same season — second should not trigger external fetch

### Implementation for User Story 3

- [x] T020 [US3] Verify cache hit path in GetDrivers use-case returns cached data when CacheEntry.isFresh() is true, with no call to DriverDataSource in src/main/kotlin/com/blaizmiko/usecase/GetDrivers.kt
- [x] T021 [US3] Verify InMemoryDriverCache correctly checks expiry (fetchedAt + cacheTtlHours) and returns null for expired entries via isFresh() in src/main/kotlin/com/blaizmiko/infrastructure/cache/InMemoryDriverCache.kt

**Checkpoint**: Cache prevents redundant external API calls within TTL window

---

## Phase 7: User Story 4 - Graceful Degradation (Priority: P3)

**Goal**: When Jolpica is down, stale cached data is returned with Warning header; if no cache exists, 502 is returned

**Independent Test**: Simulate Jolpica failure with stale cache → 200 + Warning header; without cache → 502

### Implementation for User Story 4

- [x] T022 [US4] Update GetDrivers use-case to catch external fetch failures: if stale cache exists return it with a stale flag, if no cache exists throw ExternalServiceException in src/main/kotlin/com/blaizmiko/usecase/GetDrivers.kt
- [x] T023 [US4] Update driverRoutes to check stale flag from GetDrivers result and add Warning: 110 - "Response is stale" header to response when serving stale data in src/main/kotlin/com/blaizmiko/adapter/route/DriverRoutes.kt

**Checkpoint**: Graceful degradation works — stale cache served with warning header, 502 when no cache

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Integration test, OpenAPI docs, and final validation

- [x] T024 [P] Create integration test (DriversEndpointTest) using ktor-server-test-host + ktor-client-mock: test happy path (mock Jolpica returns drivers → verify 200 + correct JSON), test 401 without token, test cache hit (second request doesn't call mock), test stale fallback (mock fails + stale cache → 200 + Warning header), test 502 (mock fails + no cache) in src/test/kotlin/com/blaizmiko/integration/DriversEndpointTest.kt
- [ ] T025 [P] [DEFERRED] Add OpenAPI/Swagger annotations for GET /api/v1/drivers endpoint (request params, response schemas, error responses) per constitution requirement in src/main/kotlin/com/blaizmiko/adapter/route/DriverRoutes.kt
- [x] T026 Run quickstart.md validation: start server, obtain JWT token, call GET /api/v1/drivers, verify response matches contract

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (deps must be available)
- **US1 (Phase 3)**: Depends on Phase 2 — core MVP
- **US5 (Phase 4)**: Depends on Phase 3 (route must exist to verify auth)
- **US2 (Phase 5)**: Depends on Phase 3 (extends existing route + use-case)
- **US3 (Phase 6)**: Depends on Phase 3 (verifies cache behavior already built)
- **US4 (Phase 7)**: Depends on Phase 3 + Phase 6 (needs cache to test degradation)
- **Polish (Phase 8)**: Depends on all user stories complete

### User Story Dependencies

- **US1 (P1)**: After Foundational — no story dependencies
- **US5 (P1)**: After US1 — verifies auth on existing route
- **US2 (P2)**: After US1 — extends route with season param
- **US3 (P2)**: After US1 — validates cache behavior
- **US4 (P3)**: After US1 + US3 — needs cache populated to test degradation
- **US2 and US3 can run in parallel** after US1 is complete

### Within Each User Story

- Models before services
- Services before endpoints
- Core implementation before integration

### Parallel Opportunities

- T003, T004, T005, T006, T007, T008 can all run in parallel (Phase 2 — different files, no dependencies)
- T024, T025 can run in parallel (Phase 8 — different concerns)
- US2 and US3 can run in parallel after US1 is complete

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Launch all domain models and ports together:
Task: "Create Driver entity in src/main/kotlin/com/blaizmiko/domain/model/Driver.kt"
Task: "Create CacheEntry in src/main/kotlin/com/blaizmiko/domain/model/CacheEntry.kt"
Task: "Create DriverDataSource port in src/main/kotlin/com/blaizmiko/domain/port/DriverDataSource.kt"
Task: "Create DriverCache port in src/main/kotlin/com/blaizmiko/domain/port/DriverCache.kt"
Task: "Create ExternalServiceException in src/main/kotlin/com/blaizmiko/domain/model/Exceptions.kt"
Task: "Create JolpicaModels in src/main/kotlin/com/blaizmiko/infrastructure/external/JolpicaModels.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (dependencies + config)
2. Complete Phase 2: Foundational (domain models, ports, infrastructure)
3. Complete Phase 3: User Story 1 (GET /api/v1/drivers with current season)
4. **STOP and VALIDATE**: Manually test endpoint returns drivers
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Building blocks ready
2. US1 → Current season drivers work → MVP!
3. US5 → Auth verified (should already work from US1 wiring)
4. US2 + US3 (parallel) → Season param + cache validation
5. US4 → Graceful degradation
6. Polish → Integration tests + OpenAPI docs

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- No database is needed for this feature — all data comes from external API + in-memory cache
- JolpicaClient should be designed for reuse (future endpoints: standings, races, results)
- CacheEntry<T> is generic — reusable for any cached data type
- Constitution requires Context7 verification before using any new dependency APIs
- Commit after each task or logical group
