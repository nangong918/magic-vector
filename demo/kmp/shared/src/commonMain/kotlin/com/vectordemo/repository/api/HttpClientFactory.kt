package com.vectordemo.repository.api

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient(tokenProvider: () -> String?): HttpClient
