package com.vectordemo.repository.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

actual fun createPlatformHttpClient(tokenProvider: () -> String?): HttpClient {
    return HttpClient(Darwin) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            val token = tokenProvider()
            if (!token.isNullOrBlank()) {
                headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
}
