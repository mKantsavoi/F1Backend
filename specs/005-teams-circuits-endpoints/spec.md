# Feature Specification: Teams & Circuits Data Endpoints

**Feature Branch**: `005-teams-circuits-endpoints`
**Created**: 2026-03-27
**Status**: Draft
**Input**: User description: "Add teams and circuits data endpoints following the drivers pattern, fetching from Jolpica F1 API with caching and JWT protection"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse Current Season Teams (Priority: P1)

An authenticated mobile app user opens the teams section to see which constructors are competing in the current F1 season. The app calls the teams endpoint and displays a list showing each team's name, nationality, and stable identifier. The data loads quickly because it is served from a cache after the first request.

**Why this priority**: Teams are a core part of the F1 data catalog. Users need to know which constructors are active before exploring deeper data like standings or race results.

**Independent Test**: Can be fully tested by calling GET /api/v1/teams with a valid JWT and verifying the response contains a list of constructors with the correct fields and current season label.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they request GET /api/v1/teams without a season parameter, **Then** they receive a JSON response containing the current season's constructors with teamId, name, and nationality for each team, and the season field reflects the current year.
2. **Given** an authenticated user, **When** they request GET /api/v1/teams?season=2024, **Then** they receive constructors for the 2024 season specifically.
3. **Given** an unauthenticated user, **When** they request GET /api/v1/teams, **Then** they receive a 401 Unauthorized response.

---

### User Story 2 - Browse All F1 Circuits (Priority: P1)

An authenticated mobile app user opens the circuits section to browse the full catalog of F1 circuits. The app calls the circuits endpoint and displays each circuit's name, location (city and country), geographic coordinates, and a link to more information. The full historical list is returned since circuit data is essentially static.

**Why this priority**: Circuits are equally fundamental to the F1 data catalog. Users browsing race schedules or historical data need circuit information as a core reference.

**Independent Test**: Can be fully tested by calling GET /api/v1/circuits with a valid JWT and verifying the response contains all circuits with the correct fields.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they request GET /api/v1/circuits, **Then** they receive a JSON response containing a list of all F1 circuits, each with circuitId, name, locality, country, lat, lng, and url.
2. **Given** an unauthenticated user, **When** they request GET /api/v1/circuits, **Then** they receive a 401 Unauthorized response.

---

### User Story 3 - Cached Responses for Repeat Requests (Priority: P2)

When the same endpoint is called multiple times, subsequent requests are served from the in-memory cache rather than re-fetching from the upstream data source. Teams data is cached per season for 24 hours. Circuits data is cached indefinitely since it rarely changes.

**Why this priority**: Caching reduces latency for users and prevents excessive load on the upstream data source. It is essential for a responsive mobile experience but secondary to basic data retrieval.

**Independent Test**: Can be tested by making two sequential requests to the same endpoint and verifying the second is served from cache (faster response, no upstream call).

**Acceptance Scenarios**:

1. **Given** a teams request was made within the last 24 hours for a specific season, **When** the same season is requested again, **Then** the response is served from cache without contacting the upstream source.
2. **Given** a circuits request was made previously, **When** circuits are requested again at any later time, **Then** the response is served from cache without contacting the upstream source.

---

### User Story 4 - Graceful Degradation When Upstream Is Down (Priority: P2)

When the upstream data source is unavailable, the system serves stale cached data (if available) rather than returning an error. A Warning header is included so the client knows the data may be outdated.

**Why this priority**: Reliability is important for mobile users who expect data to always be available. Graceful degradation prevents a blank screen when the upstream source has temporary issues.

**Independent Test**: Can be tested by simulating an upstream failure after a successful cache fill and verifying the stale data is returned with a Warning header.

**Acceptance Scenarios**:

1. **Given** cached teams data exists but the upstream source is unreachable, **When** a user requests teams, **Then** the stale cached data is returned with a Warning HTTP header indicating the data may be outdated.
2. **Given** cached circuits data exists but the upstream source is unreachable, **When** a user requests circuits, **Then** the stale cached data is returned with a Warning HTTP header.
3. **Given** no cached data exists and the upstream source is unreachable, **When** a user requests teams or circuits, **Then** an appropriate error response is returned.

---

### Edge Cases

- What happens when an invalid season value is provided (e.g., ?season=abc or ?season=1800)? The system should return a meaningful error response.
- What happens when the upstream source returns an empty list of constructors for a valid season? The system should return an empty teams list with the correct season label.
- What happens when the upstream source returns malformed data? The system should return an error rather than corrupted data.
- What happens when the teams cache for one season expires but another season's cache is still valid? Each season's cache operates independently.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST expose a protected endpoint for retrieving F1 teams (constructors) for a given season
- **FR-002**: System MUST expose a protected endpoint for retrieving the full catalog of F1 circuits
- **FR-003**: The teams endpoint MUST accept an optional season parameter, defaulting to the current season when omitted
- **FR-004**: The circuits endpoint MUST return all circuits without season filtering
- **FR-005**: Both endpoints MUST require a valid JWT for access and return 401 Unauthorized without one
- **FR-006**: The teams response MUST include a season label and a list of teams, each with a stable identifier (teamId), name, and nationality
- **FR-007**: The circuits response MUST include a list of circuits, each with a stable identifier (circuitId), name, locality (city), country, latitude, longitude, and a reference URL
- **FR-008**: Teams data MUST be cached per season with a 24-hour expiry
- **FR-009**: Circuits data MUST be cached indefinitely (no time-based expiry)
- **FR-010**: Both endpoints MUST serve stale cached data with a Warning header when the upstream data source is unavailable
- **FR-011**: Both endpoints MUST follow the same architectural pattern as the existing drivers endpoint (use case, data source, DI module, route handler)
- **FR-012**: All existing endpoints and tests MUST continue to function without modification

### Key Entities

- **Team**: Represents an F1 constructor. Attributes: stable identifier (e.g., "red_bull"), display name, nationality. Scoped to a specific season.
- **Circuit**: Represents an F1 racing circuit. Attributes: stable identifier (e.g., "monza"), name, locality (city), country, geographic coordinates (latitude/longitude), reference URL. Not season-scoped.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Authenticated users can retrieve the current season's teams list in a single request with all specified fields present
- **SC-002**: Authenticated users can retrieve the full circuits catalog in a single request with all specified fields present
- **SC-003**: Users requesting a specific past season's teams (e.g., 2024) receive the correct data for that season
- **SC-004**: Unauthenticated requests to either endpoint are rejected with a 401 status
- **SC-005**: Repeated requests within the cache window are served without re-fetching from the upstream source
- **SC-006**: When the upstream source is unavailable, users still receive previously cached data with a staleness warning
- **SC-007**: All existing functionality (authentication, drivers endpoint) continues to work identically
- **SC-008**: All automated quality checks (linting, static analysis, tests) pass without errors

## Assumptions

- The existing JWT authentication infrastructure is reused as-is for protecting both new endpoints
- The existing upstream data client infrastructure (retry logic, error handling) is reused for the new data paths
- The "current season" default for teams is determined by the current calendar year (same approach as drivers)
- Circuit data changes infrequently enough (new circuits added roughly once per year) that indefinite caching is acceptable
- The upstream API returns a maximum of 100 circuits, which fits within a single paginated request
- The mobile client does not need pagination for teams or circuits (the full list is small enough to return in one response)
- Both endpoints follow the same response envelope structure established by the drivers endpoint
