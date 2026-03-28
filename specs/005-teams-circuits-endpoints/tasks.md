# Tasks: Teams & Circuits Data Endpoints

**Input**: Design documents from `/specs/005-teams-circuits-endpoints/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Integration tests are included as requested in the feature specification (FR-011, SC-008).

**Organization**: Tasks are grouped by user story. US3 (Caching) and US4 (Graceful Degradation) are cross-cutting concerns built into the use case pattern — their acceptance scenarios are verified as test cases within US1 and US2 integration tests.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: No new project setup needed — project already exists. This phase is empty.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared infrastructure changes that both endpoints depend on. MUST complete before any user story work begins.

- [x] T001 Add Jolpica constructor models (JolpicaConstructorResponse, ConstructorTable, JolpicaConstructor) to `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/external/JolpicaModels.kt`
- [x] T002 Add Jolpica circuit models (JolpicaCircuitResponse, CircuitTable, JolpicaCircuit, JolpicaLocation) to `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/external/JolpicaModels.kt`
- [x] T003 Extract `validateSeason()` from `src/main/kotlin/com/blaizmiko/f1backend/adapter/route/DriverRoutes.kt` into shared `src/main/kotlin/com/blaizmiko/f1backend/adapter/route/RouteValidation.kt` and update DriverRoutes to use it

**Checkpoint**: Shared models and utilities ready — user story implementation can begin

---

## Phase 3: User Story 1 - Browse Current Season Teams (Priority: P1) 🎯 MVP

**Goal**: Authenticated users can retrieve F1 constructors for any season via GET /api/v1/teams

**Independent Test**: Call GET /api/v1/teams with a valid JWT and verify the response contains a list of constructors with teamId, name, nationality, and the correct season label.

### Implementation for User Story 1

- [x] T004 [P] [US1] Create Team domain entity in `src/main/kotlin/com/blaizmiko/f1backend/domain/model/Team.kt`
- [x] T005 [P] [US1] Create TeamDataSource port interface in `src/main/kotlin/com/blaizmiko/f1backend/domain/port/TeamDataSource.kt`
- [x] T006 [P] [US1] Create TeamCache port interface in `src/main/kotlin/com/blaizmiko/f1backend/domain/port/TeamCache.kt`
- [x] T007 [US1] Add `fetchTeams(season: String): Pair<String, List<Team>>` method to JolpicaClient implementing TeamDataSource in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/external/JolpicaClient.kt`
- [x] T008 [US1] Create InMemoryTeamCache implementation in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/cache/InMemoryTeamCache.kt`
- [x] T009 [US1] Create GetTeams use case (mutex-guarded caching, stale fallback, retry throttle — same pattern as GetDrivers) in `src/main/kotlin/com/blaizmiko/f1backend/usecase/GetTeams.kt`
- [x] T010 [P] [US1] Create TeamsResponse and TeamDto DTOs in `src/main/kotlin/com/blaizmiko/f1backend/adapter/dto/TeamResponses.kt`
- [x] T011 [US1] Create teamRoutes() handler with season param validation and Warning header for stale responses in `src/main/kotlin/com/blaizmiko/f1backend/adapter/route/TeamRoutes.kt`
- [x] T012 [US1] Create TeamsModule Koin module in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/TeamsModule.kt`
- [x] T013 [US1] Add TeamDataSource binding to ClientModule in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/ClientModule.kt`
- [x] T014 [US1] Register teamsModule in Application.kt Koin install and add teamRoutes() inside authenticate block in Routing.kt
- [x] T015 [US1] Write integration tests (happy path, 401 without token, season param, cache hit, stale fallback with Warning header, 502 on no-cache failure, season validation) in `src/test/kotlin/com/blaizmiko/f1backend/integration/TeamsEndpointTest.kt`

**Checkpoint**: GET /api/v1/teams fully functional with caching, stale fallback, and auth. Covers US1, US3 (teams caching), and US4 (teams degradation) acceptance scenarios.

---

## Phase 4: User Story 2 - Browse All F1 Circuits (Priority: P1)

**Goal**: Authenticated users can retrieve the full F1 circuit catalog via GET /api/v1/circuits

**Independent Test**: Call GET /api/v1/circuits with a valid JWT and verify the response contains all circuits with circuitId, name, locality, country, lat, lng, and url.

### Implementation for User Story 2

- [x] T016 [P] [US2] Create Circuit domain entity in `src/main/kotlin/com/blaizmiko/f1backend/domain/model/Circuit.kt`
- [x] T017 [P] [US2] Create CircuitDataSource port interface in `src/main/kotlin/com/blaizmiko/f1backend/domain/port/CircuitDataSource.kt`
- [x] T018 [P] [US2] Create CircuitCache port interface in `src/main/kotlin/com/blaizmiko/f1backend/domain/port/CircuitCache.kt`
- [x] T019 [US2] Add `fetchCircuits(): List<Circuit>` method to JolpicaClient implementing CircuitDataSource in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/external/JolpicaClient.kt`
- [x] T020 [US2] Create InMemoryCircuitCache implementation in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/cache/InMemoryCircuitCache.kt`
- [x] T021 [US2] Create GetCircuits use case (single "all" cache key, Instant.MAX expiry for indefinite cache, mutex-guarded, stale fallback) in `src/main/kotlin/com/blaizmiko/f1backend/usecase/GetCircuits.kt`
- [x] T022 [P] [US2] Create CircuitsResponse and CircuitDto DTOs in `src/main/kotlin/com/blaizmiko/f1backend/adapter/dto/CircuitResponses.kt`
- [x] T023 [US2] Create circuitRoutes() handler with Warning header for stale responses (no season param) in `src/main/kotlin/com/blaizmiko/f1backend/adapter/route/CircuitRoutes.kt`
- [x] T024 [US2] Create CircuitsModule Koin module in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/CircuitsModule.kt`
- [x] T025 [US2] Add CircuitDataSource binding to ClientModule in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/ClientModule.kt`
- [x] T026 [US2] Register circuitsModule in Application.kt Koin install and add circuitRoutes() inside authenticate block in Routing.kt
- [x] T027 [US2] Write integration tests (happy path, 401 without token, cache hit, stale fallback with Warning header, 502 on no-cache failure) in `src/test/kotlin/com/blaizmiko/f1backend/integration/CircuitsEndpointTest.kt`

**Checkpoint**: GET /api/v1/circuits fully functional with indefinite caching, stale fallback, and auth. Covers US2, US3 (circuits caching), and US4 (circuits degradation) acceptance scenarios.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Verify everything works together and all quality gates pass

- [x] T028 Run existing tests to confirm no regressions (`./gradlew test`)
- [x] T029 Run ktlintCheck and fix any violations (`./gradlew ktlintCheck`)
- [x] T030 Run detekt and fix any violations (`./gradlew detekt`)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Foundational (Phase 2)**: No dependencies — can start immediately. BLOCKS all user stories.
- **US1 Teams (Phase 3)**: Depends on Phase 2 completion
- **US2 Circuits (Phase 4)**: Depends on Phase 2 completion. Independent of US1 — can run in parallel.
- **Polish (Phase 5)**: Depends on Phase 3 and Phase 4 completion

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Phase 2. No dependencies on US2.
- **User Story 2 (P1)**: Can start after Phase 2. No dependencies on US1.
- **User Story 3 (P2)**: Caching behavior is built into US1 (T009) and US2 (T021). Verified by test cases in T015 and T027.
- **User Story 4 (P2)**: Stale fallback is built into US1 (T009, T011) and US2 (T021, T023). Verified by test cases in T015 and T027.

### Within Each User Story

- Domain model + ports (T004-T006, T016-T018) can be created in parallel
- JolpicaClient method depends on Jolpica models from Phase 2
- Cache implementation depends on port interface
- Use case depends on port interfaces being defined
- Route depends on use case and DTOs
- DI module depends on cache, use case, and data source
- Wiring (Application.kt, Routing.kt) depends on DI module and route
- Integration tests depend on everything above

### Parallel Opportunities

- T004, T005, T006 can run in parallel (domain model + ports for teams)
- T010 can run in parallel with T007-T009 (DTOs have no dependency on use case)
- T016, T017, T018 can run in parallel (domain model + ports for circuits)
- T022 can run in parallel with T019-T021 (DTOs have no dependency on use case)
- US1 and US2 can run entirely in parallel after Phase 2 (they share ClientModule edits at T013/T025 and Application.kt/Routing.kt wiring at T014/T026, so coordinate those)

---

## Parallel Example: User Story 1

```text
# After Phase 2 completes, launch domain model + ports in parallel:
T004: "Create Team domain entity in domain/model/Team.kt"
T005: "Create TeamDataSource port in domain/port/TeamDataSource.kt"
T006: "Create TeamCache port in domain/port/TeamCache.kt"
T010: "Create TeamsResponse and TeamDto DTOs in adapter/dto/TeamResponses.kt"

