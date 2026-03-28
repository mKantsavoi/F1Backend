# Tasks: Driver Persistence & Detail Endpoint

**Input**: Design documents from `/specs/008-drivers-db-detail/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/drivers-api.md, research.md, quickstart.md

**Tests**: Included — constitution (Principle III) requires tests with every feature, and SC-008 explicitly requires integration tests.

**Organization**: Tasks grouped by user story. US2 (Seed) before US1 (List) because data must exist before the list endpoint can serve it.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Exact file paths included in descriptions

---

## Phase 1: Setup (Bundled Resource Files)

**Purpose**: Create the static resource files that the seed process reads from.

- [x] T001 [P] Create photo URL mapping file at `src/main/resources/seed/driver-photos.json` with all 22 current drivers mapped to formula1.com CDN parameters (team slug + driver code). **IMPORTANT**: The keys must use Jolpica driverId format (surname-only for most drivers, e.g., `"hamilton"` not `"lewis_hamilton"`; `"max_verstappen"` only when disambiguation is needed). Verify actual Jolpica IDs match the keys in `driver-biographies.json`.
- [x] T002 [P] Place the user-provided driver biographies file at `src/main/resources/seed/driver-biographies.json` containing the `{ "drivers": [{ "driverId": "...", "teamId": "...", "biography": "..." }] }` structure for all 22 drivers

---

## Phase 2: Foundational (Database Tables, Domain Interfaces, Repositories)

**Purpose**: Core persistence infrastructure that ALL user stories depend on. Must complete before any story work begins.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T003 [P] Add new column length constants to `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/persistence/table/ColumnLengths.kt` — TEAM_ID(64), TEAM_NAME(200), DRIVER_ID(64), DRIVER_CODE(3), FIRST_NAME(100), LAST_NAME(100), NATIONALITY(100), PHOTO_URL(500)
- [x] T004 [P] Create `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/persistence/table/TeamsTable.kt` — Exposed DSL table with id(UUID PK), teamId(VARCHAR UNIQUE), name(VARCHAR), nationality(VARCHAR), createdAt, updatedAt per data-model.md
- [x] T005 [P] Create `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/persistence/table/DriversTable.kt` — Exposed DSL table with id(UUID PK), driverId(VARCHAR UNIQUE), number(INTEGER nullable), code(VARCHAR), firstName, lastName, nationality, dateOfBirth(DATE nullable), photoUrl(VARCHAR nullable), teamId(VARCHAR nullable referencing TeamsTable.teamId), biography(TEXT nullable), createdAt, updatedAt per data-model.md
- [x] T006 Register TeamsTable and DriversTable in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/persistence/DatabaseFactory.kt` — add both to the `SchemaUtils.create()` call (TeamsTable before DriversTable due to FK reference)
- [x] T007 [P] Create `src/main/kotlin/com/blaizmiko/f1backend/domain/repository/TeamRepository.kt` — interface with methods: `findAll(): List<Team>`, `findByTeamId(teamId: String): Team?`, `insertAll(teams: List<Team>)`, `count(): Long`
- [x] T008 [P] Create `src/main/kotlin/com/blaizmiko/f1backend/domain/repository/DriverRepository.kt` — interface with methods: `findAll(): List<Driver>`, `findByDriverId(driverId: String): DriverWithTeam?`, `insertAll(drivers: List<Driver>)`, `count(): Long`. Define `DriverWithTeam` data class (driver fields + nullable team fields) in domain model
- [x] T009 [P] Create `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/persistence/repository/ExposedTeamRepository.kt` — implements TeamRepository using Exposed DSL queries wrapped in `DatabaseFactory.dbQuery()`
- [x] T010 [P] Create `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/persistence/repository/ExposedDriverRepository.kt` — implements DriverRepository using Exposed DSL. `findAll()` selects from DriversTable only. `findByDriverId()` uses `DriversTable leftJoin TeamsTable` to resolve team name. All queries wrapped in `DatabaseFactory.dbQuery()`
- [x] T011 [P] Add `photoUrl: String?` and `teamId: String?` and `biography: String?` fields to Driver domain model in `src/main/kotlin/com/blaizmiko/f1backend/domain/model/Driver.kt` (with default null values to avoid breaking existing code)
- [x] T012 [P] Update `src/main/kotlin/com/blaizmiko/f1backend/adapter/dto/DriverResponses.kt` — add `photoUrl: String? = null` to `DriverDto`, create new `DriverDetailResponse` data class with driverId, number, code, firstName, lastName, nationality, dateOfBirth, photoUrl, team(nullable `DriverTeamDto` with teamId+name), biography per contract

