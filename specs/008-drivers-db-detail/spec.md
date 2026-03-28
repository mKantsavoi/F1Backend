# Feature Specification: Driver Persistence & Detail Endpoint

**Feature Branch**: `008-drivers-db-detail`
**Created**: 2026-03-28
**Status**: Draft
**Input**: User description: "Create a persistent data layer for drivers in PostgreSQL and a detail endpoint that returns a full driver card by ID. Currently driver data lives only in memory cache from Jolpica. This task stores drivers in the database with photos from formula1.com and AI-generated biographies, switches the list endpoint to read from DB, and adds a detail endpoint."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Driver List from Database (Priority: P1)

A user opens the drivers section of the app. The system returns the list of all current-season drivers. The data comes from the local database rather than an external API, so the response is fast and reliable regardless of external service availability.

**Why this priority**: This is the foundation — the existing list endpoint must continue working identically from the user's perspective. Without database persistence, all other stories (detail cards, enrichment) have no data to serve.

**Independent Test**: Can be fully tested by calling the driver list endpoint and verifying the same response format and data as before, with drivers now sourced from the database.

**Acceptance Scenarios**:

1. **Given** the database contains seeded driver data, **When** an authenticated user requests the driver list, **Then** the system returns all drivers with the existing fields (season, list of drivers with id, number, code, firstName, lastName, nationality, dateOfBirth) plus the new photoUrl field.
2. **Given** the database contains driver data, **When** the external Jolpica API is completely unreachable, **Then** the driver list still returns successfully because it reads from the local database.
3. **Given** an unauthenticated user, **When** they request the driver list, **Then** the system returns a 401 Unauthorized response.

---

### User Story 2 - Database Seeding on First Startup (Priority: P1)

When the application starts for the first time with an empty database, the system automatically populates the drivers table with base data from the external F1 data provider, photo URLs from a bundled mapping file, and AI-generated biographies. This is a one-time operation that makes drivers available for all subsequent requests.

**Why this priority**: Equal to P1 because without seeding, the database is empty and no driver endpoints work. This is a prerequisite for all read operations.

**Independent Test**: Can be tested by starting the application with an empty drivers table and verifying that all current-season drivers are populated with base fields, photo URLs where available, and biographies where the AI service is accessible.

**Acceptance Scenarios**:

1. **Given** the drivers table is empty, **When** the application starts, **Then** the system fetches base driver data from the external F1 provider, maps photo URLs from the bundled photo file, loads biographies from the bundled biographies file, and inserts all drivers into the database.
2. **Given** the drivers table already has data, **When** the application starts, **Then** the seed process is skipped entirely.
3. **Given** a driver from the external provider is not found in the biographies file, **When** seeding runs, **Then** that driver is still inserted with all other fields, but biography is set to null.
4. **Given** a driver from the external provider is not found in the photo mapping file, **When** seeding runs, **Then** that driver is still inserted with all other fields, but photo URL is set to null.
5. **Given** the external F1 data provider is unreachable, **When** seeding runs, **Then** the seed fails and logs an error (base data is mandatory).

---

### User Story 3 - Driver Detail Card (Priority: P2)

A user taps on a driver in the list to see their full profile. The app calls the detail endpoint with the driver's identifier and receives a complete card including the driver's photo, current team name, and biography.

**Why this priority**: This is the primary new user-facing feature. It depends on P1 (data must exist in the database first) but delivers the key new value — rich driver profiles.

**Independent Test**: Can be tested by requesting a specific driver by ID and verifying the response includes all base fields plus photo URL, team information (name resolved from team data), and biography.

**Acceptance Scenarios**:

1. **Given** a driver "max_verstappen" exists in the database with a team assignment, **When** an authenticated user requests their detail card, **Then** the system returns the full card with: driverId, number, code, firstName, lastName, nationality, dateOfBirth, photoUrl, team (teamId and name resolved from team data), and biography.
2. **Given** a driver exists but has no biography (AI generation was skipped), **When** their detail card is requested, **Then** the response includes all other fields with biography as null.
3. **Given** a driver exists but has no photo URL, **When** their detail card is requested, **Then** the response includes all other fields with photoUrl as null.
4. **Given** no driver exists with the requested ID, **When** the detail endpoint is called, **Then** the system returns a 404 Not Found response.
5. **Given** an unauthenticated user, **When** they request a driver detail card, **Then** the system returns a 401 Unauthorized response.

