package com.blaizmiko.f1backend

import com.blaizmiko.f1backend.infrastructure.config.loadAppConfig
import com.blaizmiko.f1backend.infrastructure.di.authModule
import com.blaizmiko.f1backend.infrastructure.di.circuitsModule
import com.blaizmiko.f1backend.infrastructure.di.clientModule
import com.blaizmiko.f1backend.infrastructure.di.coreModule
import com.blaizmiko.f1backend.infrastructure.di.driversModule
import com.blaizmiko.f1backend.infrastructure.di.racesModule
import com.blaizmiko.f1backend.infrastructure.di.scheduleModule
import com.blaizmiko.f1backend.infrastructure.di.teamsModule
import com.blaizmiko.f1backend.infrastructure.persistence.DatabaseFactory
import com.blaizmiko.f1backend.infrastructure.security.JwtProvider
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain
        .main(args)
}

fun Application.module() {
    val appConfig = loadAppConfig()

    install(Koin) {
        slf4jLogger()
        modules(
            coreModule(appConfig),
            clientModule,
            authModule,
            driversModule,
            teamsModule,
            circuitsModule,
            scheduleModule,
            racesModule,
        )
    }

    DatabaseFactory.init(appConfig.database)

    val jwtProvider by inject<JwtProvider>()
    install(Authentication) {
        jwtProvider.configureAuth(this)
    }

    configureSerialization()
    configureStatusPages()
    configureRouting()
}
