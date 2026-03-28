# Quickstart: 008-drivers-db-detail

**Date**: 2026-03-28

## Prerequisites

- JDK 21+
- Docker (for PostgreSQL via testcontainers in tests, or local dev DB)

## New Dependencies

None. All enrichment data comes from bundled JSON files.

## Key Files to Create

### Database Layer
- `infrastructure/persistence/table/TeamsTable.kt` — Exposed DSL table definition
- `infrastructure/persistence/table/DriversTable.kt` — Exposed DSL table definition
- `infrastructure/persistence/repository/ExposedTeamRepository.kt` — Team repository implementation
- `infrastructure/persistence/repository/ExposedDriverRepository.kt` — Driver repository implementation

### Domain Layer
- `domain/repository/TeamRepository.kt` — Team repository interface
- `domain/repository/DriverRepository.kt` — Driver repository interface

### Use Cases
- `usecase/GetDriverDetail.kt` — New use case for single driver lookup

### Seed
- `infrastructure/seed/DriverSeedService.kt` — Orchestrates full seed process

### Resources
- `src/main/resources/seed/driver-photos.json` — Photo URL mapping file
- `src/main/resources/seed/driver-biographies.json` — Pre-written driver biographies

### DI
- Update `infrastructure/di/DriversModule.kt` — Replace cache with repository + seed service
- Update `infrastructure/di/TeamsModule.kt` — Replace cache with repository

## Key Files to Modify

- `Application.kt` — Add seed invocation after DatabaseFactory.init()
- `DatabaseFactory.kt` — Add TeamsTable, DriversTable to SchemaUtils.create()
- `adapter/dto/DriverResponses.kt` — Add photoUrl to DriverDto, add DriverDetailResponse
- `adapter/route/DriverRoutes.kt` — Add /{driverId} route, update list to use DB
- `adapter/route/TeamRoutes.kt` — Update to use DB repository
- `usecase/GetDrivers.kt` — Rewrite to read from repository instead of cache
- `usecase/GetTeams.kt` — Rewrite to read from repository instead of cache

## Key Files to Delete

- `adapter/port/DriverCache.kt`
- `adapter/port/TeamCache.kt`
- `infrastructure/cache/InMemoryDriverCache.kt`
- `infrastructure/cache/InMemoryTeamCache.kt`

## Running Tests

```bash
./gradlew ktlintCheck detekt test
```

Integration tests use testcontainers (PostgreSQL). Docker must be running.

## Smoke Test

1. Start app with empty DB
2. Check logs for seed completion messages
3. `curl -H "Authorization: Bearer <token>" localhost:8080/api/v1/drivers` — should return 22 drivers with photoUrl
4. `curl -H "Authorization: Bearer <token>" localhost:8080/api/v1/drivers/max_verstappen` — should return full card with team and biography
5. Restart app — logs should say "Drivers table not empty, skipping seed"
