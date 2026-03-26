package com.blaizmiko

import com.blaizmiko.infrastructure.config.loadAppConfig
import com.blaizmiko.infrastructure.persistence.DatabaseFactory
import com.blaizmiko.infrastructure.security.JwtProvider
import io.ktor.server.application.*
import io.ktor.server.auth.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    val appConfig = loadAppConfig()
    val jwtProvider = JwtProvider(appConfig.jwt)

    DatabaseFactory.init(appConfig.database)

    install(Authentication) {
        jwtProvider.configureAuth(this)
    }

    configureSerialization()
    configureStatusPages()
    configureRouting(appConfig, jwtProvider)
}
