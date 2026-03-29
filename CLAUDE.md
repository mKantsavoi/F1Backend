# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
./gradlew build                    # Full build (compile + test + checks)
./gradlew classes                  # Compile main sources only

# Test
./gradlew test                     # Run all tests (some need Docker for TestContainers)
./gradlew test --tests "com.blaizmiko.f1backend.integration.CircuitsEndpointTest"  # Single test class
./gradlew test --tests "*.CircuitsEndpointTest.happy path*"  # Single test by name pattern

# Code quality
./gradlew ktlintCheck              # Check code style
./gradlew ktlintFormat             # Auto-fix code style
./gradlew detekt                   # Static analysis
./gradlew ktlintCheck detekt test  # Full quality gate

# Run
./gradlew run                      # Start server (needs DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET env vars)
```

Tests in `DriversEndpointTest`, `TeamsEndpointTest`, and `FavoritesEndpointTest` require Docker running for PostgreSQL TestContainers. Other integration tests use in-memory test setups.

## Architecture

Clean Architecture with four layers:

- **adapter/** — HTTP routes (`route/`), request/response DTOs (`dto/`), port interfaces (`port/`)
- **domain/** — Models (`model/`), repository interfaces (`repository/`), data source interfaces (`port/`)
- **infrastructure/** — Implementations: database (`persistence/`), caching (`cache/`), external APIs (`external/`), DI modules (`di/`), security (`security/`), config (`config/`)
- **usecase/** — Business logic orchestration (one class per use case)

### Key patterns

**Dependency flow:** Routes → Use Cases → Domain Ports ← Infrastructure Implementations. Use cases never depend on infrastructure directly; they depend on domain-level port interfaces.

**Koin DI:** Each feature has its own module in `infrastructure/di/`. Modules are registered in `Application.kt`. Use `get()` for auto-resolution; explicitly pass config values from `JolpicaConfig`/`JwtConfig` where needed.

**Caching:** All caches go through `CacheProvider` (port) / `CacheRegistry` (implementation) using Caffeine+Aedile. Cache categories are defined in `CacheSpec` enum with TTL and max size. Use cases implement: quick cache check → mutex lock → double-check → fetch with retry throttle → stale fallback on failure.

**External API:** Jolpica F1 API clients in `infrastructure/external/client/` implement domain port interfaces. Response models in `JolpicaModels.kt`, mapping to domain in `JolpicaMappers.kt`.

**Database:** Exposed ORM with table definitions in `persistence/table/`, repository implementations in `persistence/repository/`. All DB calls wrapped in `dbQuery { }` suspend function from `DatabaseFactory`.

**Auth:** JWT with HMAC256. Access tokens (15min) + refresh tokens (30day) with rotation and reuse detection. `/api/v1/auth` routes are unauthenticated; all other `/api/v1/*` routes require `authenticate { }`.

**Error handling:** Domain exceptions (`ValidationException`, `AuthenticationException`, `ConflictException`, `NotFoundException`, `ExternalServiceException`) mapped to HTTP status codes via Ktor `StatusPages` in `Routing.kt`.

### Stale-on-error pattern in use cases

Use cases serve expired cached data when the external API fails. The `Warning: 110 - "Response is stale"` header signals stale responses. Fallback data is stored in `CacheProvider.putFallback()` alongside the Caffeine cache.

## Code Style

- Kotlin 2.3.0 targeting JVM 21
- ktlint enforces formatting (max line length 140, function params on separate lines for multi-param signatures)
- detekt enforces static analysis (suppress `MagicNumber` for config enums, `LongParameterList` for DI-heavy constructors)
- kotlinx.serialization for JSON (not Jackson)

## Specs

Feature specs live in `specs/NNN-feature-name/` with `spec.md`, `plan.md`, `tasks.md`, `data-model.md`, `research.md`. These document requirements and implementation plans — check them for context on design decisions.
