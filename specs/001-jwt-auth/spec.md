# Feature Specification: JWT Authentication & Authorization

**Feature Branch**: `001-jwt-auth`
**Created**: 2026-03-26
**Status**: Draft
**Input**: User description: "Build a JWT-based authentication and authorization system for the F1 backend API"

## Clarifications

### Session 2026-03-26

- Q: Can a user hold multiple active sessions simultaneously (e.g., phone + tablet)? → A: Yes, multiple concurrent sessions allowed. Each login creates an independent token pair without affecting existing sessions.
- Q: What are the validation rules for usernames? → A: 3–30 characters, Latin letters/digits/hyphens/underscores only, cannot be empty or solely digits. Uniqueness not enforced (display name, not identifier).
- Q: Does the admin role gate any behavior in this feature? → A: No. The role is stored but has no functional differentiation here; downstream features will consume it.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Account Registration (Priority: P1)

A new user opens the mobile app and creates an account by providing their email, username, and password. The system validates the input, creates the account, and immediately logs the user in by returning a token pair (access token + refresh token). The user can then access protected features without any additional login step.

**Why this priority**: Registration is the entry point for all users. Without it, no other authentication feature can be used. This is the foundational flow that unlocks the entire system.

**Independent Test**: Can be fully tested by submitting a registration request with valid credentials and verifying that the response contains a token pair and the user can access a protected endpoint.

**Acceptance Scenarios**:

1. **Given** a new user with a valid email, username, and password (8+ characters, letters and digits), **When** they submit the registration form, **Then** the system creates the account and returns an access token and refresh token.
2. **Given** an email that is already registered, **When** a user attempts to register with that email, **Then** the system rejects the request with an appropriate error message.
3. **Given** a password with fewer than 8 characters or missing letters or digits, **When** the user submits registration, **Then** the system rejects the request with a validation error.
4. **Given** an invalid email format, **When** the user submits registration, **Then** the system rejects the request with a validation error.

---

### User Story 2 - Login (Priority: P1)

A returning user provides their email and password. The system verifies the credentials and returns a fresh token pair. The user can then access all protected features.

**Why this priority**: Login is equally critical to registration -- returning users depend on it for every session. It is co-priority with registration as the two together form the minimum viable auth system.

**Independent Test**: Can be tested by registering an account, then logging in with the same credentials and verifying that a valid token pair is returned.

**Acceptance Scenarios**:

1. **Given** a registered user with valid credentials, **When** they submit their email and password, **Then** the system returns a new access token and refresh token.
2. **Given** an incorrect email or password, **When** the user attempts to log in, **Then** the system returns a generic "invalid credentials" error without indicating which field is wrong.
3. **Given** a non-existent email, **When** the user attempts to log in, **Then** the system returns the same generic "invalid credentials" error (no enumeration of valid accounts).

---

### User Story 3 - Accessing Protected Resources (Priority: P1)

A logged-in user sends requests to protected API endpoints using their access token in the Authorization header (Bearer scheme). The server validates the token and grants or denies access accordingly.

**Why this priority**: This is the core purpose of the authentication system -- gating access to protected resources. Without this, tokens have no practical use.

**Independent Test**: Can be tested by obtaining tokens via login, then calling a protected endpoint with and without the token to verify access control behavior.

**Acceptance Scenarios**:

1. **Given** a valid, non-expired access token, **When** the user sends a request with the token in the Authorization header, **Then** the server processes the request normally.
2. **Given** an expired access token, **When** the user sends a request, **Then** the server returns a 401 Unauthorized response.
3. **Given** no access token in the request, **When** the user calls a protected endpoint, **Then** the server returns a 401 Unauthorized response.
4. **Given** a tampered or malformed token, **When** the user sends a request, **Then** the server returns a 401 Unauthorized response.

---

### User Story 4 - Transparent Token Refresh (Priority: P2)

When the user's access token expires, the client sends the refresh token to obtain a new token pair. The old refresh token is immediately invalidated (rotation). The user's session continues without interruption.

**Why this priority**: Token refresh enables long-lived sessions without compromising security. It depends on login and token validation (P1) being in place first, but is essential for a usable mobile app experience.

