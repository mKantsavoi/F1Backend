# Data Model: JWT Authentication & Authorization

**Feature**: 001-jwt-auth | **Date**: 2026-03-26

## Entities

### User

Represents a registered account in the system.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | Primary key, auto-generated | Immutable after creation |
| email | String | Unique, case-insensitive, max 255 chars | Stored lowercase for uniqueness enforcement |
| username | String | 3–30 chars, Latin letters/digits/hyphens/underscores, cannot be solely digits | Display name; not unique |
| passwordHash | String | Non-null, BCrypt hash | Never exposed outside infrastructure layer |
| role | Enum(USER, ADMIN) | Non-null, default USER | No behavioral differentiation in this feature |
| createdAt | Timestamp | Non-null, set on creation | Immutable |
| updatedAt | Timestamp | Non-null, updated on any mutation | Auto-updated |

**Indexes**:
- Unique index on `email` (lowercase)

**Validation rules** (enforced at adapter layer before persistence):
- Email: valid format per RFC 5321 simplified pattern
- Password (input, not stored): minimum 8 characters, must contain at least one letter and one digit
- Username: 3–30 characters, pattern `^[a-zA-Z0-9_-]+$`, must contain at least one non-digit character

---

### RefreshToken

Represents an active or revoked session renewal credential.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | UUID | Primary key, auto-generated | Immutable |
| userId | UUID | Foreign key → User.id, non-null | ON DELETE CASCADE |
| tokenHash | String | Non-null, SHA-256 hex digest | Hash of the raw token; raw value never stored |
| expiresAt | Timestamp | Non-null | 30 days from creation |
| revoked | Boolean | Non-null, default false | Set to true on rotation, logout, or reuse detection |
| createdAt | Timestamp | Non-null, set on creation | Immutable |

**Indexes**:
- Index on `tokenHash` (for lookup during refresh)
- Index on `userId` (for revoke-all-for-user queries)

---

## State Transitions

### RefreshToken Lifecycle

```
                  ┌──────────┐
    Created ────► │  ACTIVE  │
                  └────┬─────┘
                       │
          ┌────────────┼────────────┐
          │            │            │
          ▼            ▼            ▼
     [Rotated]    [Logged out]  [Expired]
          │            │            │
          ▼            ▼            ▼
      ┌───────────────────────────────┐
      │           REVOKED             │
      └───────────────────────────────┘
                       │
                 [Reuse detected]
                       │
                       ▼
              ┌─────────────────┐
              │ ALL user tokens │
              │    REVOKED      │
              └─────────────────┘
```

**Transitions**:
1. **Created → Active**: Token is generated during registration, login, or refresh
2. **Active → Revoked (rotation)**: Token is used for refresh; new token pair issued; old token marked revoked
3. **Active → Revoked (logout)**: User explicitly logs out; token marked revoked
4. **Active → Revoked (reuse detection)**: A previously-rotated token is presented; ALL tokens for that user are revoked
5. **Active → Expired**: Token's `expiresAt` has passed; treated as invalid on any use attempt

---

## Relationships

```
User (1) ──────► (N) RefreshToken
```

- A user can have multiple active refresh tokens (concurrent sessions on different devices)
- Deleting a user cascades to delete all their refresh tokens
- Each refresh token belongs to exactly one user
