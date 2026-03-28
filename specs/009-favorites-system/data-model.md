# Data Model: Favorites System

**Feature**: 009-favorites-system
**Date**: 2026-03-28

## Entities

### FavoriteDriver (Junction)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Internal row identifier |
| userId | UUID | NOT NULL, FK → users(id) ON DELETE CASCADE | Owner of this favorite |
| driverId | VARCHAR(64) | NOT NULL | Driver identifier (e.g., "max_verstappen") |
| createdAt | TIMESTAMP | NOT NULL, default now() | When the favorite was added |

**Unique constraint**: `(userId, driverId)` -- prevents duplicate favorites.
**Index**: `idx_fav_drivers_user` on `userId` for efficient per-user queries.

### FavoriteTeam (Junction)

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | UUID | PK, auto-generated | Internal row identifier |
| userId | UUID | NOT NULL, FK → users(id) ON DELETE CASCADE | Owner of this favorite |
| teamId | VARCHAR(64) | NOT NULL | Team identifier (e.g., "red_bull") |
| createdAt | TIMESTAMP | NOT NULL, default now() | When the favorite was added |

**Unique constraint**: `(userId, teamId)` -- prevents duplicate favorites.
**Index**: `idx_fav_teams_user` on `userId` for efficient per-user queries.

## Relationships

```
users (1) ──── (*) favorite_drivers ────> drivers (validated at app level, not FK)
users (1) ──── (*) favorite_teams   ────> teams   (validated at app level, not FK)
```

- User deletion cascades to remove all favorites (DB-level CASCADE).
- Driver/team existence validated at application level before INSERT (return 404 if not found).
- No FK to drivers/teams tables to avoid coupling with seed data refresh cycles.

## Exposed DSL Tables

### FavoriteDriversTable

```kotlin
object FavoriteDriversTable : Table("favorite_drivers") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val driverId = varchar("driver_id", 64)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(userId, driverId)
        index(false, userId)
    }
}
```

### FavoriteTeamsTable

```kotlin
object FavoriteTeamsTable : Table("favorite_teams") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val teamId = varchar("team_id", 64)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(userId, teamId)
        index(false, userId)
    }
}
```

## Repository Interface

```kotlin
interface FavoriteRepository {
    // Driver favorites
    suspend fun addFavoriteDriver(userId: UUID, driverId: String): Boolean  // true = new, false = existed
    suspend fun removeFavoriteDriver(userId: UUID, driverId: String)
    suspend fun getFavoriteDriverIds(userId: UUID): List<Pair<String, Instant>>  // driverId + createdAt
    suspend fun isDriverFavorite(userId: UUID, driverId: String): Boolean

    // Team favorites
    suspend fun addFavoriteTeam(userId: UUID, teamId: String): Boolean
    suspend fun removeFavoriteTeam(userId: UUID, teamId: String)
    suspend fun getFavoriteTeamIds(userId: UUID): List<Pair<String, Instant>>
    suspend fun isTeamFavorite(userId: UUID, teamId: String): Boolean
}
```

## State Transitions

Favorites have only two states:
- **Not favorited** → `addFavorite*()` → **Favorited** (201 Created)
- **Favorited** → `removeFavorite*()` → **Not favorited** (204 No Content)

Both transitions are idempotent:
- Adding an already-favorited entity → 200 OK (no-op)
- Removing a non-favorited entity → 204 No Content (no-op)
