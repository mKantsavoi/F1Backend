# Quickstart: Teams & Circuits Data Endpoints

## Implementation Order

1. **Domain models** — `Team.kt`, `Circuit.kt` (pure data classes, no dependencies)
2. **Port interfaces** — `TeamDataSource.kt`, `TeamCache.kt`, `CircuitDataSource.kt`, `CircuitCache.kt`
3. **Jolpica models** — Add `ConstructorTable`, `JolpicaConstructor`, `CircuitTable`, `JolpicaCircuit`, `JolpicaLocation` to `JolpicaModels.kt`
4. **JolpicaClient** — Add `fetchTeams()` and `fetchCircuits()` methods, implement new interfaces
5. **Cache implementations** — `InMemoryTeamCache.kt`, `InMemoryCircuitCache.kt`
6. **Use cases** — `GetTeams.kt` (clone of GetDrivers pattern), `GetCircuits.kt` (simplified — single key, indefinite TTL)
7. **DTOs** — `TeamResponses.kt`, `CircuitResponses.kt`
8. **Route validation** — Extract `validateSeason()` to shared `RouteValidation.kt`
9. **Routes** — `TeamRoutes.kt`, `CircuitRoutes.kt`
10. **DI modules** — `TeamsModule.kt`, `CircuitsModule.kt`
11. **ClientModule** — Add `TeamDataSource` and `CircuitDataSource` bindings
12. **Application.kt** — Register new modules
13. **Routing.kt** — Register new routes inside `authenticate` block
14. **Integration tests** — `TeamsEndpointTest.kt`, `CircuitsEndpointTest.kt`

## Key Files to Reference

- Drivers pattern: `usecase/GetDrivers.kt`, `adapter/route/DriverRoutes.kt`, `infrastructure/di/DriversModule.kt`
- Cache pattern: `domain/model/CacheEntry.kt`, `infrastructure/cache/InMemoryDriverCache.kt`
- Jolpica pattern: `infrastructure/external/JolpicaClient.kt`, `infrastructure/external/JolpicaModels.kt`
- Test pattern: `integration/DriversEndpointTest.kt`

## How to Verify

```bash
./gradlew ktlintCheck    # Code style
./gradlew detekt          # Static analysis
./gradlew test            # All tests (existing + new)
```

## Key Design Decisions

- **Circuits use Instant.MAX for cache expiry** — indefinite cache without modifying CacheEntry
- **Circuits use a single "all" cache key** — no season dimension
- **validateSeason() extracted to shared file** — reused by drivers and teams routes
- **JolpicaClient implements all three data source interfaces** — single HTTP client, multiple fetch methods
- **No new dependencies** — everything built on existing libraries
