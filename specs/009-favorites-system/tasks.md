# Tasks: Favorites System

**Input**: Design documents from `/specs/009-favorites-system/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Integration tests are REQUIRED per the feature specification and project constitution (Principle III).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No project initialization needed -- project already exists and builds successfully.

(No tasks -- project structure and all dependencies are already in place.)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Create shared infrastructure (tables, repository, DTOs, DI module, wiring) that ALL user stories depend on.

- [X] T001 [P] Create FavoriteDriversTable Exposed DSL table in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/persistence/table/FavoriteDriversTable.kt` with columns: id (UUID PK), userId (UUID FK → UsersTable.id CASCADE), driverId (VARCHAR 64), createdAt (TIMESTAMP). Add uniqueIndex on (userId, driverId) and index on userId.
- [X] T002 [P] Create FavoriteTeamsTable Exposed DSL table in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/persistence/table/FavoriteTeamsTable.kt` with columns: id (UUID PK), userId (UUID FK → UsersTable.id CASCADE), teamId (VARCHAR 64), createdAt (TIMESTAMP). Add uniqueIndex on (userId, teamId) and index on userId.
- [X] T003 [P] Create FavoriteRepository domain interface in `src/main/kotlin/com/blaizmiko/f1backend/domain/repository/FavoriteRepository.kt` with methods: addFavoriteDriver(userId, driverId): Pair<Boolean, Instant> (created flag + addedAt timestamp), removeFavoriteDriver(userId, driverId), getFavoriteDriverIds(userId): List<Pair<String, Instant>>, isDriverFavorite(userId, driverId): Boolean, and analogous methods for teams.
- [X] T004 Create ExposedFavoriteRepository implementation in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/persistence/repository/ExposedFavoriteRepository.kt` implementing FavoriteRepository. Use insertIgnore with ON CONFLICT DO NOTHING for idempotent adds. Return Pair<Boolean, Instant> — if row inserted, return (true, newCreatedAt); if already existed, query and return (false, existingCreatedAt). Use dbQuery {} wrapper for all operations.
- [X] T005 [P] Create all favorites DTO classes in `src/main/kotlin/com/blaizmiko/f1backend/adapter/dto/FavoritesResponses.kt`: FavoriteDriversResponse, FavoriteDriverDto, FavoriteTeamsResponse, FavoriteTeamDto, TeamDriverDto, FavoriteStatusResponse, FavoriteDriverActionResponse(driverId: String, addedAt: String), FavoriteTeamActionResponse(teamId: String, addedAt: String), PersonalizedFeedResponse, FeedDriverDto, LastRaceDto, FeedTeamDto. All with @Serializable annotation. POST favorites returns the action response body on both 201 (newly created) and 200 (already existed), confirming what was added and when. The created-vs-existed distinction is conveyed via HTTP status code only.
- [X] T006 Register FavoriteDriversTable and FavoriteTeamsTable in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/persistence/DatabaseFactory.kt` by adding them to the SchemaUtils.create() call.
- [X] T007 Create skeleton FavoritesRoutes.kt in `src/main/kotlin/com/blaizmiko/f1backend/adapter/route/FavoritesRoutes.kt` as `fun Route.favoritesRoutes()` extension function with route("/favorites") block. Leave endpoint implementations for user story phases.
- [X] T008 Create FavoritesModule Koin module in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/FavoritesModule.kt` with `single { ExposedFavoriteRepository() } bind FavoriteRepository::class`. Use cases will be added in subsequent phases.
- [X] T009 Wire favoritesModule into Application.kt by importing and adding it to the Koin modules() list in `src/main/kotlin/com/blaizmiko/f1backend/Application.kt`.
- [X] T010 Wire favoritesRoutes() into Routing.kt by importing and adding `favoritesRoutes()` inside the authenticate block in `src/main/kotlin/com/blaizmiko/f1backend/Routing.kt`.

