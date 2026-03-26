# API Contract: Authentication Endpoints

**Base path**: `/api/v1/auth`
**Content-Type**: `application/json` (all requests and responses)

---

## 1. POST /register (Public)

Create a new user account and return a token pair.

**Request body**:
```json
{
  "email": "user@example.com",
  "username": "john_doe",
  "password": "Secret123"
}
```

**201 Created** — account created, user immediately authenticated:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "dGhpcyBpcyBhIHJhbmRvbSB0b2tlbg...",
  "expiresIn": 900
}
```

**400 Bad Request** — validation failure:
```json
{
  "error": "validation_error",
  "message": "Password must be at least 8 characters and contain both letters and digits"
}
```

**409 Conflict** — email already registered:
```json
{
  "error": "email_taken",
  "message": "An account with this email already exists"
}
```

---

## 2. POST /login (Public)

Authenticate with email and password, return a token pair.

**Request body**:
```json
{
  "email": "user@example.com",
  "password": "Secret123"
}
```

**200 OK** — credentials valid:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "dGhpcyBpcyBhIHJhbmRvbSB0b2tlbg...",
  "expiresIn": 900
}
```

**401 Unauthorized** — invalid credentials (generic, no field hints):
```json
{
  "error": "invalid_credentials",
  "message": "Invalid email or password"
}
```

---

## 3. POST /refresh (Public)

Exchange a valid refresh token for a new token pair. The old refresh token is immediately invalidated.

**Request body**:
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJhbmRvbSB0b2tlbg..."
}
```

**200 OK** — token rotated:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "bmV3IHJhbmRvbSB0b2tlbg...",
  "expiresIn": 900
}
```

**401 Unauthorized** — token expired, revoked, or reuse detected:
```json
{
  "error": "invalid_refresh_token",
  "message": "Refresh token is invalid or has been revoked"
}
```

---

## 4. POST /logout (Protected)

Revoke the refresh token for the current session.

**Headers**: `Authorization: Bearer <access_token>`

**Request body**:
```json
{
  "refreshToken": "dGhpcyBpcyBhIHJhbmRvbSB0b2tlbg..."
}
```

**200 OK** — token revoked:
```json
{
  "message": "Successfully logged out"
}
```

**401 Unauthorized** — missing or invalid access token.

---

## 5. GET /me (Protected)

Return the current user's profile.

**Headers**: `Authorization: Bearer <access_token>`

**200 OK**:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "username": "john_doe",
  "role": "user",
  "createdAt": "2026-03-26T10:30:00Z"
}
```

**401 Unauthorized** — missing or invalid access token.

---

## 6. PATCH /me (Protected)

Update the current user's username.

**Headers**: `Authorization: Bearer <access_token>`

**Request body**:
```json
{
  "username": "new_username"
}
```

**200 OK** — updated:
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "username": "new_username",
  "role": "user",
  "createdAt": "2026-03-26T10:30:00Z"
}
```

**400 Bad Request** — invalid username format.

**401 Unauthorized** — missing or invalid access token.

---

## 7. PUT /me/password (Protected)

Change the current user's password.

**Headers**: `Authorization: Bearer <access_token>`

**Request body**:
```json
{
  "currentPassword": "Secret123",
  "newPassword": "NewSecret456"
}
```

**200 OK** — password changed:
```json
{
  "message": "Password updated successfully"
}
```

**400 Bad Request** — new password does not meet strength requirements.

**401 Unauthorized** — missing/invalid access token OR incorrect current password:
```json
{
  "error": "invalid_password",
  "message": "Current password is incorrect"
}
```

---

## Common Response Patterns

### Token Pair Response

Returned by register, login, and refresh endpoints:

| Field | Type | Description |
|-------|------|-------------|
| accessToken | string | JWT signed with HS256, 15-minute expiry |
| refreshToken | string | Opaque Base64-encoded token, 30-day expiry |
| expiresIn | integer | Access token lifetime in seconds (900) |

### JWT Claims (Access Token)

| Claim | Value |
|-------|-------|
| sub | User UUID |
| iss | `f1backend` |
| aud | `f1backend-api` |
| role | `user` or `admin` |
| iat | Issued-at timestamp |
| exp | Expiration timestamp (iat + 900s) |

### Error Response

All error responses follow this structure:

| Field | Type | Description |
|-------|------|-------------|
| error | string | Machine-readable error code |
| message | string | Human-readable description |

### HTTP Status Code Summary

| Code | Meaning | Used by |
|------|---------|---------|
| 200 | Success | login, refresh, logout, get profile, update profile, change password |
| 201 | Created | register |
| 400 | Validation error | register, update profile, change password |
| 401 | Unauthorized | login (bad creds), refresh (bad token), all protected endpoints (bad/missing JWT) |
| 409 | Conflict | register (duplicate email) |
