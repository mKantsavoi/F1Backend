package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.adapter.port.ConstructorStandingsCache
import com.blaizmiko.f1backend.adapter.port.DriverStandingsCache
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryConstructorStandingsCache
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryDriverStandingsCache
import com.blaizmiko.f1backend.usecase.GetConstructorStandings
import com.blaizmiko.f1backend.usecase.GetDriverStandings
import org.koin.dsl.bind
import org.koin.dsl.module

// Championship standings feature: caches and use cases for driver & constructor standings.
val standingsModule =
    module {
        single { InMemoryDriverStandingsCache() } bind DriverStandingsCache::class
        single { InMemoryConstructorStandingsCache() } bind ConstructorStandingsCache::class
        single { GetDriverStandings(get(), get()) }
        single { GetConstructorStandings(get(), get()) }
    }
