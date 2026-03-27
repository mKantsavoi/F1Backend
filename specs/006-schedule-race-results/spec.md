# Feature Specification: Schedule, Race Results & Qualifying Endpoints

**Feature Branch**: `006-schedule-race-results`
**Created**: 2026-03-27
**Status**: Draft
**Input**: User description: "Add schedule, race results, and qualifying endpoints sourced from the Jolpica F1 API"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Season Schedule (Priority: P1)

A user opens the app and wants to see all races this season — dates, circuits, and session times. The app requests the full season schedule and displays a calendar-style list of all Grands Prix. The user can also view schedules from previous seasons.

**Why this priority**: The season schedule is the most fundamental data in any F1 app. Users need to know when races happen before they can follow results. This is the entry point for all other race-weekend data.

**Independent Test**: Can be fully tested by requesting the season schedule endpoint and verifying a complete list of race weekends with dates, circuits, and session times is returned.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they request the current season schedule without specifying a season, **Then** the system returns all race weekends for the current year with round numbers, race names, circuit details, countries, dates, times, and session schedules.
2. **Given** an authenticated user, **When** they request a schedule for a specific past season (e.g., 2024), **Then** the system returns the complete schedule for that season.
3. **Given** an authenticated user, **When** the upstream data source is temporarily unavailable but cached data exists, **Then** the system returns the cached schedule with a warning indicating the data may be stale.
4. **Given** an unauthenticated user, **When** they request the schedule, **Then** the system rejects the request with an authentication error.

---

### User Story 2 - Check Next Race Countdown (Priority: P1)

The app home screen shows a countdown to the next race. The app requests the next upcoming Grand Prix and receives all session dates and times (FP1, FP2, FP3, Qualifying, Sprint if applicable, Race) for easy countdown display.

**Why this priority**: The "next race" widget is the most common home-screen element in F1 apps. It drives daily engagement and is the simplest endpoint to consume.

**Independent Test**: Can be fully tested by requesting the next-race endpoint and verifying a single race entry with all session times is returned.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they request the next race, **Then** the system returns a single race entry with all session dates and times.
2. **Given** an authenticated user, **When** the season has ended and there is no next race, **Then** the system returns an appropriate empty or informational response.
3. **Given** an authenticated user, **When** the upstream source is unavailable but cached data exists, **Then** the system returns cached data with a staleness warning.

---

### User Story 3 - View Race Results (Priority: P1)

After a Grand Prix, the user checks the finishing order. The app requests results for a specific season and round and displays positions, driver info, team info, lap counts, finishing times, points awarded, finishing status, and fastest lap information.

**Why this priority**: Race results are the core content users seek after every Grand Prix. Without results, the app has no post-race value.

**Independent Test**: Can be fully tested by requesting race results for a known completed race and verifying the full finishing order with all expected fields.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they request results for a completed race (e.g., season 2025, round 1), **Then** the system returns the full finishing order with position, driver details, team details, grid position, laps completed, finishing time, points, status, and fastest lap info.
2. **Given** an authenticated user, **When** they request results for a race that has not yet occurred, **Then** the system returns an appropriate empty response or informational message.
3. **Given** an authenticated user, **When** they provide an invalid season or round (e.g., negative number, non-numeric, round 0), **Then** the system rejects the request with a validation error.
4. **Given** an authenticated user, **When** the upstream source is unavailable but cached data exists, **Then** the system returns cached results with a staleness warning.

---

### User Story 4 - View Qualifying Results (Priority: P2)

Before or after qualifying, the user checks Q1, Q2, and Q3 lap times. The app requests qualifying results for a specific season and round and displays each driver's qualifying position and times per session.

**Why this priority**: Qualifying results are highly sought after but slightly less critical than race results. They complete the race-weekend picture.

**Independent Test**: Can be fully tested by requesting qualifying results for a known completed qualifying session and verifying positions with Q1/Q2/Q3 times.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they request qualifying results for a completed qualifying session, **Then** the system returns each driver's position, driver details, team details, and Q1/Q2/Q3 times (with missing times for drivers eliminated in earlier sessions).
2. **Given** an authenticated user, **When** they request qualifying for a round that hasn't happened yet, **Then** the system returns an appropriate empty response.
3. **Given** an authenticated user, **When** they provide invalid path parameters, **Then** the system returns a validation error.

---

### User Story 5 - View Sprint Results (Priority: P3)

For sprint weekends, the user checks sprint race results. The app requests sprint results for a specific season and round and displays the finishing order in the same format as race results.

**Why this priority**: Sprint races occur only at select weekends, making this a supplementary feature. Important for completeness but lower frequency of use.

