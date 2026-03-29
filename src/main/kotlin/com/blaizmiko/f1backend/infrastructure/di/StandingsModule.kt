package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.usecase.GetConstructorStandings
import com.blaizmiko.f1backend.usecase.GetDriverStandings
import org.koin.dsl.module

// Championship standings feature: use cases for driver & constructor standings.
val standingsModule =
    module {
        single { GetDriverStandings(get(), get()) }
        single { GetConstructorStandings(get(), get()) }
    }
