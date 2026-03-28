# Data Model: Championship Standings Endpoints

**Date**: 2026-03-27
**Feature**: 007-championship-standings

## Domain Models (domain/model/)

### DriverStanding

Represents a single driver's position in the championship for a given season.

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| position | Int | Yes | Championship position (1-based) |
| driverId | String | Yes | Jolpica driver identifier, e.g., "max_verstappen" |
| driverCode | String | Yes | 3-letter code, e.g., "VER" |
| driverName | String | Yes | Full name, e.g., "Max Verstappen" |
| nationality | String | Yes | e.g., "Dutch" |
| teamId | String | Yes | Jolpica constructor identifier, e.g., "red_bull" |
| teamName | String | Yes | e.g., "Red Bull Racing" |
| points | Double | Yes | Total championship points (can be fractional from sprint) |
| wins | Int | Yes | Number of race wins |

### ConstructorStanding

Represents a team's position in the championship for a given season.

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| position | Int | Yes | Championship position (1-based) |
| teamId | String | Yes | Jolpica constructor identifier |
| teamName | String | Yes | e.g., "Red Bull Racing" |
| nationality | String | Yes | e.g., "Austrian" |
| points | Double | Yes | Total championship points |
| wins | Int | Yes | Number of race wins |

### StandingsData<T>

Wrapper for standings response data. Similar to `SeasonCache<T>` but includes the round number.

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| season | String | Yes | Resolved season year, e.g., "2026" |
| round | Int | Yes | Round through which standings are current |
| standings | List<T> | Yes | Ordered list of standings entries |

## Reused Domain Models

### CacheEntry<T> (existing)

No changes needed. Used to wrap all cached data with `fetchedAt` and `expiresAt` timestamps.

### SeasonCache<T> (existing)

Not reused for standings — `StandingsData<T>` is used instead because standings include a `round` field that `SeasonCache<T>` does not have.

## Cache Interfaces (adapter/port/)

### DriverStandingsCache

Keyed by season string (same pattern as DriverCache/TeamCache).

```
get(season: String) → CacheEntry<StandingsData<DriverStanding>>?
put(season: String, entry: CacheEntry<StandingsData<DriverStanding>>)
```

### ConstructorStandingsCache

Keyed by season string (same pattern as DriverCache/TeamCache).

```
get(season: String) → CacheEntry<StandingsData<ConstructorStanding>>?
put(season: String, entry: CacheEntry<StandingsData<ConstructorStanding>>)
```

## Domain Port Interface (domain/port/)

### StandingsDataSource

```
fetchDriverStandings(season: String) → StandingsData<DriverStanding>
fetchConstructorStandings(season: String) → StandingsData<ConstructorStanding>
```

Both methods accept a season string (the raw query parameter value or "current") and return the standings with the resolved season and round.

## Jolpica Response Models (infrastructure/external/)

### New models to add to JolpicaModels.kt

```
JolpicaDriverStandingsResponse → MRData → StandingsTable → StandingsLists[]
  └── StandingsList: season, round, DriverStandings[]
       └── DriverStanding: position, points, wins, Driver, Constructors[]
```

```
JolpicaConstructorStandingsResponse → MRData → StandingsTable → StandingsLists[]
  └── StandingsList: season, round, ConstructorStandings[]
       └── ConstructorStanding: position, points, wins, Constructor
```

## Entity Relationships

```
Championship Standings (1 season)
  ├── Driver Standings (N drivers per season)
  │    └── Team affiliation (1 per driver per season)
  └── Constructor Standings (N teams per season)
```
