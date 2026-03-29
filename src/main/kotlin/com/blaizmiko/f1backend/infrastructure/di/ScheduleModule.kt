package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.infrastructure.config.JolpicaConfig
import com.blaizmiko.f1backend.usecase.GetNextRace
import com.blaizmiko.f1backend.usecase.GetSchedule
import org.koin.dsl.module

// Schedule feature: season schedule + next race.
val scheduleModule =
    module {
        single { GetSchedule(get(), get(), get<JolpicaConfig>().cacheTtlHours) }
        single { GetNextRace(get(), get()) }
    }
