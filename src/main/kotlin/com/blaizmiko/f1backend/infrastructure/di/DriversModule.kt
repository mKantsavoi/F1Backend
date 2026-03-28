package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.domain.repository.DriverRepository
import com.blaizmiko.f1backend.infrastructure.persistence.repository.ExposedDriverRepository
import com.blaizmiko.f1backend.infrastructure.seed.DriverSeedService
import com.blaizmiko.f1backend.usecase.GetDriverDetail
import com.blaizmiko.f1backend.usecase.GetDrivers
import org.koin.dsl.bind
import org.koin.dsl.module

val driversModule =
    module {
        single { ExposedDriverRepository() } bind DriverRepository::class
        single { DriverSeedService(get(), get(), get(), get(), get()) }
        single { GetDrivers(get()) }
        single { GetDriverDetail(get()) }
    }