**Checkpoint**: Foundation ready — tables created, repositories implemented, domain models updated. User story implementation can begin.

---

## Phase 3: User Story 2 — Database Seeding on First Startup (Priority: P1)

**Goal**: On first startup with empty DB, seed teams and drivers from Jolpica + bundled files.

**Independent Test**: Start app with empty DB, verify all 22 drivers populated with base data, photos, and biographies.

### Implementation for User Story 2

- [x] T013 [US2] Create `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/seed/DriverSeedService.kt` — orchestrates the full seed flow: (1) check if drivers table is empty via `DriverRepository.count()`, (2) if empty: fetch teams via `TeamDataSource.fetchTeams("current")` and insert via `TeamRepository.insertAll()`, (3) fetch drivers+constructor assignments via Jolpica driver standings endpoint to get canonical teamId per driver, (4) load photo mapping from classpath `seed/driver-photos.json` and build photo URLs using the CDN template, (5) load biographies from classpath `seed/driver-biographies.json` and map by driverId, (6) merge enrichment data into driver records, (7) insert via `DriverRepository.insertAll()`, (8) log enrichment results (how many photos/bios applied). Handle missing/malformed JSON files gracefully (log warning, continue with nulls). Jolpica failure is fatal — throw and let startup fail.
- [x] T014 [US2] Update `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/DriversModule.kt` — remove `InMemoryDriverCache` binding, add `ExposedDriverRepository` bound to `DriverRepository`, add `DriverSeedService` as singleton, add `GetDrivers` with repository dependency (replacing cache+dataSource), add `GetDriverDetail`
- [x] T015 [US2] Update `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/TeamsModule.kt` — remove `InMemoryTeamCache` binding, add `ExposedTeamRepository` bound to `TeamRepository`, update `GetTeams` with repository dependency (replacing cache+dataSource)
- [x] T016 [US2] Update `src/main/kotlin/com/blaizmiko/f1backend/Application.kt` — after `DatabaseFactory.init(appConfig.database)`, retrieve `DriverSeedService` from Koin and call `seedIfEmpty()` in a coroutine scope. This must run before routes are configured.

**Checkpoint**: Seed populates teams + drivers on first startup. Database has data for subsequent phases.

---

## Phase 4: User Story 1 — Driver List from Database (Priority: P1) MVP

**Goal**: Driver list endpoint reads from PostgreSQL instead of Jolpica cache. Adds photoUrl field. Season param ignored.

**Independent Test**: Call GET /api/v1/drivers with JWT, verify response includes all drivers with photoUrl, no external API call at runtime.

### Implementation for User Story 1

- [x] T017 [US1] Rewrite `src/main/kotlin/com/blaizmiko/f1backend/usecase/GetDrivers.kt` — remove cache/dataSource/mutex/throttle logic, inject `DriverRepository`, return all drivers from `repository.findAll()`. Remove `DriversResult` (no more isStale concept). Return a simple list of drivers. Season is hardcoded to "current".
- [x] T018 [US1] Rewrite `src/main/kotlin/com/blaizmiko/f1backend/usecase/GetTeams.kt` — remove cache/dataSource/mutex/throttle logic, inject `TeamRepository`, return all teams from `repository.findAll()`. Remove `TeamsResult`. Season hardcoded to "current".
- [x] T019 [US1] Update `src/main/kotlin/com/blaizmiko/f1backend/adapter/route/DriverRoutes.kt` — update list handler to use simplified `GetDrivers`, map Driver domain model to `DriverDto` including new `photoUrl` field, remove stale-cache Warning header logic, ignore season query parameter (always return current season)
- [x] T020 [US1] Update `src/main/kotlin/com/blaizmiko/f1backend/adapter/route/TeamRoutes.kt` — update handler to use simplified `GetTeams`, remove stale-cache Warning header logic, ignore season query parameter

