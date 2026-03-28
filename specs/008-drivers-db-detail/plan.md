# Implementation Plan: Driver Persistence & Detail Endpoint

**Branch**: `008-drivers-db-detail` | **Date**: 2026-03-28 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/008-drivers-db-detail/spec.md`

## Summary

Persist drivers and teams to PostgreSQL, replacing the in-memory Jolpica cache for both. Add a driver detail endpoint returning a full card with photo, team (via JOIN), and biography. A startup seed mechanism fetches base data from Jolpica, maps photo URLs and biographies from bundled JSON files, and populates the database. No external AI service dependency — biographies are pre-written and bundled.

## Technical Context

**Language/Version**: Kotlin 2.3.0, JVM 21
**Primary Dependencies**: Ktor 3.4.1, Exposed 1.0.0, Koin 4.2.0, kotlinx.serialization (no new dependencies)
**Storage**: PostgreSQL via Exposed ORM + HikariCP
**Testing**: Kotest 6.1.5, ktor-server-test-host, testcontainers 1.21.4 (PostgreSQL)
**Target Platform**: Linux server (Docker)
**Project Type**: Web service (REST API)
**Performance Goals**: Correctness first (per constitution); standard CRUD response times
**Constraints**: No Spring; Ktor only; ktlint + detekt must pass
**Scale/Scope**: 22 drivers, 11 teams — small dataset, single-instance deployment

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture | PASS | Domain repos as interfaces, infrastructure implements. Seed service in infrastructure. Use cases stateless. |
| II. API-First Design | PASS | Contracts defined in `contracts/drivers-api.md`. OpenAPI/Swagger annotations required. |
| III. Test Coverage | PASS | Integration tests with testcontainers for DB. No mocks for DB. Kotest + ktor-server-test-host. |
| IV. Security & Input Validation | PASS | JWT on both endpoints. driverId validated at adapter layer. No secrets in code. |
| V. Simplicity | PASS | No new dependencies. Bundled JSON files for enrichment. No over-engineering. |
| VI. Context7 Verification | PASS | Exposed JOIN patterns verified via Context7. No new dependencies to verify. |
| VII. Koin DI | PASS | DriversModule and TeamsModule updated. One module per feature. Interface binding. |
| VIII. Static Analysis | PASS | ktlintCheck + detekt required to pass. |

**Post-Phase 1 re-check**: All gates still pass. The design adds:
- 2 new Exposed tables (TeamsTable, DriversTable) — follows existing UsersTable pattern
- 2 new repository interfaces + implementations — follows existing UserRepository pattern
- 1 new use case (GetDriverDetail) — stateless, follows existing pattern
- 1 new infrastructure service (seed) — proper layering, no external AI dependency
- No new dependencies added

## Project Structure

### Documentation (this feature)

```text
specs/008-drivers-db-detail/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 research output
├── data-model.md        # Phase 1 data model
├── quickstart.md        # Phase 1 quickstart guide
├── contracts/
│   └── drivers-api.md   # API contract definitions
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
src/main/kotlin/com/blaizmiko/f1backend/
├── Application.kt                          # MODIFY: add seed invocation
├── adapter/
│   ├── dto/
│   │   ├── DriverResponses.kt              # MODIFY: add photoUrl, add DriverDetailResponse
│   │   └── TeamResponses.kt                # (unchanged)
│   ├── port/
│   │   ├── DriverCache.kt                  # DELETE
│   │   └── TeamCache.kt                    # DELETE
│   └── route/
│       ├── DriverRoutes.kt                 # MODIFY: add /{driverId}, update list source
│       └── TeamRoutes.kt                   # MODIFY: update to use DB repository
├── domain/
│   ├── model/
│   │   ├── Driver.kt                       # MODIFY: add photoUrl field
│   │   └── Team.kt                         # (unchanged)
│   ├── port/
│   │   └── (existing data sources unchanged)
│   └── repository/
│       ├── DriverRepository.kt             # NEW: interface
│       └── TeamRepository.kt               # NEW: interface
├── infrastructure/
│   ├── cache/
│   │   ├── InMemoryDriverCache.kt          # DELETE
│   │   └── InMemoryTeamCache.kt            # DELETE
│   ├── di/
│   │   ├── DriversModule.kt                # MODIFY: repo + seed + use cases
│   │   └── TeamsModule.kt                  # MODIFY: repo + use case
│   ├── persistence/
│   │   ├── DatabaseFactory.kt              # MODIFY: add new tables
│   │   ├── repository/
│   │   │   ├── ExposedDriverRepository.kt  # NEW
│   │   │   └── ExposedTeamRepository.kt    # NEW
│   │   └── table/
│   │       ├── ColumnLengths.kt            # MODIFY: add new constants
│   │       ├── DriversTable.kt             # NEW
│   │       └── TeamsTable.kt               # NEW
│   └── seed/
│       └── DriverSeedService.kt            # NEW: orchestrates seed
└── usecase/
    ├── GetDriverDetail.kt                  # NEW
    ├── GetDrivers.kt                       # MODIFY: read from repo
    └── GetTeams.kt                         # MODIFY: read from repo

src/main/resources/seed/
├── driver-photos.json                      # NEW: bundled photo URL mapping
└── driver-biographies.json                 # NEW: bundled biographies

src/test/kotlin/com/blaizmiko/f1backend/integration/
├── DriversEndpointTest.kt                  # MODIFY: rewrite for DB source
├── DriverDetailEndpointTest.kt             # NEW
└── TeamsEndpointTest.kt                    # MODIFY: rewrite for DB source
```

**Structure Decision**: Follows existing project layout exactly. New files placed in established package locations. Seed service gets its own sub-package (`infrastructure/seed/`). No new external HTTP clients needed — biographies come from bundled file.

## Complexity Tracking

No constitution violations. No complexity justifications needed.
