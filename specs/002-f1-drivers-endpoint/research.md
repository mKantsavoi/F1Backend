# Research: F1 Drivers Endpoint

**Feature**: 002-f1-drivers-endpoint
**Date**: 2026-03-27

## R1: Ktor HTTP Client for External API Calls

**Decision**: Use Ktor Client with CIO engine, HttpRequestRetry, and HttpTimeout plugins.

**Rationale**: Ktor Client is the natural companion to Ktor Server (already used). CIO is a lightweight coroutine-based engine with no additional native dependencies. The project already includes `ktor-client-content-negotiation` in test dependencies, confirming the ecosystem is in use. Context7 verified that Ktor 3.4.1 supports all required plugins.

**Alternatives considered**:
- **OkHttp**: Well-established but adds a non-Ktor dependency; not coroutine-native.
- **Fuel**: Kotlin-native HTTP client but less maintained and less integrated with Ktor.
- **Java HttpClient (JDK 21)**: Zero dependencies but lacks retry/serialization plugins; more boilerplate.

**New dependencies required** (all at Ktor 3.4.1, already in version catalog):
- `io.ktor:ktor-client-core` (implementation)
- `io.ktor:ktor-client-cio` (implementation)
- `io.ktor:ktor-client-content-negotiation` (implementation — currently test-only, promote)
- `io.ktor:ktor-client-mock` (testImplementation — for integration tests)

## R2: Jolpica API Response Format

**Decision**: Map from Jolpica's nested `MRData.DriverTable.Drivers[]` format to our flat `Driver` domain model.

**Rationale**: The Jolpica API returns data in the Ergast-compatible format with deep nesting (`MRData` → `DriverTable` → `Drivers`). Our domain model should be flat and only include fields the mobile client needs.

**Jolpica response structure** (verified via live API call):
```json
{
  "MRData": {
    "DriverTable": {
      "season": "2026",
      "Drivers": [
        {
          "driverId": "max_verstappen",
          "permanentNumber": "1",
          "code": "VER",
          "givenName": "Max",
          "familyName": "Verstappen",
          "dateOfBirth": "1997-09-30",
          "nationality": "Dutch",
          "url": "http://en.wikipedia.org/wiki/Max_Verstappen"
        }
      ]
    }
  }
}
```

**Mapping**: `driverId` → `id`, `permanentNumber` → `number`, `code` → `code`, `givenName` → `firstName`, `familyName` → `lastName`, `nationality` → `nationality`, `dateOfBirth` → `dateOfBirth`. The `url` field is dropped (not needed by mobile client).

## R3: In-Memory Caching Strategy

**Decision**: Use `ConcurrentHashMap<String, CacheEntry<List<Driver>>>` keyed by season year string.

**Rationale**: ConcurrentHashMap is thread-safe, lock-free for reads, and part of the JDK stdlib — no new dependency needed. Each season gets its own cache entry with an independent 24h TTL. The `CacheEntry` wrapper holds the data, timestamp, and expiry information.

**Alternatives considered**:
- **Caffeine cache**: Feature-rich (auto-eviction, stats) but adds a dependency for a simple use case. Can be adopted later if cache complexity grows.
- **Guava cache**: Heavy dependency just for caching.
- **Custom LRU**: Over-engineered for ~5-10 season entries.

**Cache entry structure**:
- `data: List<Driver>` — cached driver list
- `fetchedAt: Instant` — when data was fetched
- `expiresAt: Instant` — fetchedAt + 24 hours

**Eviction**: Entries expire passively (checked on read). No background cleanup needed at current scale.

## R4: Rate Limiting Approach

**Decision**: Rely on cache TTL (24h) as the primary rate-limiting mechanism. No explicit rate limiter implementation for v1.

**Rationale**: With a 24h cache TTL per season, the backend will make at most 1 request per season per day. Even with 50 different seasons requested daily, that's 50 requests/day — well within Jolpica's 500/hour limit. The 4 req/s burst limit is also safe since concurrent requests for the same season will be coalesced (only one fetch triggered while others wait).

**Alternatives considered**:
- **Token bucket / leaky bucket**: Unnecessary complexity given the cache provides natural rate limiting.
- **Guava RateLimiter**: Adds dependency for a problem that doesn't exist at current scale.

**Future**: If additional Jolpica endpoints are added (standings, races, results), reassess whether a shared rate limiter across all endpoints is needed.

## R5: Retry and Timeout Configuration

**Decision**: 2 retries with exponential backoff (1s, 2s), 10s request timeout, 5s connect timeout.

**Rationale**: Jolpica is a free community API; transient failures are expected. Two retries with exponential backoff give reasonable recovery without excessive waiting. Total worst-case time: ~13s (attempt 1 timeout + 1s delay + attempt 2 timeout... capped by the retry config). The `retryOnServerErrors` function handles 5xx responses automatically.

**Configuration**:
```kotlin
install(HttpRequestRetry) {
    maxRetries = 2
    retryOnServerErrors(maxRetries = 2)
    exponentialDelay()
}
install(HttpTimeout) {
    requestTimeoutMillis = 10_000
    connectTimeoutMillis = 5_000
}
```

## R6: Stale Cache Delivery Mechanism

**Decision**: Return stale cached data with HTTP `Warning: 110 - "Response is stale"` header when external API is unavailable.

**Rationale**: Per clarification session, the team chose HTTP warning headers as the stale-data indicator. The `Warning` header with code 110 is the standard HTTP mechanism for this purpose (RFC 7234). The response body remains identical to a fresh response — no additional fields needed.

## R7: Concurrent Request Coalescing

**Decision**: Use Kotlin `Mutex` per cache key to ensure only one external fetch per season at a time.

**Rationale**: When the cache is cold and multiple requests arrive simultaneously for the same season, without coalescing all of them would hit the external API. A per-key mutex ensures only the first request fetches; others suspend and receive the cached result. This naturally respects rate limits.

**Implementation**: `ConcurrentHashMap<String, Mutex>` for per-season locks. Lock is acquired before checking cache staleness; if another coroutine already refreshed, the fresh cache is returned.
