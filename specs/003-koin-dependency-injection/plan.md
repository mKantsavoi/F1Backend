# Implementation Plan: Koin Dependency Injection

**Branch**: `003-koin-dependency-injection` | **Date**: 2026-03-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/003-koin-dependency-injection/spec.md`

## Summary

Replace manual dependency wiring in Application.kt and Routing.kt with Koin 4.2.0 DI container. Install Koin as a Ktor plugin, declare all existing components (~20) across four logically organized modules (core, client, auth, drivers), and convert route handlers to use Koin injection. Zero changes to API behavior — all 13 existing tests must pass.

## Technical Context

**Language/Version**: Kotlin 2.3.0, JVM 21
**Primary Dependencies**: Ktor 3.4.1, Koin 4.2.0 (BOM), Exposed 1.0.0, kotlinx.serialization
**Storage**: PostgreSQL via Exposed ORM + HikariCP
**Testing**: Kotest 6.1.5 + ktor-server-test-host + TestContainers 1.21.4
**Target Platform**: Linux server (Docker)
**Project Type**: Web service (REST API)
**Performance Goals**: Correctness-first phase; no performance targets for this refactoring
**Constraints**: Zero behavioral changes to existing endpoints; all 13 tests pass
**Scale/Scope**: ~20 injectable components across 2 feature areas (auth, drivers)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture | PASS | Koin modules respect layer boundaries: domain has no Koin imports, DI declarations live in infrastructure layer |
| II. API-First Design | PASS | No API changes — existing OpenAPI/Swagger docs remain valid |
| III. Test Coverage | PASS | All 13 existing tests must pass; test setup adapts to Koin but assertions unchanged |
| IV. Security & Input Validation | PASS | No security changes; JWT auth and input validation unaffected |
| V. Simplicity & Established Libraries | PASS | Koin is the established DI framework for Ktor; justified by scaling needs documented in spec |
| VI. Dependency Verification via Context7 | PASS | Koin 4.2.0 verified via Context7 (BOM approach with koin-ktor, koin-logger-slf4j, koin-test) |

**Post-Phase 1 re-check**: All gates still pass. DI module files live in `infrastructure/di/` (infrastructure layer). No domain layer contamination.

## Project Structure

### Documentation (this feature)

```text
specs/003-koin-dependency-injection/
├── plan.md              # This file
├── research.md          # Phase 0: Koin version, patterns, scoping decisions
├── data-model.md        # Phase 1: Dependency graph (no schema changes)
├── quickstart.md        # Phase 1: Setup and verification guide
├── contracts/           # Phase 1: No API changes (README only)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
src/main/kotlin/
├── Application.kt                              # MODIFY: install Koin plugin
├── Routing.kt                                  # MODIFY: remove dependency params, inject via Koin
└── com/blaizmiko/
    ├── adapter/route/
    │   ├── AuthRoutes.kt                       # MODIFY: remove params, use inject()
    │   └── DriverRoutes.kt                     # MODIFY: remove params, use inject()
    ├── domain/                                 # UNCHANGED (no Koin imports here)
    ├── infrastructure/
    │   ├── di/                                 # NEW: Koin module declarations
    │   │   ├── CoreModule.kt                   # AppConfig, JwtProvider, sub-configs
    │   │   ├── ClientModule.kt                 # JolpicaClient → DriverDataSource
    │   │   ├── AuthModule.kt                   # Repositories + auth use cases
    │   │   └── DriversModule.kt                # DriverCache + GetDrivers
    │   ├── cache/                              # UNCHANGED
    │   ├── config/                             # UNCHANGED
    │   ├── external/                           # UNCHANGED
    │   ├── persistence/                        # UNCHANGED
    │   └── security/                           # UNCHANGED
    └── usecase/                                # UNCHANGED

src/test/kotlin/
└── com/blaizmiko/integration/
    ├── AuthLifecycleTest.kt                    # MODIFY: test module uses Koin
    └── DriversEndpointTest.kt                  # MODIFY: test module uses Koin with fake overrides
```

**Structure Decision**: Single project structure (existing). New files only in `infrastructure/di/` — four Koin module files. All other source files are modifications to existing code, not new files.

## Complexity Tracking

No constitution violations. No complexity justification needed.
