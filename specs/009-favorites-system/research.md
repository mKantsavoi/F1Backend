# Research: Favorites System

**Feature**: 009-favorites-system
**Date**: 2026-03-28

## Findings

### 1. Idempotent INSERT Pattern with Exposed

- **Decision**: Use Exposed's `insertIgnore` or raw `INSERT ... ON CONFLICT DO NOTHING` for idempotent add operations. Return whether a row was actually inserted (created=true) vs already existed (created=false) to distinguish 201 vs 200 status codes.
- **Rationale**: The `UNIQUE(user_id, driver_id)` constraint prevents duplicates at DB level. `insertIgnore` returns the inserted row count, allowing the use case to determine if a new row was created.
- **Alternatives considered**: Application-level check-then-insert (rejected: race condition prone), `UPSERT` with `ON CONFLICT DO UPDATE` (rejected: no fields to update).

### 2. JWT User ID Extraction Pattern

- **Decision**: Reuse existing pattern: `UUID.fromString(call.principal<JWTPrincipal>()!!.subject)`. No `call.userId()` extension exists in the codebase.
- **Rationale**: Consistent with AuthRoutes.kt pattern. The `authenticate` block guarantees a non-null principal.
- **Alternatives considered**: Creating a `call.userId()` extension (rejected: unnecessary abstraction for this scope; existing pattern is clear and used consistently).

### 3. Feed Aggregation Strategy

- **Decision**: The `GetPersonalizedFeed` use case directly reads from existing `DriverStandingsCache`, `ConstructorStandingsCache`, and `GetRaceResults` use case (or its cache). It queries the favorites repository for user's favorite IDs, then filters cached standings data.
- **Rationale**: Standings and race results are already cached in-memory with their own TTL. No need for a separate feed cache -- the source caches already handle freshness. The feed use case simply filters and combines.
- **Alternatives considered**: Per-user feed cache with 30s TTL (rejected for Phase 1: adds complexity; source data is already cached; can add if latency becomes an issue). Calling the existing use cases (GetDriverStandings, GetRaceResults) instead of reading caches directly (preferred: reuses existing fetch+cache logic).

### 4. Last Race Results for Feed

- **Decision**: The feed calls `GetRaceResults.execute("current", latestRound)` to get the most recent race results. The latest round can be derived from the schedule cache or from the standings data (which includes the round number).
- **Rationale**: Reuses existing use case which handles caching, retries, and stale data fallback.
- **Alternatives considered**: Storing last race result per driver in a separate table (rejected: duplicates cached data).

### 5. Foreign Key Strategy for Favorites Tables

- **Decision**: The `favorite_drivers` and `favorite_teams` tables use `user_id UUID REFERENCES users(id) ON DELETE CASCADE` for user FK, but `driver_id VARCHAR(64)` and `team_id VARCHAR(64)` are NOT foreign keys to the drivers/teams tables.
- **Rationale**: Drivers and teams are seeded from external API (Jolpica) and may be refreshed. Adding FK constraints to volatile seed data creates migration complexity. Instead, validate existence at the application level (check driver/team exists in DB before INSERT). This matches the spec's "return 404 if driver doesn't exist" requirement.
- **Alternatives considered**: Full FK to DriversTable/TeamsTable (rejected: seed refresh could violate FK constraints; would require complex cascade/update logic).

### 6. Table Registration in DatabaseFactory

- **Decision**: Add `FavoriteDriversTable` and `FavoriteTeamsTable` to `SchemaUtils.create()` in `DatabaseFactory.init()`.
- **Rationale**: Follows existing pattern where all tables are created on startup.
- **Alternatives considered**: Database migrations (not currently used in project; would be a new pattern).

### 7. Repository Design: Single vs Separate

- **Decision**: Single `FavoriteRepository` interface with methods for both drivers and teams (e.g., `addFavoriteDriver`, `addFavoriteTeam`, `getFavoriteDriverIds`, `getFavoriteTeamIds`).
- **Rationale**: Both favorites types share identical structure and behavior. A single repository reduces file count without sacrificing clarity. The methods are clearly named to distinguish driver vs team operations.
- **Alternatives considered**: Separate `FavoriteDriverRepository` + `FavoriteTeamRepository` interfaces (rejected: would create 4 files for nearly identical code).
