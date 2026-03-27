# Data Model: F1 Drivers Endpoint

**Feature**: 002-f1-drivers-endpoint
**Date**: 2026-03-27

## Domain Entities

### Driver

Pure Kotlin domain entity. No framework annotations.

| Field | Type | Description | Source (Jolpica) |
|-------|------|-------------|------------------|
| id | String | Stable identifier (e.g., "max_verstappen") | `driverId` |
| number | Int | Permanent race number | `permanentNumber` (String → Int) |
| code | String | Three-letter driver code (e.g., "VER") | `code` |
| firstName | String | Given name | `givenName` |
| lastName | String | Family name | `familyName` |
| nationality | String | Driver nationality | `nationality` |
| dateOfBirth | LocalDate | Birth date | `dateOfBirth` (String → LocalDate) |

**Identity**: `id` (the Jolpica `driverId`) is the stable identifier across seasons.

**Uniqueness**: Within a season, each driver has a unique `id`, unique `number`, and unique `code`.

## Value Objects

### CacheEntry\<T\>

Generic cache wrapper for any cached data type (reusable across future endpoints).

| Field | Type | Description |
|-------|------|-------------|
| data | T | The cached payload |
| fetchedAt | Instant | When the data was fetched from external source |
| expiresAt | Instant | When this entry should be considered stale |

**State transitions**: Fresh → Stale (when `Instant.now() > expiresAt`). Stale entries are still served as fallback when the external source is unavailable.

## External API Models (Jolpica)

### JolpicaResponse

Represents the top-level Jolpica API response. Used only in the infrastructure layer for deserialization.

```
JolpicaResponse
└── MRData
    └── DriverTable
        ├── season: String
        └── Drivers: List<JolpicaDriver>
            ├── driverId: String
            ├── permanentNumber: String
            ├── code: String
            ├── givenName: String
            ├── familyName: String
            ├── dateOfBirth: String
            ├── nationality: String
            └── url: String (ignored in mapping)
```

## Response DTOs (Adapter Layer)

### DriversResponse

| Field | Type | Description |
|-------|------|-------------|
| season | String | The season year (e.g., "2026") |
| drivers | List\<DriverDto\> | Array of driver entries |

### DriverDto

| Field | Type | Description |
|-------|------|-------------|
| id | String | Stable driver identifier |
| number | Int | Permanent race number |
| code | String | Three-letter code |
| firstName | String | Given name |
| lastName | String | Family name |
| nationality | String | Nationality |
| dateOfBirth | String | ISO 8601 date (e.g., "1997-09-30") |

## Interfaces (Ports)

### DriverDataSource

```
interface DriverDataSource {
    suspend fun fetchDrivers(season: String): List<Driver>
}
```

Implemented by `JolpicaClient` in infrastructure layer. The `season` parameter is either a year string (e.g., "2024") or "current".

### DriverCache

```
interface DriverCache {
    fun get(season: String): CacheEntry<List<Driver>>?
    fun put(season: String, entry: CacheEntry<List<Driver>>)
}
```

Implemented by `InMemoryDriverCache` in infrastructure layer.

## Relationships

```
DriverRoutes (adapter) → GetDrivers (use-case) → DriverCache (port)
                                                → DriverDataSource (port)
                                                        ↑
                                                  JolpicaClient (infrastructure)
                                                  InMemoryDriverCache (infrastructure)
```

The use-case layer depends only on domain interfaces. The adapter layer maps between DTOs and domain models. The infrastructure layer implements the ports.
