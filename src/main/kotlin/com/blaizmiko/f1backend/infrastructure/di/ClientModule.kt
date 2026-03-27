package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.domain.port.CircuitDataSource
import com.blaizmiko.f1backend.domain.port.DriverDataSource
import com.blaizmiko.f1backend.domain.port.TeamDataSource
import com.blaizmiko.f1backend.infrastructure.external.JolpicaClient
import org.koin.core.module.dsl.onClose
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.bind
import org.koin.dsl.module

// External HTTP clients. Add new API client bindings here (e.g., telemetry API).
val clientModule =
    module {
        single { JolpicaClient(get()) } bind DriverDataSource::class withOptions {
            onClose { (it as? JolpicaClient)?.close() }
        }
        single<TeamDataSource> { get<JolpicaClient>() }
        single<CircuitDataSource> { get<JolpicaClient>() }
    }
