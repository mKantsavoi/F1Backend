# Quickstart: Schedule, Race Results & Qualifying Endpoints

**Feature**: 006-schedule-race-results

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
4. **Cache implementation** in `infrastructure/cache/` — ConcurrentHashMap or AtomicReference
5. **JolpicaClient** in `infrastructure/external/` — implements data source port, maps Jolpica JSON to domain model
6. **Use case** in `usecase/` — double-check locking, mutex per key, retry throttle, stale fallback
7. **DTOs** in `adapter/dto/` — `@Serializable` response classes
8. **Route handler** in `adapter/route/` — Koin lazy inject, validate params, map domain→DTO, Warning header for stale
9. **Koin module** in `infrastructure/di/` — `single {}` for cache + use case, `bind` for interface
10. **Register module** in `Application.kt` — add to `modules()` call
11. **Integration test** — fake data source, test app builder, JWT token, assert status + body + headers

### Cache TTL Quick Reference

| Data | TTL | Key Pattern |
|------|-----|-------------|
| Schedule | 6 hours | season string |
| Next race | 1 hour | single value |
| Race results (current season) | 5 minutes | `"results:{season}:{round}"` |
| Race results (past season) | forever | `"results:{season}:{round}"` |
| Qualifying | forever | `"qualifying:{season}:{round}"` |
| Sprint | forever | `"sprint:{season}:{round}"` |

### Validation

- Season: reuse existing `validateSeason()` in `RouteValidation.kt`
- Round: add `validateRound()` — must be positive integer 1-99

### New Exception

Add `NotFoundException` to `Exceptions.kt`, map to 404 in StatusPages (`configureStatusPages()` in `Routing.kt`).

## Testing Approach

Each endpoint gets its own test file with a `FakeDataSource` + test app builder (same pattern as `DriversEndpointTest.kt`):

- Happy path: returns correct data
- 401: rejected without token
- Cache hit: second call doesn't hit data source
- Stale fallback: data source fails after cache populated → returns cached with Warning header
- 502: data source fails with no cache → returns 502
- 400: invalid season/round parameters
- 404 (sprint only): non-sprint round returns 404
