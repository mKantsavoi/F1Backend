# Data Model: 008-drivers-db-detail

**Date**: 2026-03-28

## Entities

### Team

Persisted entity representing an F1 constructor/team.

| Field       | Type         | Constraints                  | Notes                                    |
|-------------|--------------|------------------------------|------------------------------------------|
| id          | UUID         | PK, auto-generated           | Internal surrogate key                   |
| teamId      | VARCHAR(64)  | UNIQUE, NOT NULL             | Jolpica canonical ID (e.g., "red_bull")  |
| name        | VARCHAR(200) | NOT NULL                     | Display name (e.g., "Red Bull Racing")   |
| nationality | VARCHAR(100) |                              | Team nationality                         |
| createdAt   | TIMESTAMP    | NOT NULL, default now()      | Record creation timestamp                |
| updatedAt   | TIMESTAMP    | NOT NULL, default now()      | Last update timestamp                    |

**Identity**: `teamId` is the unique business key (from Jolpica).

### Driver

Persisted entity representing an F1 driver with enrichment data.

| Field       | Type         | Constraints                  | Notes                                           |
|-------------|--------------|------------------------------|-------------------------------------------------|
| id          | UUID         | PK, auto-generated           | Internal surrogate key                          |
| driverId    | VARCHAR(64)  | UNIQUE, NOT NULL             | Jolpica canonical ID (e.g., "max_verstappen")   |
| number      | INTEGER      | nullable                     | Permanent racing number (null for some drivers) |
| code        | VARCHAR(3)   |                              | Three-letter abbreviation (e.g., "VER")         |
| firstName   | VARCHAR(100) | NOT NULL                     | Driver's given name                             |
| lastName    | VARCHAR(100) | NOT NULL                     | Driver's family name                            |
| nationality | VARCHAR(100) |                              | Driver's nationality                            |
| dateOfBirth | DATE         | nullable                     | ISO date                                        |
| photoUrl    | VARCHAR(500) | nullable                     | formula1.com CDN URL, set during seed           |
| teamId      | VARCHAR(64)  | nullable, FK → teams.teamId  | References Team entity                          |
| biography   | TEXT         | nullable                     | AI-generated bio, set during seed               |
| createdAt   | TIMESTAMP    | NOT NULL, default now()      | Record creation timestamp                       |
| updatedAt   | TIMESTAMP    | NOT NULL, default now()      | Last update timestamp                           |

**Identity**: `driverId` is the unique business key (from Jolpica).

**Relationships**:
- Driver → Team: Many-to-one via `teamId`. Resolved via LEFT JOIN (driver may not have a team assigned).

### Photo Mapping (bundled resource, not persisted)

Static JSON configuration at `src/main/resources/seed/driver-photos.json`.

| Field | Type   | Notes                                              |
|-------|--------|----------------------------------------------------|
| key   | String | Jolpica driverId                                   |
| team  | String | formula1.com team slug (e.g., "redbullracing")     |
| code  | String | formula1.com driver code (e.g., "maxver01")        |

**URL template**: `https://media.formula1.com/image/upload/c_fill,w_720/q_auto/v1740000001/common/f1/2026/{team}/{code}/2026{team}{code}right.webp`

## State Transitions

### Seed Lifecycle

```
Application Start
  → Check: drivers table empty?
    → YES: Run full seed
      1. Fetch teams from Jolpica → INSERT into teams table
      2. Fetch drivers + constructor assignments from Jolpica
      3. Enrich with photo URLs from bundled mapping
      4. Enrich with biographies from Claude API (optional)
      5. INSERT into drivers table
    → NO: Skip seed
```

No ongoing state transitions — data is static after seed. Future re-seed (UPSERT) is out of scope for this feature.

## Validation Rules

- `driverId`: Non-empty, max 64 characters, unique
- `teamId` (on teams): Non-empty, max 64 characters, unique
- `number`: Positive integer or null
- `code`: Exactly 3 uppercase letters or null
- `firstName`, `lastName`: Non-empty
- `photoUrl`: Valid URL format or null (not validated at DB level)
- `biography`: Free text or null