**Checkpoint**: Foundation ready. Tables created, repository wired, routes skeleton in place. `./gradlew build` should pass.

---

## Phase 3: User Story 1 + 2 - Add and Remove Favorite Drivers and Teams (Priority: P1)

**Goal**: Users can add and remove drivers and teams from their personal favorites list. Operations are idempotent. Returns 404 for non-existent entities.

**Independent Test**: POST a driver to favorites, verify 201. POST again, verify 200. DELETE, verify 204. POST non-existent driver, verify 404.

### Implementation

- [X] T011 [P] [US1] Create AddFavoriteDriver use case in `src/main/kotlin/com/blaizmiko/f1backend/usecase/AddFavoriteDriver.kt`. Constructor inject FavoriteRepository and DriverRepository. execute(userId, driverId): check driver exists via DriverRepository.findByDriverId() (throw NotFoundException if null), then call FavoriteRepository.addFavoriteDriver(). Return Pair<Boolean, Instant> (created flag + addedAt timestamp).
- [X] T012 [P] [US1] Create RemoveFavoriteDriver use case in `src/main/kotlin/com/blaizmiko/f1backend/usecase/RemoveFavoriteDriver.kt`. Constructor inject FavoriteRepository. execute(userId, driverId): call FavoriteRepository.removeFavoriteDriver().
- [X] T013 [P] [US2] Create AddFavoriteTeam use case in `src/main/kotlin/com/blaizmiko/f1backend/usecase/AddFavoriteTeam.kt`. Constructor inject FavoriteRepository and TeamRepository. execute(userId, teamId): check team exists via TeamRepository.findByTeamId() (throw NotFoundException if null), then call FavoriteRepository.addFavoriteTeam(). Return Pair<Boolean, Instant> (created flag + addedAt timestamp).
- [X] T014 [P] [US2] Create RemoveFavoriteTeam use case in `src/main/kotlin/com/blaizmiko/f1backend/usecase/RemoveFavoriteTeam.kt`. Constructor inject FavoriteRepository. execute(userId, teamId): call FavoriteRepository.removeFavoriteTeam().
- [X] T015 [US1] Add POST /favorites/drivers/{driverId} and DELETE /favorites/drivers/{driverId} route handlers in `src/main/kotlin/com/blaizmiko/f1backend/adapter/route/FavoritesRoutes.kt`. Extract userId from JWT principal via `UUID.fromString(call.principal<JWTPrincipal>()!!.subject)`. POST responds with FavoriteDriverActionResponse(driverId, addedAt) — 201 if newly created, 200 if already existed. DELETE responds 204.
- [X] T016 [US2] Add POST /favorites/teams/{teamId} and DELETE /favorites/teams/{teamId} route handlers in `src/main/kotlin/com/blaizmiko/f1backend/adapter/route/FavoritesRoutes.kt`. Same JWT extraction pattern. POST responds with FavoriteTeamActionResponse(teamId, addedAt) — 201 if newly created, 200 if already existed. DELETE responds 204.
- [X] T017 [US1] Register AddFavoriteDriver, RemoveFavoriteDriver as `single` in FavoritesModule in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/FavoritesModule.kt` with `single { AddFavoriteDriver(get(), get()) }` and `single { RemoveFavoriteDriver(get()) }`.
- [X] T018 [US2] Register AddFavoriteTeam, RemoveFavoriteTeam as `single` in FavoritesModule in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/FavoritesModule.kt` with `single { AddFavoriteTeam(get(), get()) }` and `single { RemoveFavoriteTeam(get()) }`.

**Checkpoint**: Add/remove favorites for both drivers and teams works. Idempotent behavior verified.

---

## Phase 4: User Story 3 - View All Favorite Drivers and Teams (Priority: P1)

**Goal**: Users can list all their favorited drivers and teams with full entity card details (not just IDs).

