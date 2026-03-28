# Research: Championship Standings Endpoints

**Date**: 2026-03-27
**Feature**: 007-championship-standings

## No NEEDS CLARIFICATION Items

All technical context was resolved from the existing codebase. No unknowns required external research. This feature follows patterns established in 6 prior Jolpica integrations.

## Decision Log

### D-001: Two Separate Cache Interfaces vs One Generic Cache

**Decision**: Use two separate cache interfaces (`DriverStandingsCache`, `ConstructorStandingsCache`), each keyed by season string, following the same pattern as `DriverCache` and `TeamCache`.

**Rationale**: Driver and constructor standings have different data shapes. Using the same keyed-by-season pattern as existing caches (DriverCache, TeamCache, ScheduleCache) maintains consistency and full type safety. Each cache stores `CacheEntry<StandingsData<T>>` where T differs.

**Alternatives considered**:
- Single generic cache with prefixed keys (like RaceResultCache) — rejected: standings are keyed only by season (no round), so the simpler per-type pattern is appropriate. The generic approach was designed for results/qualifying/sprint which share the same key shape.

### D-002: Single StandingsDataSource vs Two Separate Interfaces

**Decision**: Create a single `StandingsDataSource` interface with two methods (`fetchDriverStandings`, `fetchConstructorStandings`), implemented by `JolpicaStandingsClient`.

**Rationale**: Both methods interact with the same upstream API and share the same client infrastructure. Follows the pattern of `RaceDataSource` which groups multiple related fetch methods. Avoids creating two nearly-identical single-method interfaces.

**Alternatives considered**:
- Two separate interfaces (DriverStandingsDataSource, ConstructorStandingsDataSource) — rejected: excessive granularity when both methods share the same client

### D-003: Cache TTL Strategy

**Decision**: 1-hour TTL for current season (compare against `Year.now().value.toString()`), `Instant.MAX` for past seasons. Identical logic to the existing use cases.

**Rationale**: Standings update after each race, so 1 hour balances freshness with upstream load. Past seasons are immutable. This mirrors the established pattern.

### D-004: Standings Data Wrapper

**Decision**: Create a `StandingsData<T>` wrapper (similar to `SeasonCache<T>` but including `round`) to hold the resolved season, round number, and list of standings entries.

**Rationale**: The standings response includes both season and round (indicating how many races are included in the standings). `SeasonCache<T>` only stores `resolvedSeason` and `items`. Adding a `round` field requires either extending `SeasonCache` (breaking existing usages) or creating a new wrapper. A new wrapper is cleaner.

**Alternatives considered**:
- Extend SeasonCache with optional round — rejected: modifies shared model used by 4+ other features
- Store round separately outside cache — rejected: couples cache and use case state unnecessarily

### D-005: One Koin Module for Both Standings

**Decision**: Create a single `StandingsModule` that registers both driver and constructor standings caches and use cases.

**Rationale**: Both standings endpoints are logically related (championship standings) and will always be deployed together. One module reduces boilerplate while keeping separation from other feature modules (drivers, teams, races, etc.).

### D-006: JolpicaStandingsClient — New Client File

**Decision**: Create a new `JolpicaStandingsClient` class rather than adding methods to an existing client.

**Rationale**: Follows the established pattern where each feature domain has its own client class (`JolpicaDriverClient`, `JolpicaRaceClient`, `JolpicaScheduleClient`). Keeps files focused and avoids bloating existing clients.

### D-007: Route Structure — /standings/drivers and /standings/constructors

**Decision**: Use query parameter `?season=` (optional, defaults to current) on flat routes `/standings/drivers` and `/standings/constructors`.

**Rationale**: Follows the same pattern as `/schedule?season=` and `/drivers?season=`. Season is a filter, not a resource identifier, so query parameter is the correct REST choice.
