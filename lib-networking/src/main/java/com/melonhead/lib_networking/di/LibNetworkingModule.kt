package com.melonhead.lib_networking.di

import com.melonhead.lib_logging.Clog
import com.melonhead.lib_networking.ratelimit.RateLimit
import com.melonhead.lib_networking.ratelimit.impl.default
import com.melonhead.lib_networking.ratelimit.impl.rate
import com.melonhead.lib_networking.ratelimit.impl.select
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import java.net.ConnectException
import kotlin.time.DurationUnit

val LibNetworkingModule = module {
    single {
        HttpClient(CIO) {
            install(RateLimit) {
                select { it.url.toString().contains("api.mangadex.org") }.rate(1, 3, DurationUnit.SECONDS)
                select { it.url.toString().contains("auth.mangadex.org") }.rate(1, 3, DurationUnit.SECONDS)
                // set default rate to 5 requests per 1 second
                default().rate(5, 1, DurationUnit.SECONDS)
            }
            install(Logging) {
                level = LogLevel.INFO
                logger = object: Logger {
                    override fun log(message: String) {
                        Clog.d(message)
                    }
                }
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
            install(HttpRequestRetry) {
                retryIf { _, response ->
                    !response.status.isSuccess() && response.status.value != 301 && response.status.value != 429 && response.status.value != 404
                }
                retryOnExceptionIf { _, cause ->
                    cause is ConnectTimeoutException || cause is JsonConvertException || cause is ConnectException
                }
                exponentialDelay()
            }
        }
    }
}
