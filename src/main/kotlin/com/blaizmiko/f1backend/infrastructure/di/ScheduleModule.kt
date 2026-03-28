package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.adapter.port.NextRaceCache
import com.blaizmiko.f1backend.adapter.port.ScheduleCache
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryNextRaceCache
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryScheduleCache
import com.blaizmiko.f1backend.infrastructure.config.JolpicaConfig
import com.blaizmiko.f1backend.usecase.GetNextRace
import com.blaizmiko.f1backend.usecase.GetSchedule
import org.koin.dsl.bind
import org.koin.dsl.module

// Schedule feature: season schedule + next race.
val scheduleModule =
    module {
        single { InMemoryScheduleCache() } bind ScheduleCache::class
        single { InMemoryNextRaceCache() } bind NextRaceCache::class
        single { GetSchedule(get(), get(), get<JolpicaConfig>().cacheTtlHours) }
        single { GetNextRace(get(), get()) }
    }
