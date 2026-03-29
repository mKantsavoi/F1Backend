# Quickstart: Caffeine Cache Registry

**Feature**: 010-caffeine-cache-registry
**Date**: 2026-03-28

## Prerequisites

- JDK 21
- Existing project builds and tests pass (`./gradlew ktlintCheck detekt test`)

## New Dependencies

Add to `gradle/libs.versions.toml`:
```toml
[versions]
caffeine = "3.2.3"
aedile = "3.0.2"

[libraries]
caffeine = { module = "com.github.ben-manes.caffeine:caffeine", version.ref = "caffeine" }
aedile-core = { module = "com.sksamuel.aedile:aedile-core", version.ref = "aedile" }
```

Add to `build.gradle.kts`:
```kotlin
implementation(libs.caffeine)
implementation(libs.aedile.core)
```

## Key Files to Create

| File | Purpose |
|------|---------|
| `infrastructure/cache/CacheSpec.kt` | Enum defining all cache configurations |
| `infrastructure/cache/CacheRegistry.kt` | Singleton managing all Caffeine cache instances + fallback maps |
| `infrastructure/di/CacheModule.kt` | Koin module registering CacheRegistry as singleton |

## Key Files to Modify

| File | Change |
|------|--------|
| `Application.kt` | Add `cacheModule` to Koin modules list |
| All use cases (8 files) | Replace cache port injection with CacheRegistry, simplify caching logic |
| All DI modules (4 files) | Remove InMemory*Cache bindings |

## Key Files to Delete

| File | Reason |
|------|--------|
| 6 cache interfaces in `adapter/port/` | Replaced by CacheRegistry |
| 6 InMemory*Cache in `infrastructure/cache/` | Replaced by Caffeine |
| `domain/model/CacheEntry.kt` | Caffeine manages TTL |

## Verification

```bash
./gradlew ktlintCheck detekt test
```

All existing tests must pass, plus the new CacheRegistry unit test.
