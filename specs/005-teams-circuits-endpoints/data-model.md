# Data Model: Teams & Circuits Data Endpoints

**Date**: 2026-03-27

## Domain Entities

### Team

Represents an F1 constructor for a specific season.

| Field       | Type   | Description                                      |
|-------------|--------|--------------------------------------------------|
| id          | String | Stable identifier from upstream (e.g., "red_bull") |
| name        | String | Constructor display name (e.g., "Red Bull Racing") |
| nationality | String | Constructor nationality (e.g., "Austrian")        |

**Notes**:
- No state transitions — read-only data fetched from upstream.
- Identity: `id` is unique within a season context.
- Scoped to a season, but the season is not part of the entity itself (it's metadata on the response envelope).

### Circuit

Represents an F1 racing circuit (historical catalog).

| Field    | Type   | Description                                            |
|----------|--------|--------------------------------------------------------|
| id       | String | Stable identifier from upstream (e.g., "monza")        |
| name     | String | Circuit display name (e.g., "Autodromo Nazionale di Monza") |
| locality | String | City where the circuit is located (e.g., "Monza")      |
| country  | String | Country where the circuit is located (e.g., "Italy")   |
| lat      | Double | Latitude coordinate                                    |
| lng      | Double | Longitude coordinate                                   |
| url      | String | Wikipedia URL for the circuit                           |

**Notes**:
- No state transitions — read-only, essentially static data.
- Identity: `id` is globally unique across the full catalog.
- Not season-scoped.

## Cache Entries

Both entities are cached using the existing `CacheEntry<T>` wrapper:

```
CacheEntry<List<Team>>   — keyed by season string, 24h TTL
CacheEntry<List<Circuit>> — keyed by "all", indefinite TTL (expiresAt = Instant.MAX)
```

## Upstream Data Mapping

### Jolpica Constructor → Team

| Jolpica Field    | Domain Field  | Transformation |
|------------------|---------------|----------------|
| constructorId    | id            | Direct mapping |
| name             | name          | Direct mapping |
| nationality      | nationality   | Direct mapping |

### Jolpica Circuit → Circuit

| Jolpica Field          | Domain Field | Transformation                |
|------------------------|--------------|-------------------------------|
| circuitId              | id           | Direct mapping                |
| circuitName            | name         | Direct mapping                |
| Location.locality      | locality     | Unnest from Location object   |
| Location.country       | country      | Unnest from Location object   |
| Location.lat           | lat          | Parse String → Double         |
| Location.long          | lng          | Parse String → Double         |
| url                    | url          | Direct mapping                |

## Relationships

- **Team** has no direct relationship to other domain entities in this feature.
- **Circuit** has no direct relationship to other domain entities in this feature.
- Both entities exist independently of the `Driver` entity from the existing feature.
- Future features (race results, standings) may link teams and circuits to other entities, but that is out of scope.
