package com.vectordemo.repository.api.config

import com.vectordemo.MainApplication
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val skip = path.startsWith("/user/login") || path.startsWith("/user/register") || path.startsWith("/user/token/verify")
        if (skip) return chain.proceed(request)
        val currentUser = runBlocking { MainApplication.getUserManager().getCurrentUser() }
        val builder = request.newBuilder()
        if (currentUser != null && currentUser.userId > 0L && currentUser.accessToken.isNotBlank()) {
            builder.header("user_id", currentUser.userId.toString())
            builder.header("access_token", currentUser.accessToken)
        }
        return chain.proceed(builder.build())
    }
}
