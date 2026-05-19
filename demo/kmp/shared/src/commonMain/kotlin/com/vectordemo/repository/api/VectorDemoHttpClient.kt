package com.vectordemo.repository.api

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** 业务 API 鉴权头（对齐 demo/app [AuthInterceptor] 与 Spring Boot AuthTokenInterceptor）。 */
data class ApiAuthHeaders(
    val userId: Long,
    val accessToken: String,
)

internal fun shouldSkipAuthHeaders(path: String): Boolean {
    return path.contains("/user/login") ||
        path.contains("/user/register") ||
        path.contains("/user/token/verify")
}

internal fun HttpClientConfig<*>.configureVectorDemoHttpClient(
    authProvider: () -> ApiAuthHeaders?,
) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            },
        )
    }
    install("VectorDemoAuthHeaders") {
        requestPipeline.intercept(HttpRequestPipeline.State) {
            val path = context.url.pathSegments.joinToString(prefix = "/", separator = "/")
            if (shouldSkipAuthHeaders(path)) return@intercept
            authProvider()?.let { auth ->
                context.headers.append("user_id", auth.userId.toString())
                context.headers.append("access_token", auth.accessToken)
            }
        }
    }
    defaultRequest {
        contentType(ContentType.Application.Json)
    }
}
