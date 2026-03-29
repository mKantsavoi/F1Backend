# Research: Caffeine Cache Registry

**Feature**: 010-caffeine-cache-registry
**Date**: 2026-03-28

## R1: Caffeine Library Selection

**Decision**: Use `com.github.ben-manes.caffeine:caffeine:3.2.3` as the core caching library.

**Rationale**: Caffeine is the de facto standard JVM caching library with near-optimal hit rates (Window TinyLfu eviction policy). It supports time-based expiration (`expireAfterWrite`), size-based eviction (`maximumSize`), built-in statistics (`recordStats()`), and async loading via `AsyncLoadingCache`. Version 3.x requires Java 11+ (project uses JVM 21). Context7-verified: high reputation, benchmark score 83.3.

**Alternatives considered**:
- Guava Cache: predecessor to Caffeine, lower performance, deprecated in favor of Caffeine
- Ehcache: heavier, XML config, overkill for in-memory-only use case
- Custom (current): lacks auto-eviction, unbounded memory, duplicated TTL logic

## R2: Kotlin Coroutines Integration

**Decision**: Use `com.sksamuel.aedile:aedile-core:3.0.2` as the Kotlin coroutines wrapper for Caffeine.

**Rationale**: Aedile wraps Caffeine's `AsyncCache`/`AsyncLoadingCache` with Kotlin-native suspend functions, eliminating the need for `CompletableFuture` interop or `runBlocking`. API usage:
```kotlin
val cache = Caffeine.newBuilder()
    .expireAfterWrite(1.hours)
    .maximumSize(100)
    .asCache<String, MyData>()

// Suspending get with loader:
val value = cache.get("key") { fetchFromUpstream() }  // suspend function
```
Aedile 3.0.2 is built against Caffeine 3.1+ and works with Kotlin coroutines. No Context7 entry exists, but GitHub shows active maintenance (latest release Dec 2025).

**Alternatives considered**:
- Raw Caffeine `AsyncLoadingCache` with `CompletableFuture.asDeferred()`: more boilerplate, manual coroutine bridging
- `kotlinx-coroutines-jdk8` future interop: works but less ergonomic than Aedile's native API
- Writing a custom coroutine wrapper: violates Constitution Principle V (prefer established libraries)

## R3: Statistics Exposure Pattern

**Decision**: Use Caffeine's built-in `recordStats()` + `Cache.stats()` API exposed via a simple GET endpoint.

**Rationale**: Caffeine provides `CacheStats` with `hitRate()`, `missRate()`, `hitCount()`, `missCount()`, `evictionCount()` out of the box. Aedile exposes the underlying Caffeine cache via `.underlying` for stats access. No external metrics library needed for this phase.

**Alternatives considered**:
- Micrometer integration: overkill for a single admin endpoint; can be added later
- Custom stats tracking: unnecessary when Caffeine provides this natively

## R4: Stale-on-Error Fallback Strategy

**Decision**: Maintain a `ConcurrentHashMap<String, Any>` backup map per CacheSpec inside CacheRegistry. On successful fetch, store in both Caffeine cache and backup map. On fetch failure when Caffeine cache is expired/empty, return from backup map.

**Rationale**: Caffeine does not support stale-while-error natively. The backup map pattern is simple, bounded by the same key space as the Caffeine cache, and preserves the existing graceful degradation behavior. The backup map does not need TTL — it only stores the last successful value and is bounded by the cache's key cardinality (which is small for this project: max ~500 keys per cache).

**Alternatives considered**:
- Caffeine `refreshAfterWrite` with `expireAfterWrite` set higher: doesn't handle errors gracefully (expired entries are removed)
- External resilience library (Resilience4j): adds complexity for a simple fallback pattern
- Keep stale-on-error logic in use cases: defeats the purpose of centralizing cache management

## R5: Thundering Herd Prevention

**Decision**: Retain existing per-key `Mutex` + `withLock` pattern in use cases. Caffeine's `AsyncLoadingCache.get()` already prevents duplicate loads for the same key, but the retry throttle logic (60-second backoff after failures) must remain in use cases.

**Rationale**: Caffeine's loading cache naturally coalesces concurrent requests for the same key. However, the project's retry throttle (skip upstream if last failure was <60s ago) is application-specific logic that belongs in use cases. The Mutex pattern is preserved for this throttle check, but the cache-miss-triggers-load deduplication is handled by Caffeine/Aedile natively.

**Alternatives considered**:
- Move all retry logic into CacheRegistry: violates Clean Architecture (CacheRegistry is infrastructure, retry policy is application logic)
- Remove retry throttle entirely: would cause excessive upstream calls during outages

## R6: Dependency Versions (Context7 + Web Verified)

| Dependency | Group ID | Artifact | Version | Source |
|------------|----------|----------|---------|--------|
| Caffeine | com.github.ben-manes.caffeine | caffeine | 3.2.3 | Context7 + Maven Central |
| Aedile | com.sksamuel.aedile | aedile-core | 3.0.2 | GitHub releases (Dec 2025) |

Both are compatible with JVM 21 and Kotlin 2.3.0.