**Independent Test**: Can be tested by obtaining tokens, waiting for the access token to expire (or using a short-lived test token), then calling the refresh endpoint and verifying a new token pair is returned and the old refresh token no longer works.

**Acceptance Scenarios**:

1. **Given** a valid refresh token, **When** the client sends it to the refresh endpoint, **Then** the system returns a new access token and a new refresh token, and the old refresh token is invalidated.
2. **Given** an expired refresh token, **When** the client attempts to refresh, **Then** the system returns a 401 response.
3. **Given** a revoked refresh token, **When** the client attempts to refresh, **Then** the system returns a 401 response.

---

### User Story 5 - Reuse Detection (Priority: P2)

If a refresh token that has already been rotated (used and replaced) is presented again, the system detects this as a potential security compromise and revokes all refresh tokens for that user, forcing them to log in again.

**Why this priority**: Reuse detection is a critical security safeguard against stolen token replay. It builds on the refresh flow (P2) and protects users whose tokens may have been intercepted.

**Independent Test**: Can be tested by obtaining tokens, refreshing to get new tokens, then attempting to use the original (now-rotated) refresh token and verifying that all tokens for that user are revoked.

**Acceptance Scenarios**:

1. **Given** a refresh token that has already been rotated, **When** an attacker attempts to use it, **Then** the system revokes all refresh tokens for that user and returns a 401 response.
2. **Given** the legitimate user after a reuse detection event, **When** they attempt to use their current refresh token, **Then** the system rejects it (it was revoked) and the user must log in again.

---

### User Story 6 - Logout (Priority: P3)

A logged-in user explicitly logs out. The server revokes their refresh token so it cannot be used to obtain new tokens. The short-lived access token will naturally expire.

**Why this priority**: Logout is important for user control over their sessions, but the system remains secure without it since access tokens expire quickly and refresh tokens can be rotated. It is a lower priority than the core auth flows.

**Independent Test**: Can be tested by logging in, then calling the logout endpoint and verifying the refresh token can no longer be used.

**Acceptance Scenarios**:

1. **Given** a logged-in user with a valid refresh token, **When** they call the logout endpoint, **Then** the refresh token is revoked and cannot be used for token refresh.
2. **Given** a user who has logged out, **When** they attempt to refresh their token, **Then** the system returns a 401 response.

---

### User Story 7 - Profile Management (Priority: P3)

A logged-in user can view their profile information (email, username, role, account creation date), update their username, and change their password. Password changes require providing the current password for confirmation.

**Why this priority**: Profile management is a standard feature that improves user experience but is not required for the core auth flow to function. It depends on all P1 capabilities being in place.

**Independent Test**: Can be tested by logging in, viewing the profile, updating the username, and changing the password -- each operation can be verified independently.

**Acceptance Scenarios**:

1. **Given** a logged-in user, **When** they request their profile, **Then** the system returns their email, username, role, and account creation date.
2. **Given** a logged-in user, **When** they submit a new username, **Then** the system updates their username and confirms the change.
3. **Given** a logged-in user with the correct current password, **When** they submit a new password that meets strength requirements, **Then** the system updates their password.
4. **Given** a logged-in user with an incorrect current password, **When** they attempt to change their password, **Then** the system rejects the request with an error.
5. **Given** a logged-in user, **When** they submit a new password that does not meet strength requirements, **Then** the system rejects the request with a validation error.

---

### Edge Cases

