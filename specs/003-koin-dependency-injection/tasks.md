# Tasks: Koin Dependency Injection

**Input**: Design documents from `/specs/003-koin-dependency-injection/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: No new test tasks — spec requires existing tests to pass with unchanged assertions. Test setup modifications are included as implementation tasks.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/main/kotlin/` and `src/test/kotlin/` at repository root
- Koin modules: `src/main/kotlin/com/blaizmiko/infrastructure/di/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Add Koin dependencies to the project build

- [x] T001 Add Koin BOM 4.2.0 version entry and library entries (koin-bom, koin-ktor, koin-logger-slf4j, koin-test, koin-test-junit5) to `gradle/libs.versions.toml`
- [x] T002 Add Koin implementation and test dependencies to `build.gradle.kts` using version catalog references (platform BOM, koin-ktor, koin-logger-slf4j for implementation; koin-test, koin-test-junit5 for testImplementation)

---

## Phase 2: Foundational — Koin Module Declarations

**Purpose**: Create all four Koin module files that declare the complete dependency graph. These MUST be complete before Application.kt and routes can be migrated.

**⚠️ CRITICAL**: No route or application migration can begin until all modules are defined.

- [x] T003 [P] Create `src/main/kotlin/com/blaizmiko/infrastructure/di/CoreModule.kt` — define `coreModule(appConfig: AppConfig)` function returning a Koin module with: `single { appConfig }`, `single { appConfig.jwt }`, `single { appConfig.jolpica }`, `single { appConfig.database }`, `single { JwtProvider(get()) }`. See research.md R6 for pattern.
- [x] T004 [P] Create `src/main/kotlin/com/blaizmiko/infrastructure/di/ClientModule.kt` — define `val clientModule = module { single { JolpicaClient(get()) } bind DriverDataSource::class }`. See research.md R4 for scoping.
- [x] T005 [P] Create `src/main/kotlin/com/blaizmiko/infrastructure/di/AuthModule.kt` — define `val authModule = module { ... }` with: `single { ExposedUserRepository() } bind UserRepository::class`, `single { ExposedRefreshTokenRepository() } bind RefreshTokenRepository::class`, and `factory {}` declarations for all 7 auth use cases (RegisterUser, LoginUser, RefreshTokens, GetProfile, UpdateProfile, ChangePassword, LogoutUser) using `get()` for their constructor dependencies. See data-model.md Factories table for exact dependencies.
- [x] T006 [P] Create `src/main/kotlin/com/blaizmiko/infrastructure/di/DriversModule.kt` — define `val driversModule = module { ... }` with: `single { InMemoryDriverCache() } bind DriverCache::class`, `single { GetDrivers(get(), get(), get<JolpicaConfig>().cacheTtlHours) }`. GetDrivers MUST be singleton (holds mutex state). See research.md R4.

**Checkpoint**: All Koin modules defined. Compile check should pass (modules are standalone declarations).

---

## Phase 3: User Story 1 + User Story 2 — Application & Route Migration (Priority: P1) 🎯 MVP

**Goal**: Install Koin as the Ktor plugin, replace all manual dependency wiring with Koin injection in Application.kt, Routing.kt, and route handlers. After this phase, zero manual instantiation remains and all endpoints work identically.

**Independent Test**: Run `./gradlew test` — all 13 existing test scenarios pass. Manually verify auth and driver endpoints respond correctly.

### Implementation

- [x] T007 [US1] [US2] Refactor `src/main/kotlin/Application.kt` — install Koin plugin with `slf4jLogger()` and all four modules `(coreModule(appConfig), clientModule, authModule, driversModule)`. Keep `loadAppConfig()` call before Koin install. After Koin install: call `DatabaseFactory.init()` with config from Koin (`get<DatabaseConfig>()`), install Authentication using `inject<JwtProvider>()`. See research.md R6, R7, R9 for exact patterns. Remove direct `JwtProvider` construction.
- [x] T008 [US1] [US2] Refactor `src/main/kotlin/Routing.kt` — remove `appConfig` and `jwtProvider` parameters from `configureRouting()`. Remove all manual dependency creation (repositories, JolpicaClient, InMemoryDriverCache, GetDrivers). Replace with Koin `inject()` calls where needed. Move `ApplicationStopped` subscription for `JolpicaClient.close()` to use `get<JolpicaClient>()` from Koin. Keep `configureSerialization()` and `configureStatusPages()` unchanged. Update `authRoutes()` and `driverRoutes()` calls to remove parameter passing.
- [x] T009 [P] [US1] [US2] Refactor `src/main/kotlin/com/blaizmiko/adapter/route/AuthRoutes.kt` — remove all function parameters (`userRepository`, `refreshTokenRepository`, `jwtProvider`, `appConfig`). Use `val registerUser by inject<RegisterUser>()` pattern (and same for all 7 use cases) via Koin's `Route.inject()` extension. Route handler bodies remain completely unchanged. See research.md R3 for injection pattern.
- [x] T010 [P] [US1] [US2] Refactor `src/main/kotlin/com/blaizmiko/adapter/route/DriverRoutes.kt` — remove `getDrivers` function parameter. Use `val getDrivers by inject<GetDrivers>()` via Koin's `Route.inject()` extension. Route handler body remains completely unchanged.

**Checkpoint**: Application compiles and starts. All endpoints respond correctly. Zero manual instantiation in Application.kt and Routing.kt. This validates US1 (behavior preserved) and US2 (all DI-managed).

---

## Phase 4: User Story 3 — Module Organization Verification (Priority: P2)

**Goal**: Verify that the four Koin modules are organized logically by domain and that adding a new feature requires only a new module file plus one line in the module list.

**Independent Test**: Review module files — each is self-contained by domain area. Confirm Application.kt module list is the only integration point.

### Implementation

- [x] T011 [US3] Review and verify module organization in `src/main/kotlin/com/blaizmiko/infrastructure/di/` — confirm core (config + security), client (HTTP), auth (repos + use cases), drivers (cache + GetDrivers) separation. Verify that Application.kt's `install(Koin)` block references all modules in a flat list. No cross-module dependencies leak implementation details. Document the module pattern with a brief comment at the top of each module file explaining what it contains and when a developer should add to it.

**Checkpoint**: Module structure is clean and documented. Adding a future feature (e.g., teams) means creating a `TeamsModule.kt` and adding it to the module list.

---

## Phase 5: User Story 4 — Test Adaptation (Priority: P2)

**Goal**: All 13 existing test scenarios pass with Koin. Test setup uses Koin where the test's `testModule()` previously did manual wiring. All test assertions remain unchanged.

**Independent Test**: Run `./gradlew test` — 13/13 pass, zero assertion modifications.

### Implementation

- [x] T012 [P] [US4] Update `src/test/kotlin/com/blaizmiko/integration/AuthLifecycleTest.kt` — modify `testModule()` to install Koin with the production modules (coreModule, authModule) instead of manual dependency creation. The test already uses a real PostgreSQL via TestContainers, so the test config loading and DatabaseFactory.init remain similar. Ensure Koin is properly started/stopped per test (Ktor test host handles this via plugin lifecycle). All 6 test assertions MUST remain unchanged.
- [x] T013 [P] [US4] Update `src/test/kotlin/com/blaizmiko/integration/DriversEndpointTest.kt` — modify `testModule()` to install Koin with production modules but override `DriverDataSource` binding with `FakeDataSource` using Koin's `allowOverride(true)` or a test-specific module that replaces the clientModule. The fake data source setup and all 7 test assertions MUST remain unchanged.

**Checkpoint**: Full test suite passes green. Test bodies untouched.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final verification and cleanup

- [x] T014 Run full test suite (`./gradlew test`) and verify all 13 scenarios pass with zero failures
- [x] T015 Verify zero manual component instantiation in `src/main/kotlin/Application.kt` and `src/main/kotlin/Routing.kt` — grep for direct constructor calls to repositories, clients, use cases, JwtProvider
- [x] T016 Run application with Docker Compose (`docker-compose up`) and smoke-test all 8 endpoints to verify identical behavior
- [x] T017 Run `./gradlew ktlintCheck detekt` and fix any violations in new or modified files (CoreModule.kt, ClientModule.kt, AuthModule.kt, DriversModule.kt, Application.kt, Routing.kt, AuthRoutes.kt, DriverRoutes.kt, test files) — constitution requires zero violations before PR
- [x] T018 Run `./gradlew build` to confirm clean compile with no warnings from Koin integration

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — T001 then T002 sequentially
- **Foundational (Phase 2)**: Depends on Phase 1 — T003-T006 all parallel
- **US1+US2 (Phase 3)**: Depends on Phase 2 — T007→T008 sequential, T009+T010 parallel after T008
- **US3 (Phase 4)**: Depends on Phase 3 — T011 sequential
- **US4 (Phase 5)**: Depends on Phase 3 — T012+T013 parallel
- **Polish (Phase 6)**: Depends on Phase 4 and Phase 5 — T014-T018 sequential

### User Story Dependencies

- **US1 + US2 (P1)**: Tightly coupled — same implementation work delivers both (DI migration = behavior preservation + DI adoption). Start after Phase 2.
- **US3 (P2)**: Depends on US2 completion (modules must exist to verify organization). Lightweight review task.
- **US4 (P2)**: Depends on US1+US2 completion (application must run with Koin before tests can be adapted). Can run in parallel with US3.

### Within Each Phase

- Phase 2: All four module files are independent (different files, no cross-dependencies) → all parallel
- Phase 3: Application.kt must be migrated before Routing.kt (Koin install before inject). Routes are independent after Routing.kt.
- Phase 5: Both test files are independent → parallel

### Parallel Opportunities

- T003 + T004 + T005 + T006: All four Koin module files (Phase 2)
- T009 + T010: AuthRoutes.kt and DriverRoutes.kt (Phase 3, after T008)
- T012 + T013: Both test files (Phase 5)
- Phase 4 (US3) + Phase 5 (US4): Can run in parallel after Phase 3

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Launch all four module files together:
Task: "Create CoreModule.kt in src/main/kotlin/com/blaizmiko/infrastructure/di/CoreModule.kt"
Task: "Create ClientModule.kt in src/main/kotlin/com/blaizmiko/infrastructure/di/ClientModule.kt"
Task: "Create AuthModule.kt in src/main/kotlin/com/blaizmiko/infrastructure/di/AuthModule.kt"
Task: "Create DriversModule.kt in src/main/kotlin/com/blaizmiko/infrastructure/di/DriversModule.kt"
```