**Independent Test**: Add several favorites, then GET /favorites/drivers and /favorites/teams. Verify full driver cards (photo, team, code) and team cards (nationality, drivers) are returned. Verify empty list for user with no favorites.

### Implementation

- [X] T019 [P] [US3] Create GetFavoriteDrivers use case in `src/main/kotlin/com/blaizmiko/f1backend/usecase/GetFavoriteDrivers.kt`. Constructor inject FavoriteRepository and DriverRepository. execute(userId): get favorite driver IDs with timestamps from FavoriteRepository, then look up full DriverWithTeam details from DriverRepository.findByDriverId() for each. If findByDriverId() returns null for an orphaned favorite (driver removed from system), skip that entry silently rather than throwing. Return list of enriched favorites.
- [X] T020 [P] [US3] Create GetFavoriteTeams use case in `src/main/kotlin/com/blaizmiko/f1backend/usecase/GetFavoriteTeams.kt`. Constructor inject FavoriteRepository, TeamRepository, and DriverRepository. execute(userId): get favorite team IDs with timestamps, then look up full Team details and find drivers belonging to each team via DriverRepository.findAll() filtered by teamId. If a team lookup returns null for an orphaned favorite, skip that entry silently. Return enriched list.
- [X] T021 [US3] Add GET /favorites/drivers and GET /favorites/teams route handlers in `src/main/kotlin/com/blaizmiko/f1backend/adapter/route/FavoritesRoutes.kt`. Map use case results to FavoriteDriversResponse and FavoriteTeamsResponse DTOs. Format addedAt timestamps as ISO-8601 strings.
- [X] T022 [US3] Register GetFavoriteDrivers, GetFavoriteTeams as `single` in FavoritesModule in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/FavoritesModule.kt`.

**Checkpoint**: Full favorites listing works with enriched entity cards. Empty state returns empty arrays.

---

## Phase 5: User Story 4 - Check Favorite Status (Priority: P2)

**Goal**: Lightweight endpoint to check if a specific driver or team is in the user's favorites (for UI heart icon state).

**Independent Test**: Check status before adding (expect false), add favorite, check again (expect true), remove, check again (expect false).

### Implementation

- [X] T023 [P] [US4] Create CheckDriverFavorite use case in `src/main/kotlin/com/blaizmiko/f1backend/usecase/CheckDriverFavorite.kt`. Constructor inject FavoriteRepository. execute(userId, driverId): return FavoriteRepository.isDriverFavorite().
- [X] T024 [P] [US4] Create CheckTeamFavorite use case in `src/main/kotlin/com/blaizmiko/f1backend/usecase/CheckTeamFavorite.kt`. Constructor inject FavoriteRepository. execute(userId, teamId): return FavoriteRepository.isTeamFavorite().
- [X] T025 [US4] Add GET /favorites/drivers/check/{driverId} and GET /favorites/teams/check/{teamId} route handlers in `src/main/kotlin/com/blaizmiko/f1backend/adapter/route/FavoritesRoutes.kt`. Respond with FavoriteStatusResponse(isFavorite = result).
- [X] T026 [US4] Register CheckDriverFavorite, CheckTeamFavorite as `single` in FavoritesModule in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/FavoritesModule.kt`.

**Checkpoint**: Favorite status check works for both drivers and teams.

---

## Phase 6: User Story 5 - Personalized Home Screen Feed (Priority: P2)

**Goal**: Single aggregated endpoint returning championship standings and last race results for the user's favorite drivers and teams.

**Independent Test**: Add favorites, request GET /favorites/feed. Verify championship positions, points, and last race data are returned. Verify empty favorites returns empty arrays. Verify relevantNews is always [].

### Implementation

