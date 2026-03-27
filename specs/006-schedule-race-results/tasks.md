# Tasks: Schedule, Race Results & Qualifying Endpoints

**Input**: Design documents from `/specs/006-schedule-race-results/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Included — constitution principle III (Test Coverage) requires tests for every feature.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Domain models, port interfaces, cache implementations, Jolpica models, DTOs, and JolpicaClient extensions that ALL user stories depend on.

- [x] T001 [P] Create domain models RaceWeekend, Sessions, RaceResult, FastestLap, QualifyingResult in src/main/kotlin/com/blaizmiko/f1backend/domain/model/RaceWeekend.kt, RaceResult.kt, QualifyingResult.kt
- [x] T002 [P] Add NotFoundException to src/main/kotlin/com/blaizmiko/f1backend/domain/model/Exceptions.kt and map to HTTP 404 in configureStatusPages() in src/main/kotlin/com/blaizmiko/f1backend/Routing.kt
- [x] T003 [P] Add validateRound() function to src/main/kotlin/com/blaizmiko/f1backend/adapter/route/RouteValidation.kt — positive integer 1-99
- [x] T004 [P] Add Jolpica response models for schedule, results, qualifying, and sprint to src/main/kotlin/com/blaizmiko/f1backend/infrastructure/external/JolpicaModels.kt — @Serializable data classes matching Jolpica JSON structure
- [x] T005 [P] Create data source port interfaces ScheduleDataSource in src/main/kotlin/com/blaizmiko/f1backend/domain/port/ScheduleDataSource.kt and RaceDataSource in src/main/kotlin/com/blaizmiko/f1backend/domain/port/RaceDataSource.kt
- [x] T006 [P] Create cache port interfaces ScheduleCache in src/main/kotlin/com/blaizmiko/f1backend/adapter/port/ScheduleCache.kt, NextRaceCache in src/main/kotlin/com/blaizmiko/f1backend/adapter/port/NextRaceCache.kt, and RaceResultCache in src/main/kotlin/com/blaizmiko/f1backend/adapter/port/RaceResultCache.kt
- [x] T007 [P] Create cache implementations InMemoryScheduleCache (ConcurrentHashMap, keyed by season) in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/cache/InMemoryScheduleCache.kt, InMemoryNextRaceCache (AtomicReference, single value) in InMemoryNextRaceCache.kt, and InMemoryRaceResultCache (ConcurrentHashMap, keyed by "type:season:round") in InMemoryRaceResultCache.kt
- [x] T008 [P] Create DTO models ScheduleResponses.kt (ScheduleResponse, NextRaceResponse, RaceWeekendDto, SessionsDto) in src/main/kotlin/com/blaizmiko/f1backend/adapter/dto/ScheduleResponses.kt and RaceResponses.kt (RaceResultsResponse, RaceResultDto, FastestLapDto, QualifyingResultsResponse, QualifyingResultDto) in src/main/kotlin/com/blaizmiko/f1backend/adapter/dto/RaceResponses.kt
- [x] T009 Add 5 fetch methods to JolpicaClient (fetchSchedule, fetchNextRace, fetchRaceResults, fetchQualifyingResults, fetchSprintResults) implementing ScheduleDataSource and RaceDataSource interfaces, with domain model mapping, in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/external/JolpicaClient.kt
- [x] T010 Add ScheduleDataSource and RaceDataSource interface bindings to src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/ClientModule.kt — same pattern as existing TeamDataSource/CircuitDataSource bindings

**Checkpoint**: All shared infrastructure is in place. User story implementation can begin.

---

## Phase 2: User Story 1 — View Season Schedule (Priority: P1) 🎯 MVP

**Goal**: Authenticated users can retrieve the full season schedule with all race weekends, session times, and circuit details for any season.

**Independent Test**: Request GET /api/v1/schedule and verify a complete list of race weekends is returned with correct fields.

### Implementation for User Story 1

- [x] T011 [US1] Implement GetSchedule use case in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetSchedule.kt — follows GetDrivers pattern: double-check locking, mutex per season, retry throttle, stale fallback. Cache TTL: 6 hours. Returns ScheduleResult(season, races, isStale).
- [x] T012 [US1] Implement schedule route handler (GET /schedule) in src/main/kotlin/com/blaizmiko/f1backend/adapter/route/ScheduleRoutes.kt — Koin lazy inject GetSchedule, validate optional ?season param, map domain→DTO, Warning header for stale
- [x] T013 [US1] Create ScheduleModule in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/ScheduleModule.kt — register InMemoryScheduleCache bound to ScheduleCache, register GetSchedule use case as single with cacheTtlHours from JolpicaConfig
- [x] T014 [US1] Register scheduleModule in Application.kt modules() call and add scheduleRoutes() to authenticated route block in src/main/kotlin/com/blaizmiko/f1backend/Routing.kt

### Tests for User Story 1

- [x] T015 [US1] Write integration tests for GET /schedule in src/test/kotlin/com/blaizmiko/f1backend/integration/ScheduleEndpointTest.kt — test: happy path (current season), past season param, 401 without token, cache hit (second call skips data source), stale fallback with Warning header, 502 on failure with no cache, 400 for invalid season

**Checkpoint**: Season schedule endpoint is fully functional and tested independently.

---

## Phase 3: User Story 2 — Check Next Race Countdown (Priority: P1)

**Goal**: Authenticated users can retrieve the next upcoming race with all session dates/times for countdown display.

**Independent Test**: Request GET /api/v1/schedule/next and verify a single race entry with session times is returned.

### Implementation for User Story 2

- [x] T016 [US2] Implement GetNextRace use case in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetNextRace.kt — follows GetCircuits pattern (single-value cache with AtomicReference-backed NextRaceCache). Cache TTL: 1 hour. Returns NextRaceResult(season, race?, isStale). race is null when season has ended.
- [x] T017 [US2] Add next-race route handler (GET /schedule/next) to src/main/kotlin/com/blaizmiko/f1backend/adapter/route/ScheduleRoutes.kt — Koin lazy inject GetNextRace, map domain→DTO, Warning header for stale, return race: null when no next race
- [x] T018 [US2] Update ScheduleModule in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/ScheduleModule.kt — add InMemoryNextRaceCache bound to NextRaceCache, register GetNextRace use case as single

### Tests for User Story 2

- [x] T019 [US2] Write integration tests for GET /schedule/next in src/test/kotlin/com/blaizmiko/f1backend/integration/NextRaceEndpointTest.kt — test: happy path, 401 without token, cache hit, stale fallback with Warning header, 502 on failure with no cache, null race when season ended

**Checkpoint**: Next race endpoint is fully functional and tested independently.

---

## Phase 4: User Story 3 — View Race Results (Priority: P1)

**Goal**: Authenticated users can retrieve the full finishing order for any Grand Prix by season and round, with positions, times, points, and fastest lap info.

**Independent Test**: Request GET /api/v1/races/2025/1/results and verify complete finishing order with all expected fields.

### Implementation for User Story 3

- [x] T020 [US3] Implement GetRaceResults use case in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetRaceResults.kt — double-check locking, mutex per "results:{season}:{round}" key, retry throttle. Cache TTL: 5 minutes if season == current year, Instant.MAX otherwise. Returns RaceResultsResult(season, round, raceName, results, isStale).
- [x] T021 [US3] Implement race results route handler (GET /races/{season}/{round}/results) in src/main/kotlin/com/blaizmiko/f1backend/adapter/route/RaceRoutes.kt — Koin lazy inject GetRaceResults, validate season + round path params, map domain→DTO, Warning header for stale
- [x] T022 [US3] Create RacesModule in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/RacesModule.kt — register InMemoryRaceResultCache bound to RaceResultCache, register GetRaceResults use case as single
- [x] T023 [US3] Register racesModule in Application.kt modules() call and add raceRoutes() to authenticated route block in src/main/kotlin/com/blaizmiko/f1backend/Routing.kt

### Tests for User Story 3

- [x] T024 [US3] Write integration tests for GET /races/{season}/{round}/results in src/test/kotlin/com/blaizmiko/f1backend/integration/RaceResultsEndpointTest.kt — test: happy path, 401 without token, cache hit, stale fallback with Warning header, 502 on failure with no cache, 400 for invalid season, 400 for invalid round, empty results for future race

**Checkpoint**: Race results endpoint is fully functional and tested independently.

---

## Phase 5: User Story 4 — View Qualifying Results (Priority: P2)

**Goal**: Authenticated users can retrieve qualifying results with Q1/Q2/Q3 times for any Grand Prix by season and round.

**Independent Test**: Request GET /api/v1/races/2025/1/qualifying and verify qualifying positions with Q1/Q2/Q3 times.

### Implementation for User Story 4

- [x] T025 [US4] Implement GetQualifyingResults use case in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetQualifyingResults.kt — double-check locking, mutex per "qualifying:{season}:{round}" key, retry throttle. Cache TTL: Instant.MAX always (qualifying results never change). Returns QualifyingResultsResult(season, round, raceName, qualifying, isStale).
- [x] T026 [US4] Add qualifying route handler (GET /races/{season}/{round}/qualifying) to src/main/kotlin/com/blaizmiko/f1backend/adapter/route/RaceRoutes.kt — Koin lazy inject GetQualifyingResults, validate season + round, map domain→DTO, Warning header for stale
- [x] T027 [US4] Update RacesModule in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/RacesModule.kt — register GetQualifyingResults use case as single (reuses existing RaceResultCache)

### Tests for User Story 4

- [x] T028 [US4] Write integration tests for GET /races/{season}/{round}/qualifying in src/test/kotlin/com/blaizmiko/f1backend/integration/QualifyingEndpointTest.kt — test: happy path with Q1/Q2/Q3 times, null Q2/Q3 for eliminated drivers, 401 without token, cache hit, stale fallback with Warning header, 502 on failure with no cache, 400 for invalid params

**Checkpoint**: Qualifying results endpoint is fully functional and tested independently.

---

## Phase 6: User Story 5 — View Sprint Results (Priority: P3)

**Goal**: Authenticated users can retrieve sprint race results for sprint weekends, with a clear 404 for non-sprint rounds.

**Independent Test**: Request sprint results for a sprint weekend (verify finishing order) and for a non-sprint round (verify 404).

### Implementation for User Story 5

- [x] T029 [US5] Implement GetSprintResults use case in src/main/kotlin/com/blaizmiko/f1backend/usecase/GetSprintResults.kt — double-check locking, mutex per "sprint:{season}:{round}" key, retry throttle. Cache TTL: Instant.MAX always. Throws NotFoundException when data source returns null/empty (non-sprint round). Returns SprintResultsResult(season, round, raceName, results, isStale).
- [x] T030 [US5] Add sprint route handler (GET /races/{season}/{round}/sprint) to src/main/kotlin/com/blaizmiko/f1backend/adapter/route/RaceRoutes.kt — Koin lazy inject GetSprintResults, validate season + round, map domain→DTO, Warning header for stale
- [x] T031 [US5] Update RacesModule in src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/RacesModule.kt — register GetSprintResults use case as single (reuses existing RaceResultCache)

### Tests for User Story 5

- [x] T032 [US5] Write integration tests for GET /races/{season}/{round}/sprint in src/test/kotlin/com/blaizmiko/f1backend/integration/SprintEndpointTest.kt — test: happy path (sprint weekend), 404 for non-sprint round, 401 without token, cache hit, stale fallback with Warning header, 502 on failure with no cache, 400 for invalid params

**Checkpoint**: Sprint results endpoint is fully functional and tested independently.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Verify all stories work together, static analysis passes, existing tests unbroken.

- [x] T033 Run ./gradlew ktlintCheck detekt to verify zero violations across all new files
- [x] T034 Run full test suite (./gradlew test) to verify all new tests pass AND all existing tests (auth, drivers, teams, circuits) remain green

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately. All [P] tasks within this phase can run in parallel.
- **User Stories (Phases 2–6)**: ALL depend on Phase 1 (Setup) completion.
  - US1 (Schedule), US3 (Race Results) can start in parallel after Phase 1.
  - US2 (Next Race) shares ScheduleRoutes.kt and ScheduleModule.kt with US1 — best done after US1.
  - US4 (Qualifying) and US5 (Sprint) share RaceRoutes.kt and RacesModule.kt with US3 — best done after US3.
- **Polish (Phase 7)**: Depends on all user stories being complete.

### User Story Dependencies

- **US1 (P1 — Schedule)**: After Phase 1 — no dependencies on other stories
- **US2 (P1 — Next Race)**: After Phase 1 — shares files with US1, best sequenced after US1
- **US3 (P1 — Race Results)**: After Phase 1 — no dependencies on other stories, can parallel with US1
- **US4 (P2 — Qualifying)**: After Phase 1 — shares files with US3, best sequenced after US3
- **US5 (P3 — Sprint)**: After Phase 1 — shares files with US3/US4, best sequenced after US4

### Recommended Execution Order

```
Phase 1 (T001–T010, all parallel) → US1 (T011–T015) → US2 (T016–T019) → US3 (T020–T024) → US4 (T025–T028) → US5 (T029–T032) → Polish (T033–T034)
```

### Within Each User Story

- Use case before route handler (route depends on use case)
- Koin module before or alongside route (both needed for integration)
- Registration in Application.kt/Routing.kt after module + route exist
- Tests after all implementation tasks for that story

### Parallel Opportunities

**Phase 1** (maximum parallelism):
```
T001, T002, T003, T004, T005, T006, T007, T008 — all different files, full parallel
T009 — after T004 + T005 (needs Jolpica models + port interfaces)
T010 — after T009 (needs client methods to exist)
```

**Cross-story parallelism** (if two developers):
```
Developer A: US1 → US2 (schedule domain)
Developer B: US3 → US4 → US5 (races domain)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (all shared infrastructure)
2. Complete Phase 2: User Story 1 — Season Schedule
3. **STOP and VALIDATE**: Run tests, verify GET /schedule works end-to-end
4. Incrementally add remaining stories

### Incremental Delivery

1. Phase 1 → Foundation ready
2. US1 (Schedule) → Test → First deliverable endpoint
3. US2 (Next Race) → Test → Home screen countdown support
4. US3 (Race Results) → Test → Post-race value delivered
5. US4 (Qualifying) → Test → Full qualifying weekend coverage
6. US5 (Sprint) → Test → Complete race weekend coverage
7. Polish → All quality gates pass

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable after Phase 1
- All use cases follow the established GetDrivers pattern (double-check locking, mutex, retry throttle, stale fallback)
- No new dependencies needed — all libraries already in libs.versions.toml
- Sprint 404 uses NotFoundException (new) mapped via StatusPages (existing pattern)
- RaceResultCache is shared across US3/US4/US5 with key prefix differentiation