## Parallel Example: Phase 3 (Route Migration)

```bash
# After T008 (Routing.kt), launch both route files together:
Task: "Refactor AuthRoutes.kt to use inject()"
Task: "Refactor DriverRoutes.kt to use inject()"
```

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Complete Phase 1: Add Koin dependencies
2. Complete Phase 2: Create all four Koin module files (parallel)
3. Complete Phase 3: Migrate Application.kt → Routing.kt → Routes
4. **STOP and VALIDATE**: Run `./gradlew test` — all 13 tests should pass
5. If tests pass → MVP complete (behavior preserved + DI adopted)

### Incremental Delivery

1. Phase 1 + 2 → Dependencies added, modules declared
2. Phase 3 (US1+US2) → Full DI migration, tests pass → **MVP deployed**
3. Phase 4 (US3) → Module organization verified and documented
4. Phase 5 (US4) → Test setup cleaned up to use Koin idiomatically
5. Phase 6 → Final smoke test and verification

### Single Developer Strategy

Execute phases sequentially: 1 → 2 → 3 → validate → 4 + 5 → 6. Total: 18 tasks across 6 phases.

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US1 and US2 are co-delivered (same implementation, different acceptance criteria)
- No new tests created — existing 13 tests are the regression gate
- Commit after each phase or logical group
- Stop at Phase 3 checkpoint to validate MVP independently
- Reference research.md for exact Koin patterns and scoping decisions
- Reference data-model.md for complete component → module mapping
