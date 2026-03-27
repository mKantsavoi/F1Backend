# Tasks: JWT Authentication & Authorization

**Input**: Design documents from `/specs/001-jwt-auth/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: Included per constitution (Principle III: Test Coverage is NON-NEGOTIABLE) and SC-006 (integration test required).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/main/kotlin/com/blaizmiko/` for source, `src/test/kotlin/com/blaizmiko/` for tests
- All paths relative to repository root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project dependencies, configuration files, and directory structure

- [x] T001 Update dependency catalog with all required libraries in gradle/libs.versions.toml (add Exposed 1.0.0, HikariCP 6.2.1, PostgreSQL driver 42.7.10, bcrypt 0.10.2, kotest 6.1.5, testcontainers 1.21.4, ktor-server-auth, ktor-server-auth-jwt, ktor-server-content-negotiation, ktor-serialization-kotlinx-json, ktor-server-status-pages, ktor-server-openapi, ktor-server-swagger)
- [x] T002 Update build.gradle.kts with all dependencies from version catalog, add kotlinx.serialization plugin, and configure kotest JUnit5 test runner
- [x] T003 Create Clean Architecture package directories under src/main/kotlin/com/blaizmiko/ (domain/model/, domain/repository/, usecase/, adapter/route/, adapter/dto/, infrastructure/persistence/table/, infrastructure/persistence/repository/, infrastructure/security/, infrastructure/config/) and src/test/kotlin/com/blaizmiko/ (usecase/, adapter/route/, integration/)
- [x] T004 Create Dockerfile with multi-stage build (gradle build stage + JRE 21 runtime stage) at Dockerfile
- [x] T005 Create docker-compose.yml at project root with PostgreSQL service (port 5432, f1backend db/user/password) and app service (port 8080, depends on db, environment variables for DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**WARNING**: No user story work can begin until this phase is complete

- [x] T006 Implement AppConfig reading JWT and database settings from environment/application.yaml in src/main/kotlin/com/blaizmiko/infrastructure/config/AppConfig.kt
- [x] T007 Update src/main/resources/application.yaml with database connection settings (url, user, password from env vars), JWT settings (secret, issuer=f1backend, audience=f1backend-api, accessTokenExpiry=900, refreshTokenExpiry=2592000 from env vars), and reference AppConfig
- [x] T008 Implement DatabaseFactory with HikariCP connection pool and Exposed schema creation (SchemaUtils.create for both tables) in src/main/kotlin/com/blaizmiko/infrastructure/persistence/DatabaseFactory.kt
- [x] T009 [P] Define User domain model (id: UUID, email, username, passwordHash, role enum USER/ADMIN, createdAt, updatedAt) as pure Kotlin data class in src/main/kotlin/com/blaizmiko/domain/model/User.kt
- [x] T010 [P] Define RefreshToken domain model (id: UUID, userId, tokenHash, expiresAt, revoked, createdAt) as pure Kotlin data class in src/main/kotlin/com/blaizmiko/domain/model/RefreshToken.kt
- [x] T011 [P] Define UserRepository interface (findById, findByEmail, create, updateUsername, updatePasswordHash) in src/main/kotlin/com/blaizmiko/domain/repository/UserRepository.kt
- [x] T012 [P] Define RefreshTokenRepository interface (findByTokenHash, create, revokeByTokenHash, revokeAllForUser) in src/main/kotlin/com/blaizmiko/domain/repository/RefreshTokenRepository.kt
- [x] T013 [P] Define UsersTable (Exposed DSL object with uuid id, varchar email with uniqueIndex, varchar username, varchar passwordHash, varchar role default "user", datetime createdAt, datetime updatedAt) in src/main/kotlin/com/blaizmiko/infrastructure/persistence/table/UsersTable.kt
- [x] T014 [P] Define RefreshTokensTable (Exposed DSL object with uuid id, reference userId to UsersTable with CASCADE delete, varchar tokenHash with index, datetime expiresAt, bool revoked default false, datetime createdAt; index on userId) in src/main/kotlin/com/blaizmiko/infrastructure/persistence/table/RefreshTokensTable.kt
- [x] T015 Implement ExposedUserRepository (all UserRepository methods using Exposed DSL, email stored lowercase for case-insensitive lookup) in src/main/kotlin/com/blaizmiko/infrastructure/persistence/repository/ExposedUserRepository.kt
- [x] T016 Implement ExposedRefreshTokenRepository (all RefreshTokenRepository methods using Exposed DSL) in src/main/kotlin/com/blaizmiko/infrastructure/persistence/repository/ExposedRefreshTokenRepository.kt
- [x] T017 [P] Implement PasswordHasher (hash with BCrypt, verify password against hash) using at.favre.lib:bcrypt in src/main/kotlin/com/blaizmiko/infrastructure/security/PasswordHasher.kt
- [x] T018 [P] Implement TokenHasher (SHA-256 hex digest of raw token string, generate secure random token as Base64 using SecureRandom 128-bit) in src/main/kotlin/com/blaizmiko/infrastructure/security/TokenHasher.kt
- [x] T019 Implement JwtProvider (generateAccessToken with sub=userId/iss=f1backend/aud=f1backend-api/role claim/15min expiry, configureAuth extension to install Ktor JWT verifier checking signature+expiry+issuer+audience) in src/main/kotlin/com/blaizmiko/infrastructure/security/JwtProvider.kt
- [x] T020 [P] Create request DTOs (RegisterRequest, LoginRequest, RefreshRequest, LogoutRequest, UpdateProfileRequest, ChangePasswordRequest) with kotlinx.serialization @Serializable in src/main/kotlin/com/blaizmiko/adapter/dto/AuthRequests.kt
- [x] T021 [P] Create response DTOs (TokenResponse with accessToken/refreshToken/expiresIn, ProfileResponse with id/email/username/role/createdAt, ErrorResponse with error/message, MessageResponse with message) with kotlinx.serialization @Serializable in src/main/kotlin/com/blaizmiko/adapter/dto/AuthResponses.kt
- [x] T022 Install ContentNegotiation with kotlinx.serialization JSON and StatusPages with domain exception-to-HTTP mapping (ValidationException→400, AuthenticationException→401, ConflictException→409) in src/main/kotlin/com/blaizmiko/Routing.kt, wire up DatabaseFactory.init and JWT auth configuration in src/main/kotlin/com/blaizmiko/Application.kt

**Checkpoint**: Foundation ready — database connects, tables created, security utilities functional, DTOs defined. User story implementation can now begin.

---

## Phase 3: User Story 1 — Account Registration (Priority: P1) MVP

**Goal**: New users can create accounts and receive tokens to immediately access protected resources.

**Independent Test**: POST /api/v1/auth/register with valid data returns 201 with token pair; invalid data returns 400; duplicate email returns 409.

### Tests for User Story 1

- [x] T023 [P] [US1] Write route test for POST /api/v1/auth/register (success 201 with tokens, validation errors 400, duplicate email 409) using ktor-server-test-host in src/test/kotlin/com/blaizmiko/adapter/route/RegisterRouteTest.kt

### Implementation for User Story 1

- [x] T024 [US1] Implement RegisterUser use case (validate email/username/password, check email uniqueness via UserRepository, hash password, create user, generate refresh token, hash and store it, generate access token, return token pair) in src/main/kotlin/com/blaizmiko/usecase/RegisterUser.kt
- [x] T025 [US1] Add POST /register route in src/main/kotlin/com/blaizmiko/adapter/route/AuthRoutes.kt (parse RegisterRequest, call RegisterUser, return 201 TokenResponse; input validation for email format, password strength, username rules at adapter layer before calling use case)
- [x] T026 [US1] Wire AuthRoutes into Application module: create route mounting function, instantiate repositories and use cases, register /api/v1/auth route group in src/main/kotlin/com/blaizmiko/Routing.kt

**Checkpoint**: Registration works end-to-end. A new user can create an account and receive a token pair. Route test passes.

---

## Phase 4: User Story 2 — Login (Priority: P1)

**Goal**: Returning users can authenticate with email/password and receive a fresh token pair.

**Independent Test**: POST /api/v1/auth/login with valid credentials returns 200 with tokens; invalid credentials return 401 with generic error.

### Tests for User Story 2

- [x] T027 [P] [US2] Write route test for POST /api/v1/auth/login (success 200 with tokens, invalid credentials 401 generic error, nonexistent email 401 same error) using ktor-server-test-host in src/test/kotlin/com/blaizmiko/adapter/route/LoginRouteTest.kt

### Implementation for User Story 2

- [x] T028 [US2] Implement LoginUser use case (find user by email, verify password hash, generate new refresh token and store hashed, generate access token, return token pair; throw generic AuthenticationException on any failure) in src/main/kotlin/com/blaizmiko/usecase/LoginUser.kt
- [x] T029 [US2] Add POST /login route in src/main/kotlin/com/blaizmiko/adapter/route/AuthRoutes.kt (parse LoginRequest, call LoginUser, return 200 TokenResponse)

**Checkpoint**: Login works end-to-end. A registered user can log in and receive tokens. Route test passes.

---

## Phase 5: User Story 3 — Accessing Protected Resources (Priority: P1)

**Goal**: Protected endpoints reject invalid/missing/expired tokens with 401 and accept valid tokens.

**Independent Test**: GET /api/v1/auth/me with valid token returns 200; with no/expired/tampered token returns 401.

### Tests for User Story 3

- [x] T030 [P] [US3] Write route test for JWT authentication middleware (valid token accepted, missing token 401, expired token 401, tampered token 401) using ktor-server-test-host in src/test/kotlin/com/blaizmiko/adapter/route/ProtectedRouteTest.kt

### Implementation for User Story 3

- [x] T031 [US3] Implement GetProfile use case (find user by ID from JWT claims, return user data) in src/main/kotlin/com/blaizmiko/usecase/GetProfile.kt
- [x] T032 [US3] Add GET /me protected route in src/main/kotlin/com/blaizmiko/adapter/route/AuthRoutes.kt (extract userId from JWTPrincipal, call GetProfile, return 200 ProfileResponse) wrapped in authenticate block

**Checkpoint**: Token-based access control works. Protected endpoints correctly reject bad tokens and serve data for valid ones. Route test passes.

---

## Phase 6: User Story 4 — Transparent Token Refresh (Priority: P2)

**Goal**: Clients can exchange a valid refresh token for a new token pair. Old refresh token is invalidated immediately (rotation).

**Independent Test**: POST /api/v1/auth/refresh with valid refresh token returns 200 with new pair; old refresh token no longer works; expired/revoked tokens return 401.

### Tests for User Story 4

- [x] T033 [P] [US4] Write route test for POST /api/v1/auth/refresh (success 200 with new tokens, old token revoked after rotation, expired token 401, revoked token 401) using ktor-server-test-host in src/test/kotlin/com/blaizmiko/adapter/route/RefreshRouteTest.kt

### Implementation for User Story 4

- [x] T034 [US4] Implement RefreshTokens use case (hash incoming token, find in DB, verify not revoked and not expired, revoke old token, generate new refresh token and store hashed, generate new access token, return token pair) in src/main/kotlin/com/blaizmiko/usecase/RefreshTokens.kt
- [x] T035 [US4] Add POST /refresh route in src/main/kotlin/com/blaizmiko/adapter/route/AuthRoutes.kt (parse RefreshRequest, call RefreshTokens, return 200 TokenResponse)

**Checkpoint**: Token refresh with rotation works. Old tokens are invalidated. Route test passes.

---

## Phase 7: User Story 5 — Reuse Detection (Priority: P2)

**Goal**: If a previously-rotated refresh token is reused, all refresh tokens for that user are revoked (full session invalidation).

**Independent Test**: After refreshing, reuse the old token — all tokens for that user should be revoked; user must log in again.

### Tests for User Story 5

- [x] T036 [P] [US5] Write route test for reuse detection (refresh token A, get token B; reuse token A triggers revocation of all user tokens including B; subsequent refresh with B fails 401) using ktor-server-test-host in src/test/kotlin/com/blaizmiko/adapter/route/ReuseDetectionRouteTest.kt

### Implementation for User Story 5

- [x] T037 [US5] Extend RefreshTokens use case in src/main/kotlin/com/blaizmiko/usecase/RefreshTokens.kt: when a revoked (already-rotated) token is presented, call revokeAllForUser on RefreshTokenRepository before returning 401

**Checkpoint**: Reuse detection works. Replaying an old refresh token triggers full session revocation. Route test passes.

---

## Phase 8: User Story 6 — Logout (Priority: P3)

**Goal**: A logged-in user can explicitly revoke their refresh token so it cannot be reused.

**Independent Test**: POST /api/v1/auth/logout with valid access token and refresh token in body returns 200; subsequent refresh with that token fails 401.

### Tests for User Story 6

- [x] T038 [P] [US6] Write route test for POST /api/v1/auth/logout (success 200, refresh token revoked, subsequent refresh fails 401) using ktor-server-test-host in src/test/kotlin/com/blaizmiko/adapter/route/LogoutRouteTest.kt

### Implementation for User Story 6

- [x] T039 [US6] Implement LogoutUser use case (hash incoming refresh token, find in DB, verify belongs to authenticated user, revoke it) in src/main/kotlin/com/blaizmiko/usecase/LogoutUser.kt
- [x] T040 [US6] Add POST /logout protected route in src/main/kotlin/com/blaizmiko/adapter/route/AuthRoutes.kt (parse LogoutRequest, extract userId from JWT, call LogoutUser, return 200 MessageResponse)

**Checkpoint**: Logout works. Revoked refresh tokens cannot be reused. Route test passes.

---

## Phase 9: User Story 7 — Profile Management (Priority: P3)

**Goal**: Logged-in users can view their profile, update their username, and change their password.

**Independent Test**: GET /me returns profile; PATCH /me updates username; PUT /me/password changes password with current password confirmation.

### Tests for User Story 7

- [x] T041 [P] [US7] Write route tests for PATCH /me (success 200, invalid username 400) and PUT /me/password (success 200, wrong current password 401, weak new password 400) using ktor-server-test-host in src/test/kotlin/com/blaizmiko/adapter/route/ProfileRouteTest.kt

### Implementation for User Story 7

- [x] T042 [P] [US7] Implement UpdateProfile use case (validate new username rules, update via UserRepository, return updated user) in src/main/kotlin/com/blaizmiko/usecase/UpdateProfile.kt
- [x] T043 [P] [US7] Implement ChangePassword use case (verify current password hash, validate new password strength, hash new password, update via UserRepository) in src/main/kotlin/com/blaizmiko/usecase/ChangePassword.kt
- [x] T044 [US7] Add PATCH /me and PUT /me/password protected routes in src/main/kotlin/com/blaizmiko/adapter/route/AuthRoutes.kt (parse requests, extract userId from JWT, call respective use cases, return responses)

**Checkpoint**: Profile management works. Users can view, update username, and change password. Route tests pass.

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Integration test, Docker validation, API documentation, and final verification

- [x] T045 Write full auth lifecycle integration test using testcontainers PostgreSQL (register → access protected → token expires → refresh → access again → logout → refresh fails) in src/test/kotlin/com/blaizmiko/integration/AuthLifecycleTest.kt
- [x] T046 Configure OpenAPI spec generation and Swagger UI endpoint at /swagger in src/main/kotlin/com/blaizmiko/Routing.kt
- [x] T047 Validate docker-compose.yml works end-to-end: build image, start services, run quickstart.md curl commands against running container
- [x] T048 Run full test suite (./gradlew test) and verify all tests pass with zero failures

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Stories (Phase 3–9)**: All depend on Foundational phase completion
  - US1 (Registration): No dependencies on other stories
  - US2 (Login): No dependencies on other stories (shares infrastructure only)
  - US3 (Access Protected): No dependencies on other stories
  - US4 (Token Refresh): No dependencies on other stories
  - US5 (Reuse Detection): Depends on US4 (extends RefreshTokens use case)
  - US6 (Logout): No dependencies on other stories
  - US7 (Profile Management): No dependencies on other stories
- **Polish (Phase 10)**: Depends on all user stories being complete

### Within Each User Story

- Tests can be written first (they will fail until implementation is done)
- Use cases before routes (routes depend on use cases)
- Tasks marked [P] within a story can run in parallel
- Story complete before moving to next priority

### Parallel Opportunities

- T009–T014 (domain models, interfaces, tables) are all [P] within Phase 2
- T017–T021 (security utilities, DTOs) are all [P] within Phase 2
- After Phase 2, US1–US4 and US6–US7 can all start in parallel (US5 waits for US4)
- Within US7, T042 and T043 (use cases) can run in parallel

---

## Parallel Example: Phase 2 Foundational

```bash
# Launch all domain models and interfaces together:
Task: T009 "Define User domain model in domain/model/User.kt"
Task: T010 "Define RefreshToken domain model in domain/model/RefreshToken.kt"
Task: T011 "Define UserRepository interface in domain/repository/UserRepository.kt"
Task: T012 "Define RefreshTokenRepository interface in domain/repository/RefreshTokenRepository.kt"

# Launch all table definitions together:
Task: T013 "Define UsersTable in infrastructure/persistence/table/UsersTable.kt"
Task: T014 "Define RefreshTokensTable in infrastructure/persistence/table/RefreshTokensTable.kt"

# Launch all security utilities together:
Task: T017 "Implement PasswordHasher in infrastructure/security/PasswordHasher.kt"
Task: T018 "Implement TokenHasher in infrastructure/security/TokenHasher.kt"

# Launch all DTOs together:
Task: T020 "Create request DTOs in adapter/dto/AuthRequests.kt"
Task: T021 "Create response DTOs in adapter/dto/AuthResponses.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1 (Registration)
4. **STOP and VALIDATE**: Register a user, receive tokens, access GET /me
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 (Register) → Test independently → MVP
3. US2 (Login) → Test independently → Users can return
4. US3 (Access Protected) → Test independently → Security enforced
5. US4 (Token Refresh) → Test independently → Long-lived sessions
6. US5 (Reuse Detection) → Test independently → Stolen token protection
7. US6 (Logout) → Test independently → User session control
8. US7 (Profile Management) → Test independently → Self-service profile
9. Polish → Integration test + Docker + Swagger → Production-ready

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Constitution requires tests before PR merge (Principle III)
- Constitution requires real PostgreSQL in integration tests via testcontainers (no mocks)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
