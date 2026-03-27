# Quickstart: Koin Dependency Injection

**Feature**: 003-koin-dependency-injection
**Date**: 2026-03-27

## Prerequisites

- Existing project builds and all tests pass on `master`
- Docker Compose running (PostgreSQL)
- JVM 21 installed

## What Changes

### New Dependencies (gradle/libs.versions.toml)

```toml
[versions]
koin-bom = "4.2.0"

[libraries]
koin-bom = { module = "io.insert-koin:koin-bom", version.ref = "koin-bom" }
koin-ktor = { module = "io.insert-koin:koin-ktor" }
koin-logger-slf4j = { module = "io.insert-koin:koin-logger-slf4j" }
koin-test = { module = "io.insert-koin:koin-test" }
koin-test-junit5 = { module = "io.insert-koin:koin-test-junit5" }
```

```kotlin
// build.gradle.kts
implementation(platform(libs.koin.bom))
implementation(libs.koin.ktor)
implementation(libs.koin.logger.slf4j)
testImplementation(libs.koin.test)
testImplementation(libs.koin.test.junit5)
```

### New Files

```
src/main/kotlin/com/blaizmiko/infrastructure/di/
├── CoreModule.kt      // AppConfig, JwtProvider, database config
├── ClientModule.kt    // JolpicaClient → DriverDataSource
├── AuthModule.kt      // Repositories + auth use cases
└── DriversModule.kt   // DriverCache + GetDrivers
```

### Modified Files

- `Application.kt` — Install Koin plugin, remove manual wiring
- `Routing.kt` — Remove dependency parameters from `configureRouting()`, inject via Koin
- `AuthRoutes.kt` — Remove function parameters, use `inject()` from Koin
- `DriverRoutes.kt` — Remove function parameter, use `inject()` from Koin
- `AuthLifecycleTest.kt` — Update test module to use Koin
- `DriversEndpointTest.kt` — Update test module to use Koin with fake overrides

### Unchanged

- All domain models, DTOs, repositories, use cases, ports
- Docker Compose configuration
- API endpoints and behavior
- Application YAML configuration

## Verification

```bash
# Run all tests
./gradlew test

# Start application
docker-compose up -d db
./gradlew run

# Verify endpoints
curl http://localhost:8080/api/v1/auth/register -X POST -H "Content-Type: application/json" -d '{"email":"test@test.com","username":"test","password":"Test1234!"}'
```
