package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.usecase.GetCircuits
import org.koin.dsl.module

val circuitsModule =
    module {
        single { GetCircuits(get(), get()) }
    }
