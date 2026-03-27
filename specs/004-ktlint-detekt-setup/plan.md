# Implementation Plan: Add ktlint and detekt Static Analysis

**Branch**: `004-ktlint-detekt-setup` | **Date**: 2026-03-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/004-ktlint-detekt-setup/spec.md`

## Summary

Add ktlint (code formatting) and detekt (static analysis) Gradle plugins to enforce the constitution's code quality gates. Both plugins will be added via the version catalog, configured with sensible defaults, and all existing code violations will be fixed so both tasks pass cleanly from day one.

## Technical Context

**Language/Version**: Kotlin 2.3.0, JVM 21
**Primary Dependencies**: ktlint-gradle 14.0.1, detekt 1.23.7
**Storage**: N/A
**Testing**: kotest 6.1.5 + ktor-server-test-host (existing — unchanged)
**Target Platform**: JVM server
**Project Type**: Web service (Ktor REST API)
**Performance Goals**: N/A (build tooling only)
**Constraints**: No functional changes; all existing tests must pass
**Scale/Scope**: ~41 Kotlin source files, 2 test files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Clean Architecture | PASS | No architectural changes — build config only |
| II. API-First Design | PASS | No endpoint changes |
| III. Test Coverage | PASS | No new features requiring tests; existing tests unaffected |
| IV. Security & Input Validation | PASS | No runtime changes |
| V. Simplicity & Established Libraries | PASS | ktlint and detekt are well-established, required by constitution |
| VI. Dependency Verification via Context7 | PASS | Versions verified: ktlint-gradle 14.0.1, detekt 1.23.7 |
| Code Quality Gates | PASS | This feature directly implements the ktlint + detekt gate |

**Post-Phase 1 re-check**: All gates remain PASS. This feature adds no runtime code, only build configuration and style fixes.

## Project Structure

### Documentation (this feature)

```text
specs/004-ktlint-detekt-setup/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (minimal — no data entities)
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
build.gradle.kts              # Add ktlint + detekt plugins
gradle/libs.versions.toml     # Add plugin versions + entries
detekt.yml                    # New: detekt configuration file
src/
├── main/kotlin/com/blaizmiko/f1backend/   # 39 source files (style fixes only)
└── test/kotlin/com/blaizmiko/f1backend/   # 2 test files (style fixes only)
```

**Structure Decision**: This feature modifies only build configuration files at the project root and applies non-functional formatting/style fixes to existing source files. No new directories or source files are created.
