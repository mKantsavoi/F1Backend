# Research: 008-drivers-db-detail

**Date**: 2026-03-28

## R1: Biography Source

**Decision**: Use a bundled JSON file (`driver-biographies.json`) with pre-written biographies instead of an external AI service.

**Rationale**: No API key dependency, no external service failure mode, no new SDK dependency. Biographies are user-curated and checked into the repository. The file uses the format `{ "drivers": [{ "driverId": "...", "teamId": "...", "biography": "..." }] }`. Only the `driverId` and `biography` fields are used by the seed; `teamId` in this file is ignored (team assignments come from Jolpica).

**Alternatives considered**:
- Anthropic Claude API (anthropic-sdk-java): Would require API key, adds external dependency and failure mode during seed. Rejected — user does not have API key, bundled file is simpler and more reliable.

**No new dependencies required.**

## R2: Exposed ORM JOIN Pattern for Driver-Team Resolution

**Decision**: Use Exposed DSL `leftJoin` between DriversTable and TeamsTable, selecting columns from both tables.

**Rationale**: Exposed supports `innerJoin`/`leftJoin` directly on foreign key references. The pattern is concise and type-safe. Use `leftJoin` to ensure drivers without a team assignment are still returned.

**Pattern**:
```kotlin
(DriversTable leftJoin TeamsTable)
    .selectAll()
    .where { DriversTable.driverId eq driverId }
    .map { row ->
        // Map driver fields from DriversTable columns
        // Map team fields from TeamsTable columns (nullable if leftJoin)
    }
```

**Alternatives considered**:
- Separate queries (fetch driver, then fetch team): N+1 for list endpoint. Rejected.
- Exposed DAO entities: Heavier than DSL approach. Project already uses DSL pattern for all tables. Rejected.

## R3: Team ID Mapping Strategy

**Decision**: Fetch driver-team relationships from Jolpica's constructor standings or season constructors endpoint. Each driver entry from Jolpica includes the constructor they race for, providing canonical `constructorId` values.

**Rationale**: The Jolpica API endpoint `/ergast/f1/current/constructors.json` returns teams with their canonical IDs (e.g., "red_bull", "mercedes"). The driver standings endpoint also links drivers to constructors. This avoids maintaining a separate mapping between photo slugs and team IDs.

**Implementation**: The seed service fetches teams from Jolpica (via existing `TeamDataSource`), inserts into TeamsTable, then fetches driver-constructor relationships to set `team_id` on each driver. The Jolpica API response for `/ergast/f1/current/driverStandings.json` includes both driver and constructor data per entry.

**Alternatives considered**:
- Bundled slug-to-ID mapping file: Fragile, needs manual maintenance. Rejected.
- Derive from photo mapping: Photo slugs don't match Jolpica IDs. Rejected.

## R4: Seed Orchestration Order

**Decision**: Seed teams first, then drivers. Teams must exist before drivers can reference them.

**Rationale**: Drivers table has a team_id that references teams. The seed must:
1. Fetch and insert teams from Jolpica
2. Fetch drivers from Jolpica (with constructor info from standings endpoint)
3. Enrich drivers with photos from bundled photo mapping
4. Enrich drivers with biographies from bundled biographies file
5. Insert drivers with team_id references

This ensures referential integrity and allows the driver seed to validate team_id values exist.
