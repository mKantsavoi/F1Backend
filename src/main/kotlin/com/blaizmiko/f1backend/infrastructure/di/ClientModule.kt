package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.domain.port.CircuitDataSource
import com.blaizmiko.f1backend.domain.port.DriverDataSource
import com.blaizmiko.f1backend.domain.port.RaceDataSource
import com.blaizmiko.f1backend.domain.port.ScheduleDataSource
import com.blaizmiko.f1backend.domain.port.TeamDataSource
import com.blaizmiko.f1backend.infrastructure.external.client.JolpicaDriverClient
import com.blaizmiko.f1backend.infrastructure.external.client.JolpicaHttpClient
import com.blaizmiko.f1backend.infrastructure.external.client.JolpicaRaceClient
import com.blaizmiko.f1backend.infrastructure.external.client.JolpicaScheduleClient
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.bind
import org.koin.dsl.module

// External HTTP clients. Add new API client bindings here (e.g., telemetry API).
val clientModule =
    module {
        single { JolpicaHttpClient(get()) } withOptions {
            onClose { it?.close() }
        }
        single { JolpicaDriverClient(get()) } bind DriverDataSource::class
        single<TeamDataSource> { get<JolpicaDriverClient>() }
        single<CircuitDataSource> { get<JolpicaDriverClient>() }
        single { JolpicaScheduleClient(get()) } bind ScheduleDataSource::class
        single { JolpicaRaceClient(get()) } bind RaceDataSource::class
    }
