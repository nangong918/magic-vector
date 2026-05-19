package com.vectordemo.repository.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

actual fun createPlatformHttpClient(authProvider: () -> ApiAuthHeaders?): HttpClient {
    return HttpClient(Darwin) {
        configureVectorDemoHttpClient(authProvider)
    }
}
