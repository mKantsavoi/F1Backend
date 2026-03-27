# Tasks: Championship Standings Endpoints

**Input**: Design documents from `/specs/007-championship-standings/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Included — spec requires integration tests covering happy path, auth, cache, error fallback, and validation per endpoint.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No project initialization needed — project already exists with established patterns.

This phase is empty. All infrastructure (Ktor, Koin, JWT auth, StatusPages, Jolpica HTTP client) is already in place.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared domain models, data source interface, Jolpica client, and Jolpica response models that BOTH user stories depend on.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T001 [P] Create DriverStanding domain model in src/main/kotlin/com/blaizmiko/f1backend/domain/model/DriverStanding.kt
- [x] T002 [P] Create ConstructorStanding domain model in src/main/kotlin/com/blaizmiko/f1backend/domain/model/ConstructorStanding.kt
- [x] T003 [P] Create StandingsData<T> wrapper in src/main/kotlin/com/blaizmiko/f1backend/domain/model/StandingsData.kt
- [x] T004 Create StandingsDataSource port interface with fetchDriverStandings and fetchConstructorStandings in src/main/kotlin/com/blaizmiko/f1backend/domain/port/StandingsDataSource.kt
- [x] T005 Add Jolpica standings response models (JolpicaDriverStandingsResponse, JolpicaConstructorStandingsResponse, StandingsTable, StandingsList, JolpicaDriverStanding, JolpicaConstructorStanding) to src/main/kotlin/com/blaizmiko/f1backend/infrastructure/external/JolpicaModels.kt
- [x] T006 Add standings toDomain() mapper extensions (JolpicaDriverStanding to DriverStanding, JolpicaConstructorStanding to ConstructorStanding) in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/external/JolpicaMappers.kt
- [x] T007 Create JolpicaStandingsClient implementing StandingsDataSource with fetchDriverStandings and fetchConstructorStandings methods in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/external/client/JolpicaStandingsClient.kt

**Checkpoint**: Foundational layer complete — Jolpica standings data can be fetched and mapped to domain models.

---

## Phase 3: User Story 1 — View Current Driver Championship Standings (Priority: P1) MVP

**Goal**: Authenticated users can request driver championship standings for the current season and receive a correctly formatted, position-ordered list.

**Independent Test**: Request GET /api/v1/standings/drivers without a season parameter and verify a correctly formatted driver standings response is returned.

### Implementation for User Story 1

- [x] T008 [P] [US1] Create DriverStandingsCache interface with get/put keyed by season in src/main/kotlin/com/blaizmiko/f1backend/adapter/port/DriverStandingsCache.kt
- [x] T009 [P] [US1] Create DriverStandingsResponse and DriverStandingDto serializable DTOs in src/main/kotlin/com/blaizmiko/f1backend/adapter/dto/StandingsResponses.kt
- [x] T010 [US1] Create InMemoryDriverStandingsCache using ConcurrentHashMap in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/cache/InMemoryDriverStandingsCache.kt
- [x] T011 [US1] Create GetDriverStandings use case with mutex, retry throttle, stale fallback, and current-vs-past TTL logic (1h current, forever past) in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetDriverStandings.kt
- [x] T012 [US1] Create StandingsRoutes with GET /standings/drivers endpoint (Koin inject, validateSeason, Warning header for stale, map domain to DTO) in src/main/kotlin/com/blaizmiko/f1backend/adapter/route/StandingsRoutes.kt
- [x] T013 [US1] Create StandingsModule registering DriverStandingsCache, JolpicaStandingsClient, and GetDriverStandings in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/StandingsModule.kt
- [x] T014 [US1] Register standingsModule in Application.kt modules() call and add standingsRoutes() to authenticated route block in Routing.kt
- [x] T015 [US1] Create DriverStandingsEndpointTest with FakeStandingsDataSource covering: happy path, 401 without token, cache hit, stale fallback with Warning header, 502 when no cache, 400 for invalid season in src/test/kotlin/com/blaizmiko/f1backend/integration/DriverStandingsEndpointTest.kt

**Checkpoint**: Driver standings endpoint is fully functional and independently testable. Users can view current driver championship standings.

---

## Phase 4: User Story 2 — View Current Constructor Championship Standings (Priority: P1)

**Goal**: Authenticated users can request constructor championship standings for the current season and receive a correctly formatted, position-ordered list.

**Independent Test**: Request GET /api/v1/standings/constructors without a season parameter and verify a correctly formatted constructor standings response is returned.

### Implementation for User Story 2

- [x] T016 [P] [US2] Create ConstructorStandingsCache interface with get/put keyed by season in src/main/kotlin/com/blaizmiko/f1backend/adapter/port/ConstructorStandingsCache.kt
- [x] T017 [P] [US2] Add ConstructorStandingsResponse and ConstructorStandingDto to src/main/kotlin/com/blaizmiko/f1backend/adapter/dto/StandingsResponses.kt
- [x] T018 [US2] Create InMemoryConstructorStandingsCache using ConcurrentHashMap in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/cache/InMemoryConstructorStandingsCache.kt
- [x] T019 [US2] Create GetConstructorStandings use case with mutex, retry throttle, stale fallback, and current-vs-past TTL logic in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetConstructorStandings.kt
- [x] T020 [US2] Add GET /standings/constructors endpoint to StandingsRoutes in src/main/kotlin/com/blaizmiko/f1backend/adapter/route/StandingsRoutes.kt
- [x] T021 [US2] Register ConstructorStandingsCache and GetConstructorStandings in StandingsModule in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/StandingsModule.kt
- [x] T022 [US2] Create ConstructorStandingsEndpointTest with FakeStandingsDataSource covering: happy path, 401 without token, cache hit, stale fallback with Warning header, 502 when no cache, 400 for invalid season in src/test/kotlin/com/blaizmiko/f1backend/integration/ConstructorStandingsEndpointTest.kt

**Checkpoint**: Both standings endpoints are fully functional. Users can view both driver and constructor championship standings.

---

## Phase 5: User Story 3 — View Historical Season Standings (Priority: P2)

**Goal**: Users can specify a past season year to view final championship standings for completed seasons. Past seasons are cached indefinitely.

**Independent Test**: Request GET /api/v1/standings/drivers?season=2023 and verify correct historical standings are returned.

### Implementation for User Story 3

No additional implementation tasks required. Historical season support is built into the use cases (GetDriverStandings, GetConstructorStandings) via the season parameter and current-vs-past TTL logic already implemented in Phase 3 and Phase 4.

- [x] T023 [US3] Add historical season test cases to DriverStandingsEndpointTest: request with past season parameter returns correct data, past season uses permanent cache in src/test/kotlin/com/blaizmiko/f1backend/integration/DriverStandingsEndpointTest.kt
- [x] T024 [US3] Add historical season test cases to ConstructorStandingsEndpointTest: request with past season parameter returns correct data, past season uses permanent cache in src/test/kotlin/com/blaizmiko/f1backend/integration/ConstructorStandingsEndpointTest.kt

**Checkpoint**: Historical standings verified — users can view any past season's final standings.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation that all quality gates pass and no existing functionality is broken.

- [x] T025 Run ./gradlew ktlintCheck detekt and fix any violations
- [x] T026 Run ./gradlew test and verify all existing tests pass alongside new standings tests
- [x] T027 Verify OpenAPI/Swagger documentation includes new standings endpoints

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Empty — project already initialized
- **Foundational (Phase 2)**: BLOCKS all user stories — domain models, data source, Jolpica client must exist first
- **User Story 1 (Phase 3)**: Depends on Phase 2 completion
- **User Story 2 (Phase 4)**: Depends on Phase 2 completion. Can run in PARALLEL with Phase 3 (different cache, use case, and test files) — but shares StandingsRoutes.kt and StandingsModule.kt, so T020/T021 depend on T012/T013
- **User Story 3 (Phase 5)**: Depends on Phase 3 + Phase 4 completion (adds test cases to existing test files)
- **Polish (Phase 6)**: Depends on all phases complete

### User Story Dependencies

- **US1 (Driver Standings)**: Depends only on Foundational phase. No dependencies on other stories.
- **US2 (Constructor Standings)**: Depends on Foundational phase. Shares StandingsRoutes.kt and StandingsModule.kt with US1, so US2 route/module tasks extend US1's files.
- **US3 (Historical Standings)**: Depends on US1 + US2 implementation existing. Adds test coverage only.
- **US4 (Stale Fallback)**: Built into US1 and US2 use case implementations. Tested within US1/US2 test files.
- **US5 (Auth Required)**: Built into route configuration. Tested within US1/US2 test files.

### Within Each User Story

- Cache interface before cache implementation
- Cache + data source before use case
- Use case before route handler
- Route handler before Koin module registration
- All implementation before integration tests

### Parallel Opportunities

- T001, T002, T003 can run in parallel (independent domain model files)
- T008, T009 can run in parallel (different files, no dependencies)
- T016, T017 can run in parallel (different files, no dependencies)
- T023, T024 can run in parallel (different test files)
- US1 and US2 can partially overlap (separate caches, use cases, tests) but share route/module files

---

## Parallel Example: Foundational Phase

```text
# Launch domain models in parallel:
Task T001: "Create DriverStanding model in domain/model/DriverStanding.kt"
Task T002: "Create ConstructorStanding model in domain/model/ConstructorStanding.kt"
Task T003: "Create StandingsData<T> wrapper in domain/model/StandingsData.kt"
```

## Parallel Example: User Story 1

```text
# Launch cache interface and DTOs in parallel:
Task T008: "Create DriverStandingsCache interface in adapter/port/DriverStandingsCache.kt"
Task T009: "Create standings DTOs in adapter/dto/StandingsResponses.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (shared models, client, Jolpica models)
2. Complete Phase 3: User Story 1 (driver standings end-to-end)
3. **STOP and VALIDATE**: Test driver standings independently
4. Deploy/demo if ready — driver standings available

### Incremental Delivery

1. Phase 2: Foundational → Shared infrastructure ready
2. Phase 3: US1 (Driver Standings) → Test independently → MVP!
3. Phase 4: US2 (Constructor Standings) → Test independently → Full standings feature
4. Phase 5: US3 (Historical) → Add test coverage → Complete feature
5. Phase 6: Polish → Quality gates pass → Ready for PR

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US4 (stale fallback) and US5 (auth) are cross-cutting concerns built into the use case and route patterns — tested within US1/US2 integration tests
- StandingsRoutes.kt and StandingsModule.kt are shared files — US2 extends what US1 creates
- All patterns follow existing proven implementations (GetDrivers, GetRaceResults, etc.)
- Commit after each task or logical group