---

### User Story 4 - Removal of External API Runtime Dependency for Drivers (Priority: P3)

The system no longer depends on the external F1 data provider at runtime for driver data. The in-memory cache and proxy logic for drivers are removed. This simplifies the architecture and eliminates a runtime failure mode.

**Why this priority**: This is a cleanup/architectural improvement. It reduces complexity and removes a source of runtime errors, but users don't directly see this — they just experience faster, more reliable responses (covered by P1).

**Independent Test**: Can be verified by confirming the in-memory driver cache is removed, no runtime calls to the external provider are made for driver data, and all driver tests still pass.

**Acceptance Scenarios**:

1. **Given** the system is running with a seeded database, **When** the external F1 data provider goes offline, **Then** the driver list and detail endpoints continue to function normally.
2. **Given** the previous in-memory cache code existed, **When** this feature is complete, **Then** the driver cache interface and its in-memory implementation are removed from the codebase.

---

### Edge Cases

- What happens when a driver has a null permanent number (e.g., reserve drivers)? The number field should be nullable.
- What happens when the photo mapping JSON file is malformed or missing? Seed should continue without photos and log a warning.
- What happens when the biographies JSON file is malformed or missing? Seed should continue without biographies and log a warning.
- What happens when two application instances start simultaneously with an empty database? The seed should handle concurrent execution gracefully (e.g., use UPSERT to avoid duplicate key errors).
- What happens when a driver's team assignment changes mid-season? The team_id in the drivers table can be updated on a future re-seed; the team name is always resolved dynamically via the team data relationship.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST store driver data persistently in a database table with fields: unique driver identifier, number, code, first name, last name, nationality, date of birth, photo URL, team reference, biography, and timestamps.
- **FR-002**: System MUST automatically seed the drivers table on first startup when the table is empty, using base data from the external F1 data provider. Driver-to-team assignments MUST also be fetched from the external provider (canonical team IDs), not derived from the photo mapping file.
- **FR-003**: System MUST enrich driver records during seeding with photo URLs mapped from a bundled configuration file using the formula1.com CDN URL pattern.
- **FR-004**: System MUST enrich driver records during seeding with biographies loaded from a bundled JSON file (`driver-biographies.json`) that maps each driverId to a biography text.
- **FR-005**: System MUST skip biography assignment gracefully (set to null) if a driver is not found in the biographies file.
- **FR-006**: System MUST skip photo URL assignment gracefully (set to null) if a driver is not found in the photo mapping file.
- **FR-007**: System MUST fail the seed process if the external F1 data provider is unreachable (base data is mandatory).
- **FR-008**: System MUST skip the seed process entirely on subsequent startups when the drivers table already contains data.
- **FR-009**: System MUST serve the driver list endpoint from the database instead of the in-memory cache, maintaining all existing fields and adding photoUrl as a new field (backward-compatible addition). The season query parameter is ignored — the endpoint always returns current-season drivers.
- **FR-010**: System MUST provide a detail endpoint that returns a full driver card by driver identifier, including team name resolved from team data (not stored redundantly in the drivers table).
- **FR-011**: System MUST return a 404 response when a driver detail is requested for a non-existent identifier.
- **FR-012**: Both driver list and driver detail endpoints MUST require JWT authentication.
- **FR-013**: System MUST remove the in-memory driver cache and related external API proxy logic for drivers.
- **FR-014**: System MUST store the team reference as a foreign key-style identifier that links to a persistent teams table, ensuring team name is resolved via database JOIN (not duplicated in the drivers table).
- **FR-015**: System MUST log clearly which enrichment steps (photos, biographies) succeeded and which were skipped during seeding.
- **FR-020**: Biographies MUST be loaded from a bundled JSON resource file (`src/main/resources/seed/driver-biographies.json`) containing an array of objects with `driverId` and `biography` fields. No external AI service is used.
- **FR-016**: System MUST persist teams to a database table with fields: unique team identifier, name, and nationality. The teams table is seeded alongside drivers.
- **FR-017**: System MUST seed the teams table from the external F1 data provider on first startup (same resilience rules as driver seeding — skip if already populated).
- **FR-018**: The existing team list endpoint MUST switch to reading from the database instead of the in-memory cache, maintaining all existing fields.
- **FR-019**: System MUST remove the in-memory team cache and related external API proxy logic for teams.