- What happens when a user tries to register with an email that differs only in letter casing (e.g., `User@email.com` vs `user@email.com`)? The system treats email addresses as case-insensitive to prevent duplicate accounts.
- What happens when the refresh token is valid but the associated user account has been deleted or deactivated? The system rejects the refresh attempt with a 401 response.
- What happens when multiple refresh requests are made simultaneously with the same refresh token? Only the first request succeeds; subsequent requests trigger reuse detection.
- What happens when the user submits an invalid username (empty, whitespace-only, under 3 characters, over 30 characters, containing disallowed characters, or consisting solely of digits) during registration or profile update? The system rejects the request with a validation error.
- What happens when a user attempts to change their password to the same current password? The system accepts this (no restriction on reusing the same password).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow new users to create accounts with email, username, and password
- **FR-002**: System MUST validate email format and reject invalid email addresses
- **FR-003**: System MUST enforce password strength: minimum 8 characters containing both letters and digits
- **FR-003a**: System MUST validate usernames: 3–30 characters, Latin letters, digits, hyphens, and underscores only, cannot consist solely of digits
- **FR-004**: System MUST prevent duplicate accounts by enforcing unique email addresses (case-insensitive)
- **FR-005**: System MUST securely hash passwords before storage -- plaintext passwords are never persisted
- **FR-006**: System MUST return a token pair (access token + refresh token) upon successful registration
- **FR-007**: System MUST authenticate returning users with email and password, returning a token pair on success
- **FR-008**: System MUST return a generic error for invalid login attempts without revealing whether the email or password was incorrect
- **FR-009**: System MUST validate access tokens on every request to protected endpoints, checking signature, expiration, issuer, and audience
- **FR-010**: System MUST return a 401 Unauthorized response for missing, expired, tampered, or invalid tokens
- **FR-011**: System MUST issue a new token pair when a valid refresh token is presented
- **FR-012**: System MUST invalidate the old refresh token immediately upon issuing a new one (rotation)
- **FR-013**: System MUST detect reuse of a previously rotated refresh token and revoke all refresh tokens for the affected user
- **FR-014**: System MUST allow logged-in users to revoke their refresh token (logout)
- **FR-015**: System MUST allow logged-in users to view their profile (email, username, role, creation date)
- **FR-016**: System MUST allow logged-in users to update their username
- **FR-017**: System MUST allow logged-in users to change their password, requiring the current password for confirmation
- **FR-018**: System MUST store refresh tokens as hashed values -- raw tokens are never persisted
- **FR-019**: System MUST load security secrets from environment configuration, never from hardcoded values
- **FR-020**: Access tokens MUST expire after 15 minutes; refresh tokens MUST expire after 30 days
- **FR-021**: System MUST support two user roles: regular user and administrator. In this feature the role is stored only -- no endpoints or behaviors differ by role. Downstream features will use the role for access control
- **FR-022**: System MUST create the data schema automatically on application startup
- **FR-023**: System MUST provide all auth endpoints under a versioned path (`/api/v1/auth`)
- **FR-024**: System MUST support multiple concurrent sessions per user -- login or registration on one device MUST NOT invalidate tokens on other devices

### Key Entities

- **User**: Represents a registered account. Key attributes: unique identifier, email (unique, case-insensitive), username, hashed password, role (user or admin), creation timestamp, last-updated timestamp.
- **Refresh Token**: Represents an active or revoked session renewal credential. Key attributes: unique identifier, reference to owning user, hashed token value, expiration date, revocation status, creation timestamp.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new user can complete registration and access a protected resource in under 5 seconds (end-to-end flow)
- **SC-002**: A returning user can log in and access a protected resource in under 3 seconds
- **SC-003**: 100% of requests with invalid, expired, or revoked tokens are rejected with a 401 response -- zero false acceptances
- **SC-004**: Token refresh completes transparently; users can maintain an active session for 30 days without re-entering credentials
- **SC-005**: A compromised (replayed) refresh token triggers full session revocation within a single request cycle
- **SC-006**: The full auth lifecycle (register, access, token expiry, refresh, access again) passes as an automated integration test
- **SC-007**: The complete system (application + database) starts with a single command for local development
- **SC-008**: All seven auth endpoints are functional and return correct responses for both success and error cases

## Assumptions

- The primary client is a mobile application; web browser clients may use the same API but are not the primary design target for this phase.
- New accounts default to the "user" role. Admin role assignment is handled through a separate process (out of scope for this feature).
- Email verification (confirming email ownership) is out of scope for this feature. Registration succeeds immediately without email confirmation.
- Account lockout after repeated failed login attempts is out of scope for this phase. It may be added as a future enhancement.
- Rate limiting on auth endpoints is out of scope for this feature but is assumed to be handled at the infrastructure level (reverse proxy or API gateway).
- The refresh token is sent in the request body (not as an HTTP-only cookie), since the primary client is a mobile app.
- Username uniqueness is not enforced -- only email must be unique. Multiple users may share the same display name.
- Password reset (forgot password) via email is out of scope for this feature.