- [X] T027 [US5] Create GetPersonalizedFeed use case in `src/main/kotlin/com/blaizmiko/f1backend/usecase/GetPersonalizedFeed.kt`. Constructor inject FavoriteRepository, DriverRepository, GetDriverStandings, GetConstructorStandings, and GetRaceResults. Implement a per-user in-memory cache with ~30s TTL (ConcurrentHashMap<UUID, CachedFeed> where CachedFeed holds the response and an Instant expiry). execute(userId): check cache first; if miss or expired: (1) get favorite driver/team IDs from FavoriteRepository, (2) call GetDriverStandings.execute("current") and filter standings for favorite driverIds, (3) call GetConstructorStandings.execute("current") and filter for favorite teamIds, (4) call GetRaceResults for last round and filter for favorite driverIds, (5) combine into feed result with empty relevantNews array, (6) store in cache. Handle missing/stale data gracefully (null fields). Invalidate a user's cache entry when their favorites change (call from Add/Remove use cases or accept staleness within the 30s window).
- [X] T028 [US5] Add GET /favorites/feed route handler in `src/main/kotlin/com/blaizmiko/f1backend/adapter/route/FavoritesRoutes.kt`. Map use case result to PersonalizedFeedResponse DTO.
- [X] T029 [US5] Register GetPersonalizedFeed as `single` in FavoritesModule in `src/main/kotlin/com/blaizmiko/f1backend/infrastructure/di/FavoritesModule.kt`.

**Checkpoint**: Personalized feed returns aggregated data for favorites. Empty state handled gracefully.

---

## Phase 7: Integration Tests

**Purpose**: Comprehensive integration tests covering all endpoints, idempotent behavior, 404 handling, multi-user isolation (US6), and empty states.

- [X] T030 Create FavoritesEndpointTest integration test in `src/test/kotlin/com/blaizmiko/f1backend/integration/FavoritesEndpointTest.kt`. Use Kotest StringSpec with PostgreSQLContainer. Setup: create test users, seed sample drivers and teams. Configure test Koin module with real ExposedFavoriteRepository, ExposedDriverRepository, ExposedTeamRepository, and fake standings/race caches for feed testing. Test cases:
  - POST /favorites/drivers/{driverId} returns 201 on first add
  - POST /favorites/drivers/{driverId} returns 200 on duplicate add (idempotent)
  - DELETE /favorites/drivers/{driverId} returns 204
  - DELETE /favorites/drivers/{nonFavorited} returns 204 (idempotent)
  - POST /favorites/drivers/{nonExistent} returns 404
  - GET /favorites/drivers returns full driver cards with all fields
  - GET /favorites/drivers returns empty array for user with no favorites
  - GET /favorites/drivers/check/{driverId} returns true/false correctly
  - POST /favorites/teams/{teamId} returns 201/200/404 (same pattern)
  - DELETE /favorites/teams/{teamId} returns 204
  - GET /favorites/teams returns full team cards with driver rosters
  - GET /favorites/teams/check/{teamId} returns true/false correctly
  - GET /favorites/feed returns standings and last race data for favorites
  - GET /favorites/feed response includes `relevantNews` as an empty array `[]`
  - GET /favorites/feed returns empty arrays for user with no favorites
  - [US6] Multi-user isolation: User A's favorites not visible to User B
  - 401 without JWT token on all endpoints

**Checkpoint**: All integration tests pass. `./gradlew test` green.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and code quality checks.

- [X] T031 Run `./gradlew ktlintCheck` and fix any formatting violations across all new files.
- [X] T032 Run `./gradlew detekt` and fix any static analysis violations across all new files.
- [X] T033 Run `./gradlew test` and verify ALL tests pass (existing + new favorites tests). Ensure no regressions in existing endpoints.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: N/A -- project already exists
- **Phase 2 (Foundational)**: BLOCKS all user stories. Tables, repository, DTOs, DI, and wiring must be complete first.
- **Phase 3 (US1+US2)**: Depends on Phase 2. Add/remove is the core operation all other stories build on.
- **Phase 4 (US3)**: Depends on Phase 2. Can run in parallel with Phase 3 (independent endpoints using same repository).
- **Phase 5 (US4)**: Depends on Phase 2. Can run in parallel with Phase 3/4.
- **Phase 6 (US5)**: Depends on Phase 2. Can run in parallel with Phase 3/4/5 but benefits from Phase 3 being complete for end-to-end testing.
- **Phase 7 (Tests)**: Depends on Phases 3-6. All endpoints must be implemented before integration tests can fully pass.
- **Phase 8 (Polish)**: Depends on Phase 7.

