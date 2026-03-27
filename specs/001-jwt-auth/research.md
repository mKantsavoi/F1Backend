# Research: JWT Authentication & Authorization

**Feature**: 001-jwt-auth | **Date**: 2026-03-26

## Decision Log

### 1. JWT Library

**Decision**: Use Ktor's built-in `ktor-server-auth-jwt` plugin (v3.4.1)
**Rationale**: First-party Ktor integration. Bundles `com.auth0:java-jwt` transitively — no need to declare it separately. Provides `authenticate {}` DSL blocks and `JWTPrincipal` extraction out of the box.
**Alternatives considered**: Direct use of `io.jsonwebtoken:jjwt` — rejected because Ktor's plugin already wraps a JWT library and provides middleware integration.

### 2. Password Hashing

**Decision**: `at.favre.lib:bcrypt:0.10.2`
**Rationale**: Actively maintained, modern API, supports `$2a$/$2b$` variants. The alternative `org.mindrot:jbcrypt:0.4` has not been updated since 2015.
**Alternatives considered**: `org.mindrot:jbcrypt` — abandoned, no security patches in 10+ years.

### 3. Database ORM

**Decision**: JetBrains Exposed 1.0.0 (DSL style, not DAO)
**Rationale**: Constitution mandates Exposed. DSL style is simpler for this feature — no need for entity-level caching or lazy loading. Modules: `exposed-core`, `exposed-jdbc`, `exposed-java-time`.
**Alternatives considered**: Exposed DAO style — adds unnecessary abstraction for straightforward CRUD + query operations.

### 4. Connection Pooling

**Decision**: `com.zaxxer:HikariCP:6.2.1`
**Rationale**: Industry standard for JVM connection pooling. Not bundled with Exposed — must be declared explicitly. 6.x line is widely used in the Kotlin ecosystem.
**Alternatives considered**: HikariCP 7.x — newer but 6.x is better tested with Exposed.

### 5. PostgreSQL Driver

**Decision**: `org.postgresql:postgresql:42.7.10`
**Rationale**: Official JDBC driver, latest stable release.

### 6. Serialization

**Decision**: `ktor-server-content-negotiation` + `ktor-serialization-kotlinx-json` (Ktor 3.4.1)
**Rationale**: Constitution mandates kotlinx.serialization. Requires the `org.jetbrains.kotlin.plugin.serialization` Gradle plugin (v2.3.0).

### 7. Error Handling

**Decision**: `ktor-server-status-pages` (Ktor 3.4.1)
**Rationale**: First-party Ktor plugin for centralized exception-to-HTTP-response mapping. Maps domain exceptions to appropriate status codes (400, 401, 409, etc.).

### 8. Testing Stack

**Decision**: kotest 6.1.5 + testcontainers 1.21.4
**Rationale**: Constitution mandates kotest + testcontainers. Modules:
- `io.kotest:kotest-runner-junit5-jvm:6.1.5`
- `io.kotest:kotest-assertions-core-jvm:6.1.5`
- `org.testcontainers:testcontainers:1.21.4`
- `org.testcontainers:postgresql:1.21.4`
- `io.ktor:ktor-server-test-host:3.4.1` (already in project)

### 9. API Documentation

**Decision**: `ktor-server-openapi` + `ktor-server-swagger` (Ktor 3.4.1)
**Rationale**: Constitution mandates OpenAPI/Swagger. Ktor 3.4.1 plugin generates spec from route definitions. Swagger UI served for developer convenience.

### 10. Refresh Token Hashing

**Decision**: Java standard library `java.security.MessageDigest` with SHA-256
**Rationale**: Refresh tokens are random opaque strings (not passwords), so a fast cryptographic hash is appropriate. BCrypt would add unnecessary latency for non-password hashing. SHA-256 is sufficient for token fingerprinting.
**Alternatives considered**: BCrypt for tokens — rejected, unnecessarily slow for this use case. HMAC-SHA256 — adds key management complexity without benefit over plain SHA-256 for stored token comparison.

### 11. Token Generation

**Decision**: `java.security.SecureRandom` for refresh token generation (128-bit random, Base64-encoded)
**Rationale**: Cryptographically secure, no external dependency needed. 128 bits provides sufficient entropy against brute force.

### 12. Project Structure (Clean Architecture)

**Decision**: Four-layer package structure under `com.blaizmiko`
**Rationale**: Constitution mandates Clean Architecture (domain → use-cases → adapters → infrastructure). Each layer in its own package with strict dependency direction.

```
com.blaizmiko/
├── domain/          # Entities, repository interfaces, value objects
├── usecase/         # Application business rules
├── adapter/         # Ktor routes, DTOs, validation
└── infrastructure/  # Exposed repositories, JWT provider, config
```
