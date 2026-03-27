package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.domain.port.DriverCache
import com.blaizmiko.f1backend.infrastructure.cache.InMemoryDriverCache
import com.blaizmiko.f1backend.infrastructure.config.JolpicaConfig
import com.blaizmiko.f1backend.usecase.GetDrivers
import org.koin.dsl.bind
import org.koin.dsl.module

// Drivers feature: cache and use cases.
// Add new driver-related components here (e.g., driver details, standings).
val driversModule = module {
    single { InMemoryDriverCache() } bind DriverCache::class
    single { GetDrivers(get(), get(), get<JolpicaConfig>().cacheTtlHours) }
}
