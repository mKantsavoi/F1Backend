package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.adapter.port.RaceResultCache
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryRaceResultCache
import com.blaizmiko.f1backend.usecase.GetQualifyingResults
import com.blaizmiko.f1backend.usecase.GetRaceResults
import com.blaizmiko.f1backend.usecase.GetSprintResults
import org.koin.dsl.bind
import org.koin.dsl.module

// Races feature: race results, qualifying, sprint.
val racesModule =
    module {
        single { InMemoryRaceResultCache() } bind RaceResultCache::class
        single { GetRaceResults(get(), get()) }
        single { GetQualifyingResults(get(), get()) }
        single { GetSprintResults(get(), get()) }
    }
