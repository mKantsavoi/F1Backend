# Research: Teams & Circuits Data Endpoints

**Date**: 2026-03-27

## Jolpica API — Constructors Endpoint

**Decision**: Use `GET /ergast/f1/{season}/constructors.json?limit=100` for teams data.

**Rationale**: Follows the same URL pattern as the drivers endpoint (`/ergast/f1/{season}/drivers.json?limit=100`). The Jolpica API wraps the Ergast F1 API with the same response structure. The `ConstructorTable` mirrors `DriverTable` with a `season` field and a `Constructors` array.

**Response structure**:
```json
{
  "MRData": {
    "ConstructorTable": {
      "season": "2026",
      "Constructors": [
        {
          "constructorId": "red_bull",
          "name": "Red Bull Racing",
          "nationality": "Austrian",
          "url": "https://en.wikipedia.org/wiki/Red_Bull_Racing"
        }
      ]
    }
  }
}
```

**Alternatives considered**: None — Jolpica is the only upstream data source used by this project.

## Jolpica API — Circuits Endpoint

**Decision**: Use `GET /ergast/f1/circuits.json?limit=100` for circuits data (no season prefix).

**Rationale**: Circuits are not season-scoped in the Jolpica API. The full historical catalog is returned. Using `limit=100` ensures all circuits are returned in a single request (there are approximately 77 circuits in the full catalog as of 2026).

**Response structure**:
```json
{
  "MRData": {
    "CircuitTable": {
      "Circuits": [
        {
          "circuitId": "monza",
          "circuitName": "Autodromo Nazionale di Monza",
          "url": "https://en.wikipedia.org/wiki/Monza_Circuit",
          "Location": {
            "lat": "45.6156",
            "long": "9.2811",
            "locality": "Monza",
            "country": "Italy"
          }
        }
      ]
    }
  }
}
```

**Key difference from constructors**: Circuit data includes a nested `Location` object with coordinates. The `lat` and `long` fields are strings in the API response and must be parsed to doubles in the domain model.

**Alternatives considered**: Fetching circuits per-season was considered but rejected — the spec explicitly calls for the full catalog, and season-filtered circuits would miss historical tracks not on the current calendar.

## Cache Strategy — Circuits

**Decision**: Use `Instant.MAX` as the `expiresAt` value for circuit cache entries, making them effectively indefinite.

**Rationale**: The existing `CacheEntry<T>` model uses `expiresAt: Instant` with `isFresh()` checking `Instant.now().isBefore(expiresAt)`. Setting `expiresAt = Instant.MAX` means `isFresh()` always returns `true`, achieving indefinite caching without modifying the `CacheEntry` class. This is the simplest approach — no new cache classes or TTL configuration needed.

**Alternatives considered**:
- Adding a `NeverExpiresCacheEntry` subclass — rejected as unnecessary complexity.
- Using a very large TTL (e.g., 10 years) — functionally equivalent but less explicit.
- Adding a nullable `expiresAt` — would require modifying existing code.

## Cache Interfaces — Generic vs Type-Specific

**Decision**: Create type-specific cache interfaces (`TeamCache`, `CircuitCache`) following the `DriverCache` pattern.

**Rationale**: The existing codebase uses `DriverCache` with `CacheEntry<List<Driver>>`. Creating `TeamCache` with `CacheEntry<List<Team>>` and `CircuitCache` with `CacheEntry<List<Circuit>>` maintains consistency and allows Koin to distinguish between cache bindings by type.

**Alternatives considered**:
- A generic `DataCache<T>` interface — would require refactoring existing `DriverCache` code, violating the "no changes to existing code" constraint.

## Circuits Use Case — Simplified Pattern

**Decision**: The `GetCircuits` use case uses a single cache key ("all") instead of per-season keys, and does not need season resolution logic.

**Rationale**: Circuits are not season-scoped. There is only one dataset to cache. The `resolvedSeasons` map and season-based mutex map from `GetDrivers` can be simplified to a single mutex and single cache key.

**Alternatives considered**: Using the same per-key structure with a fixed key — functionally equivalent but adds unnecessary complexity.

## JolpicaClient — Multiple Interface Implementation

**Decision**: `JolpicaClient` implements `DriverDataSource`, `TeamDataSource`, and `CircuitDataSource` on the same class.

**Rationale**: The client already manages the HTTP client lifecycle (CIO engine, retry config, timeouts). Creating separate client classes would duplicate this infrastructure. Implementing multiple interfaces on one class is clean and follows the existing pattern where `JolpicaClient` already implements `DriverDataSource` and `Closeable`.

**ClientModule update**: The existing `clientModule` binds `JolpicaClient` to `DriverDataSource`. It needs additional bindings for `TeamDataSource` and `CircuitDataSource` pointing to the same singleton.

**Alternatives considered**:
- Separate client classes per data type — rejected due to duplicated HTTP client setup and resource management.

## Season Validation — Reuse for Teams

**Decision**: Reuse the same `validateSeason()` logic from `DriverRoutes` for `TeamRoutes`.

**Rationale**: Teams use the same season parameter with the same constraints (year between 1950 and current year). Extract the validation to a shared utility or duplicate it (it's a small function).

**Decision on sharing**: Extract `validateSeason()` to a shared location in the adapter layer (e.g., a `RouteUtils.kt` or keep inline in each route file). Since it's a 5-line function, duplicating is acceptable and avoids coupling routes together. However, extracting avoids future drift.

**Final call**: Extract to a shared `RouteValidation.kt` file in the adapter/route package, since both driver and team routes need it and the logic is identical.
