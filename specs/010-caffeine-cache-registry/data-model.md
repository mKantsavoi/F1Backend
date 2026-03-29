# Data Model: Caffeine Cache Registry

**Feature**: 010-caffeine-cache-registry
**Date**: 2026-03-28

## Entities

### CacheSpec (Enum)

Central configuration for all application caches. Each entry defines a logical cache category.

| Field | Type | Description |
|-------|------|-------------|
| name | String (enum name) | Unique identifier for the cache category |
| ttl | Duration | Time-to-live for entries in this cache |
| maxSize | Long | Maximum number of entries before eviction |

**Entries**:

| Name | TTL | Max Size | Replaces |
|------|-----|----------|----------|
| SCHEDULE | 6 hours (default; overridden at runtime from JolpicaConfig.cacheTtlHours) | 50 | InMemoryScheduleCache |
| SCHEDULE_NEXT | 1 hour | 10 | InMemoryNextRaceCache |
| RACE_RESULTS | 5 minutes | 200 | InMemoryRaceResultCache (current season results) |
| RACE_RESULTS_HISTORICAL | 365 days | 500 | InMemoryRaceResultCache (past season results) |
| QUALIFYING | 5 minutes | 200 | InMemoryRaceResultCache (current season qualifying) |
| QUALIFYING_HISTORICAL | 365 days | 500 | InMemoryRaceResultCache (past season qualifying) |
| SPRINT | 5 minutes | 200 | InMemoryRaceResultCache (current season sprint) |
| SPRINT_HISTORICAL | 365 days | 500 | InMemoryRaceResultCache (past season sprint) |
| DRIVER_STANDINGS | 1 hour | 50 | InMemoryDriverStandingsCache (current season) |
| DRIVER_STANDINGS_HISTORICAL | 365 days | 500 | InMemoryDriverStandingsCache (past season) |
| CONSTRUCTOR_STANDINGS | 1 hour | 50 | InMemoryConstructorStandingsCache (current season) |
| CONSTRUCTOR_STANDINGS_HISTORICAL | 365 days | 500 | InMemoryConstructorStandingsCache (past season) |
| CIRCUITS | 365 days | 100 | InMemoryCircuitCache |
| PERSONALIZED_FEED | 30 seconds | 1000 | GetPersonalizedFeed internal ConcurrentHashMap |

**Note**: The data model defines 14 cache categories to accurately reflect the existing behavior where current-season and historical data have different TTLs, and per-user caches have their own TTL. The use cases will select the appropriate spec based on context (current vs historical season, per-user key, etc.).

**Note**: SCHEDULE TTL is currently sourced from `JolpicaConfig.cacheTtlHours` (default 24 hours). CacheSpec will hold a default, but the actual TTL can be overridden via CacheRegistry using the config value.

### CacheProvider (Port Interface)

Port interface in `adapter/port/` that use cases depend on. Defines the contract for cache access without exposing the underlying caching library.

| Method | Description |
|--------|-------------|
| `getCache<K, V>(spec)` | Returns a typed suspending cache for the given CacheSpec |
| `getOrLoad(spec, key, loader)` | Gets a cached value or loads it via the suspend loader, with stale-on-error fallback |
| `getFallback(spec, key)` | Returns the last successfully fetched value for stale-on-error scenarios |
| `putFallback(spec, key, value)` | Stores a value in the fallback map after a successful fetch |

### CacheRegistry (Infrastructure Implementation)

Implements CacheProvider. Manages all Caffeine cache instances. Singleton registered in Koin, bound to CacheProvider via `bind` operator.

| Responsibility | Description |
|---------------|-------------|
| Cache creation | Creates typed Aedile caches on demand, keyed by CacheSpec |
| Fallback storage | Maintains per-spec `ConcurrentHashMap` backup maps for stale-on-error |
| Statistics access | Exposes per-spec `CacheStats` from underlying Caffeine instances |
| Cache retrieval | Provides typed cache access via `getCache<K, V>(spec)` |

## Relationships

```
CacheSpec (enum) --[configures]--> CacheRegistry --[creates]--> Aedile Cache instances
CacheRegistry --[implements]--> CacheProvider (port interface)
CacheRegistry --[maintains]--> Fallback backup maps (one per CacheSpec)
Use Cases --[inject]--> CacheProvider --[provides]--> typed caches
```

## Entities Removed

| Entity | Location | Reason |
|--------|----------|--------|
| CacheEntry<T> | domain/model/CacheEntry.kt | Caffeine manages TTL internally |
| SeasonCache<T> | domain/model/SeasonCache.kt | Review needed: may be preserved if used for response building beyond caching |
| CircuitCache | adapter/port/CircuitCache.kt | Replaced by CacheRegistry |
| ScheduleCache | adapter/port/ScheduleCache.kt | Replaced by CacheRegistry |
| DriverStandingsCache | adapter/port/DriverStandingsCache.kt | Replaced by CacheRegistry |
| ConstructorStandingsCache | adapter/port/ConstructorStandingsCache.kt | Replaced by CacheRegistry |
| NextRaceCache | adapter/port/NextRaceCache.kt | Replaced by CacheRegistry |
| RaceResultCache | adapter/port/RaceResultCache.kt | Replaced by CacheRegistry |
| InMemoryCircuitCache | infrastructure/cache/InMemoryCircuitCache.kt | Replaced by Caffeine |
| InMemoryScheduleCache | infrastructure/cache/InMemoryScheduleCache.kt | Replaced by Caffeine |
| InMemoryDriverStandingsCache | infrastructure/cache/InMemoryDriverStandingsCache.kt | Replaced by Caffeine |
| InMemoryConstructorStandingsCache | infrastructure/cache/InMemoryConstructorStandingsCache.kt | Replaced by Caffeine |
| InMemoryNextRaceCache | infrastructure/cache/InMemoryNextRaceCache.kt | Replaced by Caffeine |
| InMemoryRaceResultCache | infrastructure/cache/InMemoryRaceResultCache.kt | Replaced by Caffeine |