**Independent Test**: Can be fully tested by requesting sprint results for a known sprint weekend and verifying the finishing order is returned, and by requesting sprint results for a non-sprint weekend and verifying a not-found response.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they request sprint results for a round that had a sprint race, **Then** the system returns the sprint finishing order with the same detail as race results.
2. **Given** an authenticated user, **When** they request sprint results for a round that did not have a sprint race, **Then** the system returns a not-found response.
3. **Given** an authenticated user, **When** the upstream source is unavailable but cached data exists, **Then** the system returns cached sprint results with a staleness warning.

---

### Edge Cases

- What happens when the upstream data source returns an empty race table (e.g., newly announced season with no races yet)?
- How does the system handle a race round that exists but has no results yet (race scheduled but not completed)?
- What happens when qualifying is cancelled or replaced (e.g., weather cancellation)?
- How does the system handle the transition between "current season" and "past season" at year boundaries for cache duration decisions?
- What happens when a user requests season data for a year with no F1 data (e.g., season=1800 or season=2099)?
- How does the system handle post-race penalties that change results after initial publication?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a season schedule endpoint that returns all race weekends for a given season, defaulting to the current season when no season parameter is provided.
- **FR-002**: System MUST provide a next-race endpoint that returns the single next upcoming Grand Prix with all session dates and times.
- **FR-003**: System MUST provide a race results endpoint that returns the full finishing order for a specific season and round, including position, driver info, team info, grid position, laps, time, points, status, and fastest lap details.
- **FR-004**: System MUST provide a qualifying results endpoint that returns qualifying positions with Q1, Q2, and Q3 times for a specific season and round.
- **FR-005**: System MUST provide a sprint results endpoint that returns sprint race finishing order for a specific season and round, returning a not-found response when the round did not include a sprint.
- **FR-006**: All five endpoints MUST require valid authentication before returning data.
- **FR-007**: System MUST cache schedule data with a time-limited expiration (hours) to account for possible calendar changes.
- **FR-008**: System MUST cache next-race data with a shorter time-limited expiration (approximately 1 hour).
- **FR-009**: System MUST cache race results with a short expiration for the current season (to capture post-race penalty updates) and indefinitely for past seasons.
- **FR-010**: System MUST cache qualifying and sprint results indefinitely (these do not change after the session).
- **FR-011**: System MUST serve stale cached data with a staleness warning when the upstream data source is unavailable (graceful degradation).
- **FR-012**: System MUST validate season and round path parameters and reject invalid values (non-numeric, out of reasonable range) with an appropriate error response.
- **FR-013**: System MUST distinguish between "current season" and "past season" for cache duration decisions based on whether the requested season matches the current calendar year.

### Key Entities

- **Race Weekend**: A single Grand Prix event identified by season and round number. Contains race name, circuit information (ID, name, country), date, time, and a set of session times (practice, qualifying, sprint, race).
- **Race Result**: A single driver's finishing position in a Grand Prix. Contains position, driver identity (ID, code, name), team identity (ID, name), grid position, laps completed, finishing time, points awarded, finishing status, and optional fastest lap details (rank, lap number, time, average speed).
- **Qualifying Result**: A single driver's qualifying outcome. Contains position, driver identity, team identity, and lap times for Q1, Q2, and Q3 (Q2/Q3 may be absent for eliminated drivers).
- **Sprint Result**: Same structure as a race result but for a sprint race. Only exists for rounds designated as sprint weekends.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Authenticated users can retrieve the full season schedule in a single request and see all race weekends with dates, circuits, and session times.
- **SC-002**: Authenticated users can retrieve the next upcoming race in a single request for countdown/widget display.
- **SC-003**: Authenticated users can retrieve race results, qualifying results, and sprint results for any valid season and round combination.
- **SC-004**: Sprint results requests for non-sprint rounds return a clear not-found indication within normal response time.
- **SC-005**: When the upstream data source is unavailable, all endpoints continue serving previously cached data with an explicit staleness warning, ensuring zero downtime for end users.
- **SC-006**: Invalid season or round values are rejected immediately with a clear validation error before any upstream request is made.
- **SC-007**: All endpoints enforce authentication — unauthenticated requests receive an appropriate rejection.
- **SC-008**: Cached data for past seasons is served without re-fetching from the upstream source, reducing unnecessary external calls.
- **SC-009**: All existing application functionality (authentication, drivers, teams, circuits) continues working identically after these additions.

## Assumptions

- Users have valid authentication credentials before accessing these endpoints.
- The Jolpica F1 API is the sole upstream data source and follows the Ergast-compatible URL structure.
- Session times (FP1, FP2, FP3, Qualifying, Sprint, Race) are provided by the upstream source; if any session time is missing, the field is omitted rather than returning a placeholder.
- The "current season" is determined by the server's current calendar year at request time.
- When the upstream source returns an empty result set (e.g., future race with no results), the system returns a successful response with an empty results list rather than an error.
- Sprint weekends are identified by the upstream source returning data for the sprint endpoint; the system does not maintain its own sprint-weekend calendar.
- Fastest lap information may be absent for some drivers (e.g., DNS, DNF early); these fields are optional in the response.