### Tests for User Story 1

- [x] T021 [US1] Rewrite `src/test/kotlin/com/blaizmiko/f1backend/integration/DriversEndpointTest.kt` — replace FakeDataSource+InMemoryCache test setup with testcontainers PostgreSQL. Pre-insert test drivers into DB. Assert: 200 with correct JSON including photoUrl, 401 without token. Remove cache-hit/stale-cache/season-validation tests (no longer applicable).
- [x] T022 [US1] Rewrite `src/test/kotlin/com/blaizmiko/f1backend/integration/TeamsEndpointTest.kt` — replace FakeDataSource+InMemoryCache with testcontainers PostgreSQL. Pre-insert test teams. Assert: 200 with correct JSON, 401 without token.

**Checkpoint**: Driver and team list endpoints fully functional from database. MVP complete.

---

## Phase 5: User Story 3 — Driver Detail Card (Priority: P2)

**Goal**: New GET /api/v1/drivers/{driverId} endpoint returns full driver card with team (via JOIN) and biography.

**Independent Test**: Call GET /api/v1/drivers/max_verstappen with JWT, verify full card response. Call with unknown ID, verify 404.

### Implementation for User Story 3

- [x] T023 [US3] Create `src/main/kotlin/com/blaizmiko/f1backend/usecase/GetDriverDetail.kt` — inject `DriverRepository`, call `findByDriverId(driverId)`, return `DriverWithTeam` or throw `NotFoundException` if not found
- [x] T024 [US3] Update `src/main/kotlin/com/blaizmiko/f1backend/adapter/route/DriverRoutes.kt` — add `get("/{driverId}")` route inside the existing drivers route block. Extract path param, call `GetDriverDetail`, map `DriverWithTeam` to `DriverDetailResponse`. Handle `NotFoundException` → 404 (should already be handled by StatusPages).

### Tests for User Story 3

- [x] T025 [US3] Create `src/test/kotlin/com/blaizmiko/f1backend/integration/DriverDetailEndpointTest.kt` — testcontainers PostgreSQL. Pre-insert driver + team. Test cases: (1) 200 happy path with full card including team name and biography, (2) 200 with null biography, (3) 200 with null photoUrl, (4) 404 for unknown driverId, (5) 401 without token.

**Checkpoint**: Full driver card endpoint working. All read endpoints serve from database.

---

## Phase 6: User Story 4 — Remove In-Memory Caches (Priority: P3)

**Goal**: Delete driver and team cache interfaces and implementations. Clean architecture — no dead code.

**Independent Test**: Verify all tests pass after deletion. Confirm no imports reference deleted files.

### Implementation for User Story 4

- [x] T026 [P] [US4] Delete `src/main/kotlin/com/blaizmiko/f1backend/adapter/port/DriverCache.kt`
- [x] T027 [P] [US4] Delete `src/main/kotlin/com/blaizmiko/f1backend/adapter/port/TeamCache.kt`
- [x] T028 [P] [US4] Delete `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/cache/InMemoryDriverCache.kt`
- [x] T029 [P] [US4] Delete `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/cache/InMemoryTeamCache.kt`
- [x] T030 [US4] Search codebase for any remaining imports or references to deleted DriverCache, TeamCache, InMemoryDriverCache, InMemoryTeamCache and remove them. Verify `CacheEntry` and `SeasonCache` models are still used by other features (circuits, schedule, races, standings) — do NOT delete those.

**Checkpoint**: All cache code for drivers and teams removed. Codebase clean.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final validation across all stories.

