# Quickstart: F1 Drivers Endpoint

**Feature**: 002-f1-drivers-endpoint
**Date**: 2026-03-27

## Prerequisites

- JDK 21
- Docker (for PostgreSQL via docker-compose — required by existing auth features)
- Valid JWT token (obtain via `POST /api/v1/auth/register` or `POST /api/v1/auth/login`)

## New Dependencies

Add to `gradle/libs.versions.toml` under `[libraries]`:

```toml
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
```

Add to `build.gradle.kts`:

```kotlin
// Implementation
implementation(libs.ktor.client.core)
implementation(libs.ktor.client.cio)
implementation(libs.ktor.client.content.negotiation) // promote from testImplementation

// Test
testImplementation(libs.ktor.client.mock)
```

## Configuration

Add to `src/main/resources/application.yaml`:

```yaml
jolpica:
  baseUrl: "https://api.jolpi.ca/ergast/f1"
  requestTimeoutMs: "10000"
  connectTimeoutMs: "5000"
  cacheTtlHours: "24"
```

## Run & Test

```bash
# Start dependencies
docker-compose up -d

# Run the server
./gradlew run

# Get a JWT token
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' | jq -r '.accessToken')

# Fetch current season drivers
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/drivers

# Fetch specific season
curl -H "Authorization: Bearer $TOKEN" "http://localhost:8080/api/v1/drivers?season=2024"

# Run tests
./gradlew test
```

## New Files

| File | Layer | Purpose |
|------|-------|---------|
| `adapter/dto/DriverResponses.kt` | Adapter | `DriversResponse` and `DriverDto` serializable DTOs |
| `adapter/route/DriverRoutes.kt` | Adapter | Route handler for `GET /api/v1/drivers` |
| `domain/model/Driver.kt` | Domain | `Driver` entity (pure Kotlin) |
| `domain/port/DriverDataSource.kt` | Domain | Interface for external driver data |
| `domain/port/DriverCache.kt` | Domain | Interface for cache operations |
| `infrastructure/external/JolpicaClient.kt` | Infrastructure | HTTP client for Jolpica API |
| `infrastructure/external/JolpicaModels.kt` | Infrastructure | Jolpica JSON response DTOs |
| `infrastructure/cache/InMemoryDriverCache.kt` | Infrastructure | ConcurrentHashMap-based cache |
| `usecase/GetDrivers.kt` | Use-case | Orchestration: cache → fetch → transform |
| `integration/DriversEndpointTest.kt` | Test | Integration test with mocked Jolpica |

## Architecture Flow

```
Client → [JWT Auth] → DriverRoutes → GetDrivers use-case
                                          ├── DriverCache.get(season)
                                          │   └── cache hit? → return cached
                                          └── DriverDataSource.fetchDrivers(season)
                                              └── JolpicaClient → Jolpica API
                                                  └── transform → cache → return
```
