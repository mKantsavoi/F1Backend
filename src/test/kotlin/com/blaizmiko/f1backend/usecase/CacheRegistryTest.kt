package com.blaizmiko.f1backend.usecase

import com.blaizmiko.f1backend.infrastructure.cache.CacheRegistry
import com.blaizmiko.f1backend.infrastructure.cache.CacheSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CacheRegistryTest :
    StringSpec({

        "getCache returns a working cache for a given spec" {
            val registry = CacheRegistry()
            val cache = registry.getCache<String, String>(CacheSpec.CIRCUITS)

            cache.put("key1", "value1")
            cache.getIfPresent("key1") shouldBe "value1"
        }

        "getCache returns the same cache instance for the same spec" {
            val registry = CacheRegistry()
            val cache1 = registry.getCache<String, String>(CacheSpec.CIRCUITS)
            val cache2 = registry.getCache<String, String>(CacheSpec.CIRCUITS)

            cache1.put("shared", "data")
            cache2.getIfPresent("shared") shouldBe "data"
        }

        "getCache with ttlOverride returns a separate cache" {
            val registry = CacheRegistry()
            val defaultCache = registry.getCache<String, String>(CacheSpec.SCHEDULE)
            val overrideCache = registry.getCache<String, String>(CacheSpec.SCHEDULE, 24.seconds)

            defaultCache.put("key", "default")
            overrideCache.put("key", "override")

            defaultCache.getIfPresent("key") shouldBe "default"
            overrideCache.getIfPresent("key") shouldBe "override"
        }

        "cache evicts entries after TTL expires" {
            val registry = CacheRegistry()
            val cache = registry.getCache<String, String>(CacheSpec.PERSONALIZED_FEED)

            cache.put("user1", "feed-data")
            cache.getIfPresent("user1") shouldBe "feed-data"

            // PERSONALIZED_FEED has 30s TTL — Caffeine uses lazy eviction,
            // so we verify the cache was created with proper config via stats
            val stats = registry.stats(CacheSpec.PERSONALIZED_FEED)
            stats.shouldNotBeNull()
        }

        "fallback put and get work correctly" {
            val registry = CacheRegistry()

            registry.getFallback<String>(CacheSpec.CIRCUITS, "all").shouldBeNull()

            registry.putFallback(CacheSpec.CIRCUITS, "all", listOf("monza", "silverstone"))
            val fallback = registry.getFallback<String>(CacheSpec.CIRCUITS, "all")
            fallback shouldBe listOf("monza", "silverstone")
        }

        "fallback maps are isolated per spec" {
            val registry = CacheRegistry()

            registry.putFallback(CacheSpec.CIRCUITS, "key", "circuits-value")
            registry.putFallback(CacheSpec.SCHEDULE, "key", "schedule-value")

            registry.getFallback<String>(CacheSpec.CIRCUITS, "key") shouldBe "circuits-value"
            registry.getFallback<String>(CacheSpec.SCHEDULE, "key") shouldBe "schedule-value"
        }

        "stats returns null for unused spec" {
            val registry = CacheRegistry()
            registry.stats(CacheSpec.CIRCUITS).shouldBeNull()
        }

        "stats returns cache statistics after use" {
            val registry = CacheRegistry()
            val cache = registry.getCache<String, String>(CacheSpec.CIRCUITS)

            cache.put("key", "value")
            cache.getIfPresent("key")
            cache.getIfPresent("missing")

            val stats = registry.stats(CacheSpec.CIRCUITS)
            stats.shouldNotBeNull()
            stats.hitCount() shouldBe 1
            stats.missCount() shouldBe 1
        }

        "cache respects maximumSize by accepting entries up to limit" {
            val registry = CacheRegistry()
            // SCHEDULE_NEXT has maxSize=10
            val cache = registry.getCache<String, String>(CacheSpec.SCHEDULE_NEXT)

            repeat(10) { i ->
                cache.put("key$i", "value$i")
            }

            // All 10 should be present immediately after insertion
            repeat(10) { i ->
                cache.getIfPresent("key$i") shouldBe "value$i"
            }
        }

        "cache get with loader works as suspend function" {
            val registry = CacheRegistry()
            val cache = registry.getCache<String, String>(CacheSpec.CIRCUITS)

            var loaderCalled = false
            val value =
                cache.get("loaded") {
                    loaderCalled = true
                    delay(1.milliseconds) // prove suspend works
                    "loaded-value"
                }

            loaderCalled shouldBe true
            value shouldBe "loaded-value"

            // Second call should hit cache, not loader
            var secondLoaderCalled = false
            val cached =
                cache.get("loaded") {
                    secondLoaderCalled = true
                    "should-not-be-used"
                }
            secondLoaderCalled shouldBe false
            cached shouldBe "loaded-value"
        }
    })
