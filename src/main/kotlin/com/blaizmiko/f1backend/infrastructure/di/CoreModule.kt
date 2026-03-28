package com.blaizmiko.f1backend.infrastructure.di

import com.blaizmiko.f1backend.infrastructure.config.AppConfig
import com.blaizmiko.f1backend.infrastructure.security.JwtProvider
import org.koin.dsl.module

// Core infrastructure: application configuration and security.
// Add shared cross-cutting concerns here (e.g., logging, metrics).
fun coreModule(appConfig: AppConfig) =
    module {
        single { appConfig }
        single { appConfig.jwt }
        single { appConfig.jolpica }
        single { appConfig.database }
        single { JwtProvider(get()) }
    }
