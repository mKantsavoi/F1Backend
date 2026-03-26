# F1 BACKEND API — BRIDGE SERVICE

> Technical Documentation for Backend Development on Kotlin / Ktor
>
> Version 1.2 | March 2026

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Open F1 API Research](#2-open-f1-api-research)
3. [Authentication & Authorization](#3-authentication--authorization)
4. [Favorites System](#4-favorites-system)
5. [API Source Strategy](#5-api-source-strategy)
6. [Data Endpoints Specification](#6-data-endpoints-specification)
7. [Summary Endpoint Table](#7-summary-endpoint-table)
8. [Next Steps](#8-next-steps)

---

## 1. Project Overview

### 1.1 Purpose

This document describes the architecture and specification of a backend bridge service that acts as an intermediary layer between a mobile application (Android/iOS) and external Formula 1 data sources. The service aggregates data from multiple open APIs, enriches it with AI-generated content, and exposes a unified, authenticated REST API for the mobile client.

### 1.2 Tech Stack

- **Language:** Kotlin 2.0+
- **Framework:** Ktor (server + client)
- **Serialization:** kotlinx.serialization
- **Database:** PostgreSQL + Exposed ORM
- **Auth:** JWT (Access + Refresh tokens) / BCrypt for passwords
- **Caching:** Redis (optional) / Caffeine (in-memory)
- **Containerization:** Docker + Docker Compose
- **CI/CD:** GitHub Actions

### 1.3 High-Level Architecture

The mobile app communicates exclusively with our backend. The backend decides where to fetch data from:

- **Proxying:** Requests are forwarded to OpenF1 / Jolpica, responses are cached and returned to the client.
- **Own data:** News, articles, AI-generated track descriptions, driver biographies — stored in our database.
- **Aggregation:** Data from multiple sources is combined into a single response (e.g., race results + telemetry + weather).
- **All protected endpoints** require a JWT Access Token in the `Authorization` header.

---

## 2. Open F1 API Research

### 2.1 OpenF1 (api.openf1.org)

- **URL:** `https://api.openf1.org/v1/`
- **Data from:** 2023 season onwards
- **Authentication:** Not required (for historical data)
- **Rate limit:** 3 req/s, 30 req/min (free tier)
- **Format:** JSON / CSV
- **License:** Educational and non-commercial use

OpenF1 is the best source for live telemetry, car positions, team radio, and weather data. Data updates with ~3 second delay from real time. It is the only open API providing real telemetry (speed, RPM, DRS, brake, throttle).

| Endpoint | Description | Value |
|---|---|---|
| `/car_data` | Telemetry: speed, RPM, DRS, throttle, brake, gear (~3.7 Hz) | **HIGH** — unique data |
| `/drivers` | Driver info: name, photo, team, color, number | MEDIUM |
| `/intervals` | Gaps between drivers and to the leader (race only) | **HIGH** — live data |
| `/laps` | Lap times, sector times, mini-sectors, speed traps | **HIGH** — key data |
| `/location` | Car coordinates on track (x, y, z) ~3.7 Hz | **HIGH** — for track map |
| `/meetings` | Grand Prix info: country, name, dates, circuit | MEDIUM |
| `/overtakes` | Overtake data | MEDIUM |
| `/pit` | Pit stop times, duration, lap number | **HIGH** |
| `/position` | Driver position on track in real time | **HIGH** |
| `/race_control` | Race direction messages: flags, penalties, Safety Car | **HIGH** |
| `/sessions` | Session list: practices, qualifying, race, sprint | MEDIUM |
| `/session_result` | Final session result (positions, times) | **HIGH** |
| `/starting_grid` | Starting grid | MEDIUM |
| `/stints` | Stints: tire compound, laps on set | **HIGH** — strategy |
| `/team_radio` | Team radio (audio file URLs) | **HIGH** — unique content |
| `/weather` | Track weather: temperature, humidity, wind, rain | MEDIUM |
| `/championship_drivers` | Driver championship standings (beta) | MEDIUM |
| `/championship_teams` | Team championship standings (beta) | MEDIUM |

### 2.2 Jolpica F1 (api.jolpi.ca)

- **URL:** `https://api.jolpi.ca/ergast/f1/`
- **Data from:** 1950 season onwards
- **Authentication:** Not required
- **Rate limit:** 4 req/s, 500 req/hour
- **Format:** JSON
- **License:** Apache 2.0 (open source)

Jolpica is the successor to the legendary Ergast API (shut down end of 2024). Contains the complete history of F1 since 1950: all seasons, races, results, qualifying, pit stops, laps. Best source for historical data and championship standings.

| Endpoint | Description | Value |
|---|---|---|
| `/circuits` | All F1 circuits: name, country, coordinates, Wikipedia link | **HIGH** — circuit database |
| `/constructors` | All teams (constructors): name, nationality | **HIGH** — team database |
| `/constructorstandings` | Constructors' Championship table by season/round | **HIGH** |
| `/drivers` | All drivers: name, DOB, nationality, number, code | **HIGH** — driver database |
| `/driverstandings` | Drivers' Championship table by season/round | **HIGH** |
| `/laps` | Lap times for all drivers (from 1996) | MEDIUM — also in OpenF1 |
| `/pitstops` | Pit stops: lap, stop time, total time | MEDIUM |
| `/qualifying` | Qualifying results (Q1, Q2, Q3) | **HIGH** |
| `/races` | Race schedule: date, time, circuit, GP name | **HIGH** — schedule |
| `/results` | Race results: position, time, points, status, fastest lap | **HIGH** |
| `/seasons` | List of all F1 seasons | LOW |
| `/sprint` | Sprint race results | MEDIUM |
| `/status` | Finish status reference (Finished, Retired, +1 Lap...) | LOW |

### 2.3 F1 API (f1api.dev)

- **URL:** `https://f1api.dev/api/`
- **Data:** Historical + current season
- **Authentication:** Not required
- **Format:** JSON

A relatively new community API. Provides data on drivers, teams, seasons, results, standings, and circuits. Convenient URL structure, has an SDK. Good alternative to Jolpica but a less mature project. Recommended as a fallback source.

### 2.4 Comparison Matrix

| Criterion | OpenF1 | Jolpica | f1api.dev |
|---|---|---|---|
| Historical data | From 2023 | From 1950 | From 1950 |
| Live telemetry | **YES** | No | No |
| Car positions (GPS) | **YES** | No | No |
| Team radio | **YES** | No | No |
| Race results | Yes (from 2023) | **Yes (full history)** | Yes |
| Qualifying | No separate endpoint | **YES** | Yes |
| Season schedule | Meetings | **Yes (complete)** | Yes |
| Driver championship | Beta (from 2023) | **Yes (from 1950)** | Yes |
| Constructor championship | Beta (from 2023) | **Yes (from 1958)** | Yes |
| Track weather | **YES** | No | No |
| Pit stops (detailed) | **YES** | Yes | No |
| Stints (tires) | **YES** | No | No |
| Rate limit | 3 req/s | 4 req/s, 500/h | Not specified |
| Project maturity | High | High | Medium |

---

## 3. Authentication & Authorization

### 3.1 General Scheme

The backend uses JWT (JSON Web Token) with a token pair: a short-lived Access Token for API access and a long-lived Refresh Token for renewing the pair without re-login. Signing algorithm — HS256 (HMAC-SHA256) with a secret key. Passwords are stored in the database as BCrypt hashes.

### 3.2 Token Lifecycle (Flow)

1. Client sends `POST /api/v1/auth/register` with email and password → account is created, token pair returned.
2. Client sends `POST /api/v1/auth/login` with email and password → server verifies BCrypt hash, returns token pair.
3. Client saves both tokens in secure storage (EncryptedSharedPreferences / DataStore).
4. Client sends requests to protected endpoints with header: `Authorization: Bearer <access_token>`
5. When Access Token expires, server returns `401 Unauthorized`.
6. Client automatically sends `POST /api/v1/auth/refresh` with refresh_token → receives new token pair.
7. Client retries the original request with the new access_token — transparent to the user.
8. If Refresh Token has also expired → user is redirected to the login screen.

### 3.3 Token Parameters

| Parameter | Access Token | Refresh Token |
|---|---|---|
| Algorithm | HS256 (HMAC-SHA256) | HS256 (HMAC-SHA256) |
| Lifetime | 15 minutes | 30 days |
| Payload claims | sub (userId), email, role, iss, exp, iat | sub (userId), tokenType=refresh, iss, exp, iat |
| Storage (client) | In-memory / DataStore | EncryptedSharedPreferences |
| Storage (server) | Not stored (stateless) | Hash in `refresh_tokens` table |
| Revocation | No (short TTL) | Yes (delete from DB) |

### 3.4 Auth Endpoints

#### POST /api/v1/auth/register

**Description:** Register a new user. Password is hashed via BCrypt and saved to DB.
**Authentication:** Not required (public)

```
Request body:
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "username": "max_fan"
}

Response 201 Created:
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 900
}

Response 409 Conflict:
{ "error": "User with this email already exists" }

Response 422 Unprocessable Entity:
{ "error": "Password must be at least 8 characters" }
```

#### POST /api/v1/auth/login

**Description:** Login with email + password. Verifies BCrypt password hash, returns token pair.
**Authentication:** Not required (public)

```
Request body:
{ "email": "user@example.com", "password": "SecurePass123!" }

Response 200 OK:
{
  "userId": "550e8400-...",
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "expiresIn": 900
}

Response 401 Unauthorized:
{ "error": "Invalid email or password" }
```

#### POST /api/v1/auth/refresh

**Description:** Refresh token pair. Old refresh token is invalidated (rotation), new pair is issued.
**Authentication:** Not required (public, but requires a valid refresh token in body)

```
Request body:
{ "refreshToken": "eyJhbGciOi..." }

Response 200 OK:
{
  "accessToken": "eyJhbGciOi... (new)",
  "refreshToken": "eyJhbGciOi... (new)",
  "expiresIn": 900
}

Response 401 Unauthorized:
{ "error": "Refresh token is invalid or expired" }
```

#### POST /api/v1/auth/logout

**Description:** Logout. Deletes refresh token from DB (invalidates session).
**Authentication:** Bearer Access Token

```
Response 200 OK:
{ "message": "Successfully logged out" }
```

#### GET /api/v1/auth/me

**Description:** Get current user profile by token.
**Authentication:** Bearer Access Token

```
Response 200 OK:
{
  "userId": "550e8400-...",
  "email": "user@example.com",
  "username": "max_fan",
  "role": "user",
  "createdAt": "2026-03-01T10:00:00Z"
}
```

#### PUT /api/v1/auth/me

**Description:** Update profile (username).
**Authentication:** Bearer Access Token

```
Request body:
{ "username": "new_name" }
```

#### PUT /api/v1/auth/password

**Description:** Change password. Requires current password for confirmation.
**Authentication:** Bearer Access Token

```
Request body:
{ "currentPassword": "OldPass123!", "newPassword": "NewPass456!" }
```

### 3.5 Access Control Matrix

| Endpoint | Access | Comment |
|---|---|---|
| POST /auth/register | Public | New user registration |
| POST /auth/login | Public | Login |
| POST /auth/refresh | Public | Requires valid refresh token in body |
| POST /auth/logout | JWT Required | Session invalidation |
| GET /auth/me | JWT Required | User profile |
| PUT /auth/me | JWT Required | Profile editing |
| PUT /auth/password | JWT Required | Password change |
| GET /drivers, /teams, ... | JWT Required | All data endpoints require token |
| GET /live/** | JWT Required | Live data requires token |

### 3.6 Auth Database Tables

| Table | Fields | Description |
|---|---|---|
| `users` | id (UUID PK), email (UNIQUE), username, password_hash, role (user/admin), created_at, updated_at | Application users |
| `refresh_tokens` | id (UUID PK), user_id (FK → users), token_hash (SHA-256), expires_at, created_at, is_revoked | Refresh token storage for validation and revocation |

> **Important:** The `refresh_tokens` table stores a SHA-256 hash of the token, not the token itself. This protects against leaks if the DB is compromised. On each refresh, the old token is marked as `is_revoked` and a new one is created (token rotation).

### 3.7 Server Implementation (Ktor)

#### 3.7.1 Dependencies (build.gradle.kts)

```kotlin
implementation("io.ktor:ktor-server-auth:$ktor_version")
implementation("io.ktor:ktor-server-auth-jwt:$ktor_version")
implementation("org.mindrot:jbcrypt:0.4")
```

#### 3.7.2 Configuration (application.conf)

```hocon
jwt {
    secret = "your-256-bit-secret"  // MINIMUM 32 characters!
    issuer = "f1-backend"
    audience = "f1-mobile-app"
    realm = "F1 Backend API"
    accessTokenExpMs = 900000       // 15 minutes
    refreshTokenExpMs = 2592000000   // 30 days
}
```

#### 3.7.3 JwtService.kt — Token Generation & Verification

```kotlin
class JwtService(private val config: JwtConfig) {

    private val algorithm = Algorithm.HMAC256(config.secret)

    fun generateAccessToken(user: User): String = JWT.create()
        .withSubject(user.id.toString())
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .withClaim("email", user.email)
        .withClaim("role", user.role.name)
        .withExpiresAt(Date(System.currentTimeMillis() + config.accessTokenExpMs))
        .sign(algorithm)

    fun generateRefreshToken(user: User): String = JWT.create()
        .withSubject(user.id.toString())
        .withIssuer(config.issuer)
        .withClaim("tokenType", "refresh")
        .withExpiresAt(Date(System.currentTimeMillis() + config.refreshTokenExpMs))
        .sign(algorithm)

    fun getVerifier(): JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()
}
```

#### 3.7.4 Installing the Authentication Plugin in Ktor

```kotlin
fun Application.configureAuth(jwtService: JwtService) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtService.config.realm
            verifier(jwtService.getVerifier())
            validate { credential ->
                val email = credential.payload.getClaim("email").asString()
                val role = credential.payload.getClaim("role").asString()
                if (email != null && role != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized,
                    mapOf("error" to "Token is invalid or expired"))
            }
        }
    }
}
```

#### 3.7.5 AuthRoutes.kt — Auth Routes

```kotlin
fun Route.authRoutes(authService: AuthService, jwtService: JwtService) {
    route("/api/v1/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val user = authService.register(request)
            val tokens = jwtService.generateTokenPair(user)
            authService.saveRefreshToken(user.id, tokens.refreshToken)
            call.respond(HttpStatusCode.Created, tokens)
        }
        post("/login") {
            val request = call.receive<LoginRequest>()
            val user = authService.authenticate(request.email, request.password)
                ?: throw AuthenticationException("Invalid email or password")
            val tokens = jwtService.generateTokenPair(user)
            authService.saveRefreshToken(user.id, tokens.refreshToken)
            call.respond(tokens)
        }
        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            val newTokens = authService.refreshTokens(request.refreshToken)
            call.respond(newTokens)
        }
        authenticate("auth-jwt") {
            post("/logout") {
                val userId = call.principal<JWTPrincipal>()!!
                    .payload.subject.let { UUID.fromString(it) }
                authService.revokeAllTokens(userId)
                call.respond(mapOf("message" to "Logged out"))
            }
            get("/me") {
                val userId = call.principal<JWTPrincipal>()!!
                    .payload.subject.let { UUID.fromString(it) }
                call.respond(authService.getProfile(userId))
            }
        }
    }
}
```

#### 3.7.6 Protecting Data Endpoints

```kotlin
fun Application.configureRouting(/* ... */) {
    routing {
        authRoutes(authService, jwtService)       // public + protected
        authenticate("auth-jwt") {
            driverRoutes(driverService)            // /api/v1/drivers/**
            teamRoutes(teamService)                // /api/v1/teams/**
            circuitRoutes(circuitService)          // /api/v1/circuits/**
            scheduleRoutes(scheduleService)        // /api/v1/schedule/**
            raceRoutes(raceService)                // /api/v1/races/**
            standingsRoutes(standingsService)      // /api/v1/standings/**
            liveRoutes(liveService)                // /api/v1/live/**
            newsRoutes(newsService)                // /api/v1/news/**
            searchRoutes(searchService)            // /api/v1/search
            favoritesRoutes(favoritesService)      // /api/v1/favorites/**
        }
    }
}
```

### 3.8 Mobile Client Implementation (Android / Ktor Client)

#### 3.8.1 Token Storage

On Android, tokens must be stored encrypted. Use EncryptedSharedPreferences or DataStore with encryption:

```kotlin
class TokenManager(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        "auth_prefs", MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveTokens(access: String, refresh: String) {
        prefs.edit().putString("access_token", access)
            .putString("refresh_token", refresh).apply()
    }
    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)
    fun clearTokens() = prefs.edit().clear().apply()
}
```

#### 3.8.2 Ktor Client with Automatic Refresh

Ktor Client has built-in Bearer authentication support with automatic token refresh on 401. This is the key mechanism for the mobile app:

```kotlin
val httpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }

    install(Auth) {
        bearer {
            loadTokens {
                val access = tokenManager.getAccessToken() ?: return@loadTokens null
                val refresh = tokenManager.getRefreshToken() ?: return@loadTokens null
                BearerTokens(access, refresh)
            }
            refreshTokens {
                // Automatically triggered on 401
                val response = client.post("$BASE_URL/api/v1/auth/refresh") {
                    markAsRefreshTokenRequest()  // IMPORTANT!
                    contentType(ContentType.Application.Json)
                    setBody(RefreshRequest(oldTokens?.refreshToken ?: ""))
                }
                if (response.status == HttpStatusCode.OK) {
                    val tokens = response.body<TokenResponse>()
                    tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                    BearerTokens(tokens.accessToken, tokens.refreshToken)
                } else {
                    tokenManager.clearTokens()
                    // Navigate to login screen
                    null
                }
            }
            sendWithoutRequest { request ->
                request.url.host == "your-backend.com"
            }
        }
    }
}
```

#### 3.8.3 UI Flow: Login / Register Screens

| Screen | Elements | Action |
|---|---|---|
| Splash | Logo + loader | Check for saved token → `GET /auth/me` → if OK, navigate to Home |
| Login | Email + Password + Login button | `POST /auth/login` → save tokens → Home |
| Register | Email + Password + Username + Register button | `POST /auth/register` → save tokens → Home |
| Profile | Name, email, Logout button | `GET /auth/me` + `PUT /auth/me` + `POST /auth/logout` |

### 3.9 Security Recommendations

- **BCrypt for passwords:** cost factor = 12 (balance of security and speed). NEVER store passwords in plaintext.
- **Refresh Token Rotation:** on each refresh the old token is invalidated — protection against replay attacks.
- **Reuse Detection:** if the server receives an already-used refresh token → revoke ALL user tokens (possible compromise).
- **JWT Secret:** store in environment variables (env), NOT in code. Minimum 256 bits (32 characters).
- **HTTPS only:** all requests only via HTTPS. Configure HSTS in production.
- **Rate limiting on /auth/*:** max 5 login attempts per minute per IP for brute-force protection.
- **Email validation:** check email format on the server (don't trust the client).
- **Password validation:** minimum 8 characters, letters + digits. Validate on both client (for UX) and server (for security).
- **Android:** EncryptedSharedPreferences for tokens, NEVER regular SharedPreferences.
- **iOS:** Keychain Services for token storage.

---

## 4. Favorites System

### 4.1 Concept

A user can add any number of drivers and teams to their favorites. This is not a single "favorite" — it is a list: you can support multiple teams and root for several drivers. This enables:

- **Quick access** to favorite drivers/teams on the home screen.
- **Personalized feed:** news, results, notifications filtered by favorites.
- **Push notifications** about favorite events (future feature).
- **Toggle mechanic:** a single request adds or removes from favorites (idempotent).

### 4.2 Database Tables

| Table | Fields | Description |
|---|---|---|
| `favorite_drivers` | id (UUID PK), user_id (FK → users), driver_id (VARCHAR), created_at, UNIQUE(user_id, driver_id) | User ↔ favorite driver junction |
| `favorite_teams` | id (UUID PK), user_id (FK → users), team_id (VARCHAR), created_at, UNIQUE(user_id, team_id) | User ↔ favorite team junction |

Classic many-to-many relationship via junction tables. The UNIQUE constraint on the pair `(user_id, driver_id/team_id)` ensures a user cannot add the same driver/team twice.

### 4.3 Exposed ORM (Kotlin Tables)

```kotlin
object FavoriteDrivers : UUIDTable("favorite_drivers") {
    val userId = reference("user_id", Users)
    val driverId = varchar("driver_id", 64)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    init { uniqueIndex(userId, driverId) }
}

object FavoriteTeams : UUIDTable("favorite_teams") {
    val userId = reference("user_id", Users)
    val teamId = varchar("team_id", 64)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    init { uniqueIndex(userId, teamId) }
}
```

### 4.4 Favorites Endpoints

All endpoints require JWT Access Token. User ID is extracted from the token automatically.

#### GET /api/v1/favorites/drivers

**Description:** List of all favorite drivers for the current user. Returns full driver cards (not just IDs) for instant rendering on the client.
**Cache:** None (personalized data)

```json
{
  "drivers": [
    {
      "driverId": "max_verstappen",
      "number": 1,
      "code": "VER",
      "firstName": "Max",
      "lastName": "Verstappen",
      "photoUrl": "https://...",
      "teamName": "Red Bull Racing",
      "teamColor": "3671C6",
      "addedAt": "2026-03-15T12:00:00Z"
    },
    { "driverId": "leclerc", "..." : "..." }
  ]
}
```

#### POST /api/v1/favorites/drivers/{driverId}

**Description:** Add a driver to favorites. Idempotent: a repeated request doesn't create a duplicate, returns 200.

```
Response 201 Created / 200 OK (already exists):
{ "driverId": "max_verstappen", "addedAt": "2026-03-26T14:00:00Z" }

Response 404 Not Found:
{ "error": "Driver not found" }
```

#### DELETE /api/v1/favorites/drivers/{driverId}

**Description:** Remove a driver from favorites. Idempotent: if already removed — returns 204.

```
Response 204 No Content
```

#### GET /api/v1/favorites/drivers/check/{driverId}

**Description:** Check if a driver is in favorites. Needed for displaying the heart/star icon state on a driver card.

```json
{ "isFavorite": true }
```

#### GET /api/v1/favorites/teams

**Description:** List of all favorite teams with full cards (name, color, logo, drivers).

```json
{
  "teams": [
    {
      "teamId": "red_bull",
      "name": "Red Bull Racing",
      "color": "3671C6",
      "logoUrl": "https://...",
      "drivers": [
        { "driverId": "max_verstappen", "code": "VER", "number": 1 },
        { "driverId": "lawson", "code": "LAW", "number": 30 }
      ],
      "addedAt": "2026-03-10T08:00:00Z"
    }
  ]
}
```

#### POST /api/v1/favorites/teams/{teamId}

**Description:** Add a team to favorites. Idempotent.

```
Response 201 Created / 200 OK (already exists):
{ "teamId": "red_bull", "addedAt": "2026-03-26T14:00:00Z" }
```

#### DELETE /api/v1/favorites/teams/{teamId}

**Description:** Remove a team from favorites. Idempotent.

```
Response 204 No Content
```

#### GET /api/v1/favorites/teams/check/{teamId}

**Description:** Check if a team is in favorites.

```json
{ "isFavorite": false }
```

#### GET /api/v1/favorites/feed

**Description:** Personalized feed: latest results, championship positions, and news for favorite drivers/teams. Ideal for the app's home screen.
**Source:** DB (favorites) + Jolpica (standings, last result) + DB (news)
**Cache:** 30 seconds (per-user)

```json
{
  "favoriteDrivers": [
    {
      "driverId": "max_verstappen", "code": "VER",
      "championshipPosition": 1, "championshipPoints": 156,
      "lastRace": { "name": "Australian GP", "position": 1, "points": 25 }
    }
  ],
  "favoriteTeams": [
    {
      "teamId": "red_bull", "name": "Red Bull Racing",
      "championshipPosition": 2, "championshipPoints": 280
    }
  ],
  "relevantNews": [
    { "id": "...", "title": "Verstappen dominates in Melbourne", "publishedAt": "..." }
  ]
}
```

### 4.5 Server Implementation (Ktor)

#### 4.5.1 FavoritesRoutes.kt

```kotlin
fun Route.favoritesRoutes(favService: FavoritesService) {
    authenticate("auth-jwt") {
        route("/api/v1/favorites") {

            // === Drivers ===
            get("/drivers") {
                val userId = call.userId()  // extracted from JWT
                call.respond(favService.getFavoriteDrivers(userId))
            }
            post("/drivers/{driverId}") {
                val userId = call.userId()
                val driverId = call.parameters["driverId"]!!
                val result = favService.addFavoriteDriver(userId, driverId)
                call.respond(
                    if (result.created) HttpStatusCode.Created
                    else HttpStatusCode.OK, result
                )
            }
            delete("/drivers/{driverId}") {
                val userId = call.userId()
                val driverId = call.parameters["driverId"]!!
                favService.removeFavoriteDriver(userId, driverId)
                call.respond(HttpStatusCode.NoContent)
            }
            get("/drivers/check/{driverId}") {
                val userId = call.userId()
                val driverId = call.parameters["driverId"]!!
                call.respond(mapOf("isFavorite" to
                    favService.isDriverFavorite(userId, driverId)))
            }

            // === Teams — analogous ===
            get("/teams") { /* ... */ }
            post("/teams/{teamId}") { /* ... */ }
            delete("/teams/{teamId}") { /* ... */ }
            get("/teams/check/{teamId}") { /* ... */ }

            // === Personalized feed ===
            get("/feed") {
                val userId = call.userId()
                call.respond(favService.getPersonalizedFeed(userId))
            }
        }
    }
}
```

#### 4.5.2 Helper: Extracting userId from JWT

```kotlin
fun ApplicationCall.userId(): UUID {
    val principal = principal<JWTPrincipal>()
        ?: throw AuthenticationException("Not authenticated")
    return UUID.fromString(principal.payload.subject)
}
```

#### 4.5.3 FavoritesService.kt (Logic)

```kotlin
class FavoritesService(
    private val db: DatabaseFactory,
    private val driverService: DriverService,
    private val standingsService: StandingsService,
) {
    suspend fun addFavoriteDriver(userId: UUID, driverId: String): FavResult {
        // 1. Check if the driver exists
        driverService.getDriverOrThrow(driverId)
        // 2. INSERT ... ON CONFLICT DO NOTHING
        return db.dbQuery {
            val existing = FavoriteDrivers.select {
                (FavoriteDrivers.userId eq userId) and
                (FavoriteDrivers.driverId eq driverId)
            }.singleOrNull()
            if (existing != null) FavResult(driverId, false)
            else {
                FavoriteDrivers.insert {
                    it[FavoriteDrivers.userId] = userId
                    it[FavoriteDrivers.driverId] = driverId
                }
                FavResult(driverId, true)
            }
        }
    }
    // removeFavoriteDriver, isDriverFavorite, getFavoriteDrivers ...
    // Analogous for teams
}
```

### 4.6 Mobile Client Implementation

#### 4.6.1 UX Patterns

- **Heart/star icon** on driver and team cards. Tap = toggle (POST/DELETE).
- **Optimistic UI:** instantly change icon state, send request in background. Rollback on error.
- **Favorites tab** in bottom navigation — quick access to all favorites.
- **Home screen:** dedicated blocks "Your Drivers" and "Your Teams" with latest results (from `/favorites/feed`).
- **Empty state:** when no favorites — beautiful screen with a call to action "Pick your driver!"

#### 4.6.2 ViewModel Example (Jetpack Compose)

```kotlin
class DriverDetailViewModel(
    private val favoritesRepo: FavoritesRepository
) : ViewModel() {

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite

    fun checkFavorite(driverId: String) = viewModelScope.launch {
        _isFavorite.value = favoritesRepo.isDriverFavorite(driverId)
    }

    fun toggleFavorite(driverId: String) = viewModelScope.launch {
        val current = _isFavorite.value
        _isFavorite.value = !current  // optimistic
        try {
            if (current) favoritesRepo.removeDriver(driverId)
            else favoritesRepo.addDriver(driverId)
        } catch (e: Exception) {
            _isFavorite.value = current  // rollback
        }
    }
}
```

#### 4.6.3 Composable Example (Favorite Button)

```kotlin
@Composable
fun FavoriteButton(isFavorite: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite
                          else Icons.Outlined.FavoriteBorder,
            contentDescription = "Toggle favorite",
            tint = if (isFavorite) Color(0xFFC0392B) else Color.Gray
        )
    }
}
```

### 4.7 Favorites Endpoints Summary

| Method | Endpoint | Description | Response |
|---|---|---|---|
| GET | /favorites/drivers | List of favorite drivers (full cards) | 200 |
| POST | /favorites/drivers/{id} | Add driver (idempotent) | 201/200 |
| DELETE | /favorites/drivers/{id} | Remove driver (idempotent) | 204 |
| GET | /favorites/drivers/check/{id} | Check isFavorite | 200 |
| GET | /favorites/teams | List of favorite teams (full cards) | 200 |
| POST | /favorites/teams/{id} | Add team (idempotent) | 201/200 |
| DELETE | /favorites/teams/{id} | Remove team (idempotent) | 204 |
| GET | /favorites/teams/check/{id} | Check isFavorite | 200 |
| GET | /favorites/feed | Personalized feed for Home | 200 |

---

## 5. API Source Strategy

### 5.1 Source Distribution

| Data Type | Source | Backend Strategy |
|---|---|---|
| Driver/team/circuit catalog | Jolpica + AI | Load into DB, enrich with AI descriptions |
| Race results (history) | Jolpica | Cache forever (immutable) |
| Schedule + championships | Jolpica | Cache 1–6h, update after race |
| Live telemetry / positions / radio | OpenF1 | Proxy directly |
| News, reviews, previews | AI generation | Store in DB |

### 5.2 Caching Strategy

| Data Category | Cache TTL | Invalidation Strategy |
|---|---|---|
| Past season results | Forever | Never (immutable) |
| Driver/team catalog | 24 hours | On source update |
| Current season schedule | 6 hours | On source change |
| Current race results | 5 minutes | After final protocol published |
| Championship standings | 1 hour | After each round |
| Weather | 30 seconds | By TTL |
| Live telemetry | No cache | Real-time proxying |
| AI content (news) | Forever | On manual update |

---

## 6. Data Endpoints Specification

> All endpoints below require JWT Access Token in the header: `Authorization: Bearer <token>`

### 6.1 Drivers

**GET /api/v1/drivers**
Source: DB (Jolpica + OpenF1 + AI) | Cache: 24h

**GET /api/v1/drivers/{id}**
Source: DB + Jolpica (career stats) | Cache: 24h

**GET /api/v1/drivers/{id}/results**
Source: Jolpica | Cache: 1h / ∞

### 6.2 Teams

**GET /api/v1/teams**
Source: DB | Cache: 24h

**GET /api/v1/teams/{id}**
Source: DB + Jolpica | Cache: 24h

### 6.3 Circuits

**GET /api/v1/circuits**
Source: DB | Cache: ∞

**GET /api/v1/circuits/{id}**
Source: DB + Jolpica | Cache: ∞

### 6.4 Schedule

**GET /api/v1/schedule**
Source: Jolpica + OpenF1 | Cache: 6h

**GET /api/v1/schedule/next**
Source: Jolpica | Cache: 1h

### 6.5 Results

**GET /api/v1/races/{season}/{round}/results**
Source: Jolpica | Cache: 5min / ∞

**GET /api/v1/races/{season}/{round}/qualifying**
Source: Jolpica | Cache: ∞

**GET /api/v1/races/{season}/{round}/sprint**
Source: Jolpica | Cache: ∞

**GET /api/v1/races/{season}/{round}/pitstops**
Source: Jolpica + OpenF1 | Cache: ∞

### 6.6 Championships

**GET /api/v1/standings/drivers**
Source: Jolpica | Cache: 1h

**GET /api/v1/standings/constructors**
Source: Jolpica | Cache: 1h

### 6.7 Live Data

| Endpoint | Source | Cache |
|---|---|---|
| GET /api/v1/live/telemetry/{driverNum} | OpenF1 /car_data | None |
| GET /api/v1/live/positions | OpenF1 /location | None |
| GET /api/v1/live/intervals | OpenF1 /intervals | None |
| GET /api/v1/live/laps | OpenF1 /laps | None |
| GET /api/v1/live/race-control | OpenF1 /race_control | None |
| GET /api/v1/live/weather | OpenF1 /weather | 30s |
| GET /api/v1/live/stints | OpenF1 /stints | 10s |
| GET /api/v1/live/team-radio/{num} | OpenF1 /team_radio | None |

### 6.8 AI Content

**GET /api/v1/news**
Source: DB (AI) | Cache: 5min

**GET /api/v1/news/{id}**
Source: DB

**GET /api/v1/race-preview/{season}/{round}**
Source: DB (AI) | Cache: ∞

**GET /api/v1/race-review/{season}/{round}**
Source: DB (AI) | Cache: ∞

**GET /api/v1/search**
Source: DB (FTS) | Cache: 1min

---

## 7. Summary Endpoint Table

| # | Endpoint | Auth | Source |
|---|---|---|---|
| 1 | POST /auth/register | None | DB |
| 2 | POST /auth/login | None | DB |
| 3 | POST /auth/refresh | None (refresh token) | DB |
| 4 | POST /auth/logout | JWT | DB |
| 5 | GET /auth/me | JWT | DB |
| 6 | PUT /auth/me | JWT | DB |
| 7 | PUT /auth/password | JWT | DB |
| 8 | GET /drivers | JWT | DB + Jolpica + OpenF1 |
| 9 | GET /drivers/{id} | JWT | DB + Jolpica |
| 10 | GET /teams | JWT | DB + Jolpica |
| 11 | GET /circuits | JWT | DB + Jolpica |
| 12 | GET /schedule | JWT | Jolpica + OpenF1 |
| 13 | GET /races/{s}/{r}/results | JWT | Jolpica |
| 14 | GET /races/{s}/{r}/qualifying | JWT | Jolpica |
| 15 | GET /standings/drivers | JWT | Jolpica |
| 16 | GET /standings/constructors | JWT | Jolpica |
| 17 | GET /live/telemetry/{num} | JWT | OpenF1 proxy |
| 18 | GET /live/positions | JWT | OpenF1 proxy |
| 19 | GET /live/intervals | JWT | OpenF1 proxy |
| 20 | GET /live/race-control | JWT | OpenF1 proxy |
| 21 | GET /live/weather | JWT | OpenF1 proxy |
| 22 | GET /live/stints | JWT | OpenF1 proxy |
| 23 | GET /live/team-radio/{num} | JWT | OpenF1 proxy |
| 24 | GET /news | JWT | DB (AI) |
| 25 | GET /race-preview/{s}/{r} | JWT | DB (AI) |
| 26 | GET /race-review/{s}/{r} | JWT | DB (AI) |
| 27 | GET /search | JWT | DB (FTS) |
| 28 | GET /favorites/drivers | JWT | DB |
| 29 | POST /favorites/drivers/{id} | JWT | DB |
| 30 | DELETE /favorites/drivers/{id} | JWT | DB |
| 31 | GET /favorites/drivers/check/{id} | JWT | DB |
| 32 | GET /favorites/teams | JWT | DB |
| 33 | POST /favorites/teams/{id} | JWT | DB |
| 34 | DELETE /favorites/teams/{id} | JWT | DB |
| 35 | GET /favorites/teams/check/{id} | JWT | DB |
| 36 | GET /favorites/feed | JWT | DB + Jolpica |

---

## 8. Next Steps

1. Initialize Ktor project with Auth + JWT + Content Negotiation plugins
2. Create PostgreSQL schema (users, refresh_tokens, favorite_drivers, favorite_teams + data tables)
3. Implement JwtService + AuthService with BCrypt + token rotation
4. Set up auth routes (register, login, refresh, logout, me)
5. Implement FavoritesService + FavoritesRoutes with toggle mechanic
6. Implement `/favorites/feed` with standings + last results aggregation
7. Wrap all data routes in `authenticate("auth-jwt")`
8. Implement HTTP clients for OpenF1 and Jolpica
9. Implement seed script for initial data loading
10. Android: Ktor Client + Bearer plugin + EncryptedSharedPreferences
11. Android: Favorites UI with optimistic toggle + Favorites tab in bottom nav
12. Android: Home screen with personalized feed from `/favorites/feed`
13. Write integration tests for auth + favorites (Ktor testApplication)
14. Docker Compose: app + PostgreSQL + Redis
15. Document API via OpenAPI/Swagger
