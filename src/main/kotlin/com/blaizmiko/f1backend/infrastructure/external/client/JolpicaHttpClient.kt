package com.blaizmiko.f1backend.infrastructure.external.client

import com.blaizmiko.f1backend.infrastructure.config.JolpicaConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class JolpicaHttpClient(
    val config: JolpicaConfig,
) : java.io.Closeable {
    val http: HttpClient =
        HttpClient(CIO) {
            install(HttpRequestRetry) {
                maxRetries = 2
                retryOnServerErrors(maxRetries = 2)
                exponentialDelay()
            }
            install(HttpTimeout) {
                requestTimeoutMillis = config.requestTimeoutMs
                connectTimeoutMillis = config.connectTimeoutMs
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        isLenient = false
                    },
                )
            }
        }

    override fun close() {
        http.close()
    }
}
