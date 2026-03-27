# Quickstart: Championship Standings Endpoints

**Feature**: 007-championship-standings

## Prerequisites

- JDK 21
- Docker (for testcontainers in existing auth tests)
- Valid `application.yml` with Jolpica API config

## Build & Test

```bash
./gradlew ktlintCheck detekt test
```

## Key Implementation Patterns

### Follow the Drivers/Teams pattern exactly

Every new endpoint follows this established pipeline:

1. **Domain model** in `domain/model/` — plain Kotlin data class
2. **Data source port** in `domain/port/` — interface with `suspend fun fetch*()`
3. **Cache port** in `adapter/port/` — interface with `get()`/`put()`
4. **Cache implementation** in `infrastructure/cache/` — ConcurrentHashMap keyed by season
5. **JolpicaClient** in `infrastructure/external/client/` — implements data source port, maps Jolpica JSON to domain model
6. **Use case** in `usecase/` — double-check locking, mutex per key, retry throttle, stale fallback
7. **DTOs** in `adapter/dto/` — `@Serializable` response classes
8. **Route handler** in `adapter/route/` — Koin lazy inject, validate params, map domain to DTO, Warning header for stale
9. **Koin module** in `infrastructure/di/` — `single {}` for cache + use case, `bind` for interface
10. **Register module** in `Application.kt` — add to `modules()` call
11. **Integration test** — fake data source, test app builder, JWT token, assert status + body + headers

### Cache TTL Quick Reference

| Data | TTL | Key Pattern |
|------|-----|-------------|
| Driver standings (current season) | 1 hour | season string |
| Driver standings (past season) | forever | season string |
| Constructor standings (current season) | 1 hour | season string |
| Constructor standings (past season) | forever | season string |

### Validation

- Season: reuse existing `validateSeason()` in `RouteValidation.kt`
- No round parameter needed (standings are per-season)

### Jolpica API Paths

- Driver standings: `GET /ergast/f1/{season}/driverstandings.json`
- Constructor standings: `GET /ergast/f1/{season}/constructorstandings.json`

## Testing Approach

Each endpoint gets its own test file with a `FakeStandingsDataSource` + test app builder (same pattern as `DriversEndpointTest.kt`):

- Happy path: returns correct standings data
- 401: rejected without token
- Cache hit: second call doesn't hit data source
- Stale fallback: data source fails after cache populated — returns cached with Warning header
- 502: data source fails with no cache — returns 502
- 400: invalid season parameter

## Key Differences from Prior Features

- **New wrapper type**: `StandingsData<T>` instead of `SeasonCache<T>` because standings include a `round` field
- **No round parameter**: Standings endpoints use only season (query param), no path params
- **1-hour TTL**: Longer than race results (5 min) because standings change less frequently