# Then sequentially: JolpicaClient → Cache → UseCase → Route → DI → Wiring → Tests
```

## Parallel Example: User Story 2

```text
# Can start in parallel with US1 after Phase 2:
T016: "Create Circuit domain entity in domain/model/Circuit.kt"
T017: "Create CircuitDataSource port in domain/port/CircuitDataSource.kt"
T018: "Create CircuitCache port in domain/port/CircuitCache.kt"
T022: "Create CircuitsResponse and CircuitDto DTOs in adapter/dto/CircuitResponses.kt"

# Then sequentially: JolpicaClient → Cache → UseCase → Route → DI → Wiring → Tests
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (shared models + validation)
2. Complete Phase 3: User Story 1 — Teams endpoint
3. **STOP and VALIDATE**: Run `./gradlew test` — teams endpoint works independently
4. Deploy/demo if ready

### Incremental Delivery

1. Complete Phase 2 → Shared infrastructure ready
2. Add US1 (Teams) → Test independently → Deploy (MVP!)
3. Add US2 (Circuits) → Test independently → Deploy
4. Phase 5: Polish → All quality gates pass → Feature complete

### Full Parallel Strategy

1. Complete Phase 2 together
2. Once Phase 2 done, work US1 and US2 in parallel
3. Coordinate on shared file edits (ClientModule, Application.kt, Routing.kt)
4. Phase 5: Final validation

---

## Notes

- US3 and US4 are cross-cutting concerns implemented within the use case pattern — no separate implementation tasks needed, only test verification within US1/US2 integration tests
- Circuits use `Instant.MAX` for cache expiry — no changes to `CacheEntry` class needed
- JolpicaClient implements `DriverDataSource`, `TeamDataSource`, and `CircuitDataSource` on the same class
- The `validateSeason()` extraction (T003) updates an existing file — verify drivers tests still pass after
- Total: 30 tasks across 4 phases