### Key Entities

- **Driver**: A Formula 1 driver with base information (identifier, number, code, name, nationality, date of birth), enrichment data (photo URL, biography), and a team reference. The driver identifier is stable across seasons (e.g., "max_verstappen").
- **Team**: A Formula 1 constructor/team with identifier, name, and nationality. Persisted to its own database table as part of this feature. The driver references a team by identifier; the team's display name is resolved via database JOIN at query time, not stored in the driver record.
- **Photo Mapping**: A bundled configuration that maps each driver identifier to their formula1.com CDN photo parameters (team slug and driver code). Used only during seeding.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The driver list endpoint returns results from the database with no runtime dependency on external APIs, achieving consistent response times regardless of external service availability.
- **SC-002**: The driver detail endpoint returns a complete driver card (including photo, team name, and biography) for any valid driver identifier.
- **SC-003**: The driver detail endpoint returns a 404 for unknown driver identifiers.
- **SC-004**: On first startup with an empty database, the seed process populates all current-season drivers (22 drivers) with base data, photo URLs for all mapped drivers, and biographies for all drivers when the AI service is available.
- **SC-005**: The seed process populates biographies from the bundled JSON file for all mapped drivers; drivers not in the file get null biography.
- **SC-006**: On subsequent startups, the seed process is skipped and application startup time is not affected by seed logic.
- **SC-007**: The driver list response retains all existing fields and adds photoUrl — no breaking changes for API consumers (additive only).
- **SC-008**: All existing tests continue to pass, and new integration tests cover: driver list from database, driver detail happy path, driver detail 404, and authentication enforcement on both endpoints.
- **SC-009**: The in-memory driver cache code is fully removed from the codebase.
- **SC-010**: Teams are persisted in the database and the team list endpoint reads from the database with no runtime external API dependency.
- **SC-011**: The in-memory team cache code is fully removed from the codebase.

## Clarifications

### Session 2026-03-28

- Q: Should photoUrl be added to the driver list response (breaking "same format" guarantee) or kept only in the detail endpoint? → A: Add photoUrl to the list response (additive, backward-compatible change).
- Q: How should the detail endpoint resolve team name, given teams are currently in-memory only? → A: Persist teams to the database as part of this feature (teams table + seed), enabling a proper DB JOIN for team name resolution.
- Q: What should happen when a client passes a non-current season to the driver list endpoint? → A: Ignore the season parameter — always return current-season drivers from DB. Historical season support is deferred to a future task.
- Q: How should the driver's team_id be determined during seed, given photo mapping slugs differ from Jolpica team IDs? → A: Fetch team assignments from Jolpica (e.g., constructor standings endpoint) to get canonical team IDs per driver. Photo mapping is only used for building photo URLs.
- Q: How should biographies be sourced? → A: From a bundled JSON file (`driver-biographies.json`) instead of the Anthropic Claude API. No external AI service dependency. The file is user-curated and checked into the repository.

## Assumptions

- Teams are persisted to the database as part of this feature. The team identifiers from the external F1 data provider match the team references stored in driver records.
- The bundled photo mapping file covers all 22 current-season drivers. If the grid changes mid-season, the mapping file would need a manual update and re-seed.
- Biographies are sourced from a bundled JSON file checked into the repository. No external AI service is required.
- The driver identifier format from the external F1 data provider (e.g., "max_verstappen") is stable and suitable as a unique lookup key.
- The season query parameter on the driver list endpoint is ignored in this version — all requests return the current season. Historical season support is a deferred future task.
- The photo URL CDN pattern (formula1.com) is stable and publicly accessible. URLs are stored as-is; the system does not verify URL reachability.
