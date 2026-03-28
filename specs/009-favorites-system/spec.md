# Feature Specification: Favorites System

**Feature Branch**: `009-favorites-system`
**Created**: 2026-03-28
**Status**: Draft
**Input**: User description: "Build a favorites system that allows users to save their favorite drivers and teams for quick access and personalized content."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Add and Remove Favorite Drivers (Priority: P1)

A user browses driver profiles and taps a heart icon to mark a driver as a favorite. The driver is saved to the user's personal favorites list. Tapping the heart again removes the driver. The user can favorite any number of drivers. The operation is forgiving: adding an already-favorited driver silently succeeds, and removing a non-favorited driver silently succeeds.

**Why this priority**: Favoriting drivers is the core interaction of the entire feature. Without add/remove capability, no other favorites functionality can work.

**Independent Test**: Can be fully tested by adding a driver to favorites and verifying it appears in the favorites list. Delivers the foundational engagement loop.

**Acceptance Scenarios**:

1. **Given** an authenticated user viewing a driver, **When** they add the driver to favorites, **Then** the driver is saved and the system confirms the addition (created status).
2. **Given** a user who has already favorited a driver, **When** they add the same driver again, **Then** the system responds successfully without error (idempotent).
3. **Given** a user who has favorited a driver, **When** they remove that driver from favorites, **Then** the driver is removed from their list.
4. **Given** a user who has NOT favorited a driver, **When** they attempt to remove that driver, **Then** the system responds successfully without error (idempotent).
5. **Given** an authenticated user, **When** they try to add a driver that does not exist in the system, **Then** the system responds with a not-found error.
6. **Given** an unauthenticated user, **When** they attempt any favorites operation, **Then** the system rejects the request as unauthorized.

---

### User Story 2 - Add and Remove Favorite Teams (Priority: P1)

Same interaction as drivers, but for teams. A user can mark teams as favorites and remove them. Operations are idempotent and require the team to exist in the system.

**Why this priority**: Teams are equally important as drivers for fan engagement. Many fans identify primarily with a team rather than individual drivers.

**Independent Test**: Can be fully tested by adding a team to favorites and verifying it appears in the favorites list.

**Acceptance Scenarios**:

1. **Given** an authenticated user, **When** they add a team to favorites, **Then** the team is saved and confirmed.
2. **Given** a user who has already favorited a team, **When** they add it again, **Then** the system responds successfully (idempotent).
3. **Given** a user with a favorited team, **When** they remove it, **Then** it is removed from their list.
4. **Given** a user, **When** they try to add a non-existent team, **Then** the system responds with not-found.

---

### User Story 3 - View All Favorite Drivers and Teams (Priority: P1)

A user opens their Favorites tab and sees complete, render-ready cards for all their favorited drivers and teams. Driver cards include photo, name, team, and number. Team cards include name, nationality, and current drivers. This is not just a list of IDs -- the response contains all data needed to render the UI immediately.

**Why this priority**: Viewing favorites is the primary read operation. Without it, users cannot see what they have favorited, making the add/remove feature useless.

**Independent Test**: Can be tested by adding several favorites and then listing them, verifying full details are returned.

**Acceptance Scenarios**:

1. **Given** a user with favorited drivers, **When** they view their favorite drivers, **Then** they see full driver cards with photo, name, number, code, team name, and team color.
2. **Given** a user with favorited teams, **When** they view their favorite teams, **Then** they see full team cards with name, nationality, and current driver roster.
3. **Given** a new user with no favorites, **When** they view favorites, **Then** they see an empty list (not an error).
4. **Given** each favorite entry, **Then** it includes a timestamp of when the user added it.

---

### User Story 4 - Check Favorite Status for UI Icons (Priority: P2)

When the app renders a driver or team card anywhere in the UI, it needs to know whether to show a filled or empty heart icon. A lightweight check endpoint returns a simple yes/no answer for a specific driver or team.

**Why this priority**: Important for UI polish but the app can function without it (could derive status from the full favorites list). Provides a fast, targeted check instead of loading the entire list.

**Independent Test**: Can be tested by checking status before and after adding a favorite, verifying the response toggles correctly.

**Acceptance Scenarios**:

1. **Given** a user who has favorited a driver, **When** they check if that driver is a favorite, **Then** the response indicates true.
2. **Given** a user who has NOT favorited a driver, **When** they check, **Then** the response indicates false.
3. **Given** a user, **When** they check favorite status for a team, **Then** the correct true/false is returned.

---

### User Story 5 - Personalized Home Screen Feed (Priority: P2)

The app's home screen displays a personalized feed showing the user's favorite drivers and teams with their current championship standings and most recent race results. This is a single aggregated request that combines data from favorites, driver/team details, championship standings, and last race results.

**Why this priority**: This is the main value proposition of the favorites feature -- turning raw favorites into a personalized, glanceable dashboard. Depends on all prior stories being functional.

**Independent Test**: Can be tested by adding favorites and requesting the feed, verifying championship positions, points, and last race results are included.

**Acceptance Scenarios**:

