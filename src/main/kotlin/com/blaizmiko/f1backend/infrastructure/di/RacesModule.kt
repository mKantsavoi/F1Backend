package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.usecase.GetQualifyingResults
import com.blaizmiko.f1backend.usecase.GetRaceResults
import com.blaizmiko.f1backend.usecase.GetSprintResults
import org.koin.dsl.module

// Races feature: race results, qualifying, sprint.
val racesModule =
    module {
        single { GetRaceResults(get(), get()) }
        single { GetQualifyingResults(get(), get()) }
        single { GetSprintResults(get(), get()) }
    }