- [x] T031 Run `./gradlew ktlintCheck detekt` — fix any formatting or static analysis violations across all new and modified files
- [x] T032 Run `./gradlew test` — verify all existing and new integration tests pass
- [x] T033 Validate quickstart smoke test: start app with empty DB, verify seed logs, test both endpoints manually per `specs/008-drivers-db-detail/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (resource files must exist for seed tests)
- **US2 Seed (Phase 3)**: Depends on Phase 2 completion — BLOCKS all endpoint stories
- **US1 List (Phase 4)**: Depends on Phase 3 (seed must populate data)
- **US3 Detail (Phase 5)**: Depends on Phase 2 (needs repos) + Phase 3 (needs data). Can run in parallel with Phase 4 if data is seeded.
- **US4 Cleanup (Phase 6)**: Depends on Phases 4+5 (all endpoints must work before removing old code)
- **Polish (Phase 7)**: Depends on all previous phases

### User Story Dependencies

- **US2 (P1 - Seed)**: Depends on Foundational only. Must complete before US1/US3 can be tested.
- **US1 (P1 - List)**: Depends on US2 (needs data in DB). Core MVP.
- **US3 (P2 - Detail)**: Depends on Foundational + US2 (data). Can parallelize with US1 implementation (different files).
- **US4 (P3 - Cleanup)**: Depends on US1 + US3 both working. Safe to do last.

### Within Each User Story

- Implementation tasks before test tasks (tests need running endpoints)
- Models/repos before use cases before routes
- DI updates before Application.kt changes

### Parallel Opportunities

Within Phase 2 (Foundational):
- T003, T004, T005 can all run in parallel (different files)
- T007, T008 can run in parallel (different files)
- T009, T010 can run in parallel (different files)
- T011, T012 can run in parallel (different files)

Within Phase 4 (US1):
- T017, T018 can run in parallel (different use case files)
- T019, T020 can run in parallel (different route files)
- T021, T022 can run in parallel (different test files)

Within Phase 5 (US3):
- T023, T024 are sequential (route depends on use case)

Within Phase 6 (US4):
- T026, T027, T028, T029 can all run in parallel (independent deletions)

---

## Parallel Example: Phase 2 Foundational

```
# Batch 1 — Tables + column lengths (all different files):
T003: Add column lengths to ColumnLengths.kt
T004: Create TeamsTable.kt
T005: Create DriversTable.kt

# Batch 2 — Domain interfaces (different files):
T007: Create TeamRepository interface
T008: Create DriverRepository interface

# Batch 3 — Implementations + models (different files):
T009: Create ExposedTeamRepository
T010: Create ExposedDriverRepository
T011: Update Driver domain model
T012: Update DriverResponses.kt
```

---

## Implementation Strategy

### MVP First (US2 + US1)

1. Complete Phase 1: Setup (resource files)
2. Complete Phase 2: Foundational (tables, repos)
3. Complete Phase 3: US2 — Seed (populate database)
4. Complete Phase 4: US1 — Driver list from DB
5. **STOP and VALIDATE**: Both endpoints return data from database
6. Deploy/demo if ready — this is the MVP

### Incremental Delivery

1. Setup + Foundational → Infrastructure ready
2. Add US2 (Seed) → Database populated on startup
3. Add US1 (List) → Driver/team list from DB (MVP!)
4. Add US3 (Detail) → Rich driver cards
5. Add US4 (Cleanup) → Remove dead cache code
6. Polish → All checks pass, smoke test validated

---

## Notes

- [P] tasks = different files, no dependencies between them
- [Story] label maps task to specific user story for traceability
- Resource files (T001, T002) should be created first — seed service reads from them
- The user must provide the `driver-biographies.json` file content (T002)
- `CacheEntry` and `SeasonCache` domain models must NOT be deleted — still used by circuits, schedule, races, standings features
- Existing `DriverDataSource` and `TeamDataSource` port interfaces are kept — seed service still uses them for Jolpica calls during first startup
