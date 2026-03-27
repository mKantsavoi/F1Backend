package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.domain.port.TeamCache
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryTeamCache
import com.blaizmiko.f1backend.infrastructure.config.JolpicaConfig
import com.blaizmiko.f1backend.usecase.GetTeams
import org.koin.dsl.bind
import org.koin.dsl.module

val teamsModule =
    module {
        single { InMemoryTeamCache() } bind TeamCache::class
        single { GetTeams(get(), get(), get<JolpicaConfig>().cacheTtlHours) }
    }
