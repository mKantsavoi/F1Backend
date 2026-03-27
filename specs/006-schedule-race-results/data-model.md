# Data Model: Schedule, Race Results & Qualifying Endpoints

**Date**: 2026-03-27
**Feature**: 006-schedule-race-results

## Domain Models (domain/model/)

### RaceWeekend

Represents a single Grand Prix event in the season schedule.

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| round | Int | Yes | 1-based race number in the season |
| raceName | String | Yes | e.g., "Bahrain Grand Prix" |
| circuitId | String | Yes | Jolpica circuit identifier |
| circuitName | String | Yes | e.g., "Bahrain International Circuit" |
| country | String | Yes | e.g., "Bahrain" |
| date | String | Yes | Race date, e.g., "2026-03-15" |
| time | String? | No | Race time UTC, e.g., "15:00:00Z" |
| sessions | Sessions | Yes | All session times |

### Sessions

Value object for session times within a race weekend.

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| fp1 | String? | No | ISO 8601 datetime or null if not scheduled |
| fp2 | String? | No | |
| fp3 | String? | No | |
| qualifying | String? | No | |
| sprint | String? | No | Only present for sprint weekends |
| race | String? | No | |

### RaceResult

Represents a single driver's finishing position in a race or sprint.

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| position | Int | Yes | Finishing position (1-based) |
| driverId | String | Yes | Jolpica driver identifier |
| driverCode | String | Yes | 3-letter code, e.g., "VER" |
| driverName | String | Yes | Full name, e.g., "Max Verstappen" |
| teamId | String | Yes | Jolpica constructor identifier |
| teamName | String | Yes | e.g., "Red Bull Racing" |
| grid | Int | Yes | Starting grid position |
| laps | Int | Yes | Laps completed |
| time | String? | No | Finishing time (null for DNF/DNS) |
| points | Double | Yes | Points awarded (can be fractional for sprint) |
| status | String | Yes | e.g., "Finished", "+1 Lap", "Collision" |
| fastestLap | FastestLap? | No | Only for drivers with recorded fastest lap |

### FastestLap

Value object for fastest lap details.

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| rank | Int | Yes | Fastest lap ranking across all drivers |
| lap | Int | Yes | Lap number |
| time | String | Yes | Lap time, e.g., "1:32.608" |
| avgSpeed | String | Yes | Average speed in km/h, e.g., "206.018" |

### QualifyingResult

Represents a single driver's qualifying outcome.

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| position | Int | Yes | Final qualifying position |
| driverId | String | Yes | |
| driverCode | String | Yes | |
| driverName | String | Yes | |
| teamId | String | Yes | |
| teamName | String | Yes | |
| q1 | String? | No | Q1 time (null if no time set) |
| q2 | String? | No | Q2 time (null if eliminated in Q1) |
| q3 | String? | No | Q3 time (null if eliminated in Q2) |

## Reused Domain Models

### CacheEntry<T> (existing)

No changes needed. Used to wrap all cached data with `fetchedAt` and `expiresAt` timestamps.

### SeasonCache<T> (existing)

No changes needed. Used for schedule data: `SeasonCache<RaceWeekend>` stores the resolved season + list of race weekends.

## Cache Interfaces (adapter/port/)

### ScheduleCache

Keyed by season string (same pattern as DriverCache/TeamCache).

```
get(season: String) → CacheEntry<SeasonCache<RaceWeekend>>?
put(season: String, entry: CacheEntry<SeasonCache<RaceWeekend>>)
```

### NextRaceCache

Single-value cache (same pattern as CircuitCache).

```
get() → CacheEntry<RaceWeekend>?
put(entry: CacheEntry<RaceWeekend>)
```

### RaceResultCache

Generic keyed cache for results, qualifying, and sprint. Key format: `"type:season:round"`.

```
get(key: String) → CacheEntry<*>?
put(key: String, entry: CacheEntry<*>)
```

Note: Type safety is maintained at the use case level. Each use case knows what type it stores/retrieves. The cache is a simple key-value store.

## Domain Port Interfaces (domain/port/)

### ScheduleDataSource

```
fetchSchedule(season: String) → Pair<String, List<RaceWeekend>>   // resolvedSeason to races
fetchNextRace() → RaceWeekend?                                      // null if season ended
```

### RaceDataSource

```
fetchRaceResults(season: String, round: Int) → RaceResultsData
fetchQualifyingResults(season: String, round: Int) → QualifyingResultsData
fetchSprintResults(season: String, round: Int) → SprintResultsData?  // null = no sprint
```

Where result data types wrap the metadata (season, round, raceName) + list of results.

## New Exception

### NotFoundException

Added to `Exceptions.kt`. Maps to HTTP 404 via StatusPages.

```
class NotFoundException(message: String) : RuntimeException(message)
```

## Entity Relationships

```
Season Schedule (1 season)
  └── Race Weekend (N per season)
       ├── Race Results (N drivers per race)
       │    └── FastestLap (0..1 per driver)
       ├── Qualifying Results (N drivers per qualifying)
       └── Sprint Results (0..N drivers, only sprint weekends)
```
