package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.domain.port.CircuitCache
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryCircuitCache
import com.blaizmiko.f1backend.usecase.GetCircuits
import org.koin.dsl.bind
import org.koin.dsl.module

val circuitsModule =
    module {
        single { InMemoryCircuitCache() } bind CircuitCache::class
        single { GetCircuits(get(), get()) }
    }
