package com.vectordemo.repository.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createPlatformHttpClient(authProvider: () -> ApiAuthHeaders?): HttpClient {
    return HttpClient(OkHttp) {
        configureVectorDemoHttpClient(authProvider)
    }
}