1. **Given** a user with favorited drivers, **When** they request the feed, **Then** each favorite driver shows their championship position, points, and last race result (race name, finishing position, points scored).
2. **Given** a user with favorited teams, **When** they request the feed, **Then** each favorite team shows their constructor championship position and points.
3. **Given** a user with no favorites, **When** they request the feed, **Then** empty lists are returned (not errors).
4. **Given** any feed response, **Then** it includes a news section (empty array placeholder for future implementation).
5. **Given** a user requesting the feed, **Then** the response is served with short-lived caching (approximately 30 seconds) to balance freshness and performance.

---

### User Story 6 - Multi-User Isolation (Priority: P1)

Each user's favorites are completely private to them. User A's favorites never appear in User B's responses. All favorites operations are scoped to the authenticated user.

**Why this priority**: Data isolation is a fundamental correctness requirement. Without it, the feature is broken from a privacy and usability standpoint.

**Independent Test**: Can be tested by having two different users add different favorites and verifying each user only sees their own.

**Acceptance Scenarios**:

1. **Given** User A has favorited Driver X and User B has favorited Driver Y, **When** User A lists favorites, **Then** only Driver X appears.
2. **Given** User A has favorited a team, **When** User B lists favorites, **Then** User A's team does NOT appear.

---

### Edge Cases

- What happens when a user deletes their account? All their favorites are automatically removed (cascading deletion).
- What happens when a driver or team is removed from the system after being favorited? The favorite record remains orphaned but listing favorites gracefully handles missing driver/team data.
- What happens if the championship standings or last race data is unavailable when building the feed? The feed returns whatever data is available; missing standings fields show as null rather than causing an error.
- What happens if a user rapidly adds and removes the same driver? Each operation is idempotent and atomic, so rapid toggling produces consistent results.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow authenticated users to add any existing driver to their personal favorites list.
- **FR-002**: System MUST allow authenticated users to remove any driver from their personal favorites list.
- **FR-003**: System MUST allow authenticated users to add any existing team to their personal favorites list.
- **FR-004**: System MUST allow authenticated users to remove any team from their personal favorites list.
- **FR-005**: Add and remove operations MUST be idempotent -- adding an already-favorited entity returns success, removing a non-favorited entity returns success.
- **FR-006**: System MUST return a not-found error when a user tries to favorite a driver or team that does not exist in the system.
- **FR-007**: System MUST provide a list of all favorite drivers for a user, returning full driver details (name, number, code, photo, team name, team color, date added).
- **FR-008**: System MUST provide a list of all favorite teams for a user, returning full team details (name, nationality, current drivers, date added).
- **FR-009**: System MUST provide a lightweight check endpoint that returns whether a specific driver or team is in a user's favorites.
- **FR-010**: System MUST provide an aggregated feed endpoint combining favorite drivers' and teams' championship standings and last race results in a single response.
- **FR-011**: The feed MUST include a news section (empty array) as a placeholder for future news integration.
- **FR-012**: All favorites endpoints MUST require authentication; unauthenticated requests MUST be rejected.
- **FR-013**: Favorites MUST be scoped per user -- one user's favorites are never visible to another user.
- **FR-014**: System MUST return empty lists (not errors) when a user has no favorites.
- **FR-015**: System MUST support users favoriting any number of drivers and teams without an artificial limit.
- **FR-016**: The add-favorite operation MUST distinguish between newly created (first time) and already existing (duplicate) in its response status.

### Key Entities

- **Favorite Driver**: A junction between a user and a driver. Attributes: the user, the driver, and the timestamp when favorited. The combination of user and driver must be unique.
- **Favorite Team**: A junction between a user and a team. Attributes: the user, the team, and the timestamp when favorited. The combination of user and team must be unique.
- **Personalized Feed**: A computed view (not stored) that aggregates a user's favorite drivers and teams with their current championship standings and most recent race performance.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can add a driver or team to favorites in a single action and see it reflected immediately in their favorites list.
- **SC-002**: Users can view a personalized home screen feed with championship standings and last race results for all their favorites in a single request.
- **SC-003**: Duplicate add or redundant remove operations complete successfully without errors 100% of the time (idempotent behavior).
- **SC-004**: Attempting to favorite a non-existent driver or team produces a clear not-found response.
- **SC-005**: Favorites are completely isolated per user -- no cross-user data leakage under any circumstance.
- **SC-006**: Users with no favorites see appropriate empty states (empty lists), not errors.
- **SC-007**: The favorites list returns complete, render-ready entity cards so the client does not need follow-up requests for display data.
- **SC-008**: All favorites operations require authentication; unauthorized access attempts are rejected.
- **SC-009**: All existing application functionality continues to work identically after the favorites feature is added (no regressions).
- **SC-010**: The feed response is served within acceptable latency using short-lived caching (~30 seconds) to balance data freshness with performance.

## Assumptions

- Users are already registered and authenticated via the existing JWT-based auth system.
- Driver and team data already exists in the system from prior features (driver and team persistence).
- Championship standings and race results are available from the existing in-memory cache (Jolpica integration).
- There is no limit on the number of favorites a user can have (the F1 grid is inherently limited to ~20 drivers and ~10 teams).
- The news section in the feed is a placeholder (always empty array) until a future news feature is built.
- Deleting a user account cascades to remove all their favorites automatically.
- The feed endpoint aggregates data in real-time from existing data sources; no separate denormalized storage is needed.