### User Story Dependencies

- **US1 (Add/Remove Drivers)**: Depends on Foundational only. No other story dependencies.
- **US2 (Add/Remove Teams)**: Depends on Foundational only. Independent of US1.
- **US3 (View Favorites)**: Depends on Foundational only. Independent (can list empty).
- **US4 (Check Status)**: Depends on Foundational only. Independent.
- **US5 (Feed)**: Depends on Foundational only. Independent (returns empty feed for no favorites).
- **US6 (Multi-User Isolation)**: Tested in Phase 7 integration tests. No separate implementation.

### Within Each User Story

- Use cases before routes (routes inject use cases)
- DI registration before routes can inject
- All changes to shared files (FavoritesRoutes.kt, FavoritesModule.kt) are additive and non-conflicting

### Parallel Opportunities

- T001, T002, T003, T005 can all run in parallel (different files)
- T011, T012, T013, T014 can all run in parallel (different files)
- T019, T020 can run in parallel (different files)
- T023, T024 can run in parallel (different files)
- Phases 3, 4, 5 can technically run in parallel (independent stories using the same repository interface)

---

## Parallel Example: Phase 2 (Foundational)

```
# Launch in parallel (all different files):
T001: Create FavoriteDriversTable in .../table/FavoriteDriversTable.kt
T002: Create FavoriteTeamsTable in .../table/FavoriteTeamsTable.kt
T003: Create FavoriteRepository interface in .../repository/FavoriteRepository.kt
T005: Create DTOs in .../dto/FavoritesResponses.kt

# Then sequentially (depends on T001-T003):
T004: Create ExposedFavoriteRepository (depends on T001, T002, T003)
T006-T010: Wiring tasks (depend on tables + repository)
```

## Parallel Example: Phase 3 (US1+US2)

```
# Launch in parallel (all different files):
T011: AddFavoriteDriver use case
T012: RemoveFavoriteDriver use case
T013: AddFavoriteTeam use case
T014: RemoveFavoriteTeam use case

# Then sequentially (shared files):
T015: POST/DELETE driver routes in FavoritesRoutes.kt
T016: POST/DELETE team routes in FavoritesRoutes.kt
T017-T018: DI registration in FavoritesModule.kt
```

---

## Implementation Strategy

### MVP First (US1 + US2 Only)

1. Complete Phase 2: Foundational (tables, repository, DTOs, wiring)
2. Complete Phase 3: Add/Remove Drivers + Teams
3. **STOP and VALIDATE**: Test add, remove, idempotent, 404 behavior
4. Deploy/demo if ready

### Incremental Delivery

1. Phase 2 → Foundation ready
2. Phase 3 (US1+US2) → Add/remove works → MVP
3. Phase 4 (US3) → Favorites listing with full cards
4. Phase 5 (US4) → Status check for UI icons
5. Phase 6 (US5) → Personalized feed
6. Phase 7 → Integration tests validate everything
7. Phase 8 → Polish and code quality

### Recommended Single-Developer Flow

Execute phases sequentially: 2 → 3 → 4 → 5 → 6 → 7 → 8. Each phase builds on the previous. Total: 33 tasks.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US6 (Multi-User Isolation) has no separate implementation tasks -- it's a cross-cutting concern validated via integration tests in Phase 7
- US1 and US2 are combined in Phase 3 because they follow identical patterns and share the same repository
- All use cases registered as `single` scope per constitution (Principle VII)
- No new dependencies needed -- all tools already in build.gradle.kts
