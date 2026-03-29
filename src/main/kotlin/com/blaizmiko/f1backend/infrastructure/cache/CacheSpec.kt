package com.blaizmiko.f1backend.infrastructure.cache

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("MagicNumber")
enum class CacheSpec(
    val ttl: Duration,
    val maxSize: Long,
) {
    SCHEDULE(6.hours, 50),
    SCHEDULE_NEXT(1.hours, 10),
    RACE_RESULTS(5.minutes, 200),
    RACE_RESULTS_HISTORICAL(365.days, 500),
    QUALIFYING(5.minutes, 200),
    QUALIFYING_HISTORICAL(365.days, 500),
    SPRINT(5.minutes, 200),
    SPRINT_HISTORICAL(365.days, 500),
    DRIVER_STANDINGS(1.hours, 50),
    DRIVER_STANDINGS_HISTORICAL(365.days, 500),
    CONSTRUCTOR_STANDINGS(1.hours, 50),
    CONSTRUCTOR_STANDINGS_HISTORICAL(365.days, 500),
    CIRCUITS(365.days, 100),
    PERSONALIZED_FEED(30.seconds, 1000),
}
