package com.magicvector.repository.api.config

import com.magicvector.MainApplication
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        val excludePaths = listOf(
            "/user/login",
            "/user/register",
            "/user/token/verify",
            "/test/",
            "/error"
        )

        val needAuth = !excludePaths.any { path.startsWith(it) }

        if (!needAuth) {
            return chain.proceed(originalRequest)
        }

        val userManager = MainApplication.getUserManager()
        val currentUser = runBlocking { userManager.getCurrentUser() }

        val requestBuilder = originalRequest.newBuilder()

        if (currentUser != null && currentUser.userId > 0 && currentUser.accessToken.isNotBlank()) {
            requestBuilder
                .header("user_id", currentUser.userId.toString())
                .header("access_token", currentUser.accessToken)
        }

        return chain.proceed(requestBuilder.build())
    }
}
