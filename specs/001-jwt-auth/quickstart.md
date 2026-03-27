# Quickstart: JWT Authentication & Authorization

**Feature**: 001-jwt-auth | **Date**: 2026-03-26

## Prerequisites

- JDK 21+
- Docker and Docker Compose (for PostgreSQL)
- Gradle 9.3+ (or use the included wrapper `./gradlew`)

## Run Locally

### 1. Start the database

```bash
docker compose up -d db
```

This starts PostgreSQL on port 5432. The application creates the schema automatically on startup.

### 2. Set environment variables

```bash
export JWT_SECRET="your-256-bit-secret-change-in-production"
export DB_URL="jdbc:postgresql://localhost:5432/f1backend"
export DB_USER="f1backend"
export DB_PASSWORD="f1backend"
```

### 3. Start the application

```bash
./gradlew run
```

The server starts on `http://localhost:8080`.

### 4. Run everything with one command (Docker Compose)

```bash
docker compose up
```

This starts both PostgreSQL and the application. The API is available at `http://localhost:8080`.

## Verify It Works

### Register a new user

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","username":"testuser","password":"Secret123"}'
```

Expected: `201 Created` with `accessToken`, `refreshToken`, and `expiresIn`.

### Access a protected endpoint

```bash
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <access_token_from_above>"
```

Expected: `200 OK` with user profile.

### Refresh the token

```bash
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refresh_token_from_above>"}'
```

Expected: `200 OK` with new token pair.

## Run Tests

```bash
# Unit tests only
./gradlew test

# All tests including integration (requires Docker for testcontainers)
./gradlew test
```

Integration tests use testcontainers to spin up a PostgreSQL instance automatically — no manual database setup needed.

## Project Structure

```
src/main/kotlin/com/blaizmiko/
├── domain/                    # Pure entities, repository interfaces
│   ├── model/
│   │   ├── User.kt
│   │   └── RefreshToken.kt
│   └── repository/
│       ├── UserRepository.kt
│       └── RefreshTokenRepository.kt
├── usecase/                   # Business rules (no framework deps)
│   ├── RegisterUser.kt
│   ├── LoginUser.kt
│   ├── RefreshTokens.kt
│   ├── LogoutUser.kt
│   ├── GetProfile.kt
│   ├── UpdateProfile.kt
│   └── ChangePassword.kt
├── adapter/                   # Ktor routes, DTOs, validation
│   ├── route/
│   │   └── AuthRoutes.kt
│   └── dto/
│       ├── AuthRequests.kt
│       └── AuthResponses.kt
├── infrastructure/            # Database, JWT, hashing, config
│   ├── persistence/
│   │   ├── DatabaseFactory.kt
│   │   ├── table/
│   │   │   ├── UsersTable.kt
│   │   │   └── RefreshTokensTable.kt
│   │   └── repository/
│   │       ├── ExposedUserRepository.kt
│   │       └── ExposedRefreshTokenRepository.kt
│   ├── security/
│   │   ├── JwtProvider.kt
│   │   ├── PasswordHasher.kt
│   │   └── TokenHasher.kt
│   └── config/
│       └── AppConfig.kt
├── Application.kt
└── Routing.kt
```
