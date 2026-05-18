package com.vectordemo.repository.api

import io.ktor.client.HttpClient

expect fun createPlatformHttpClient(tokenProvider: () -> String?): HttpClient

/** 阿里云/讯飞等第三方 AI 接口专用，不注入业务登录 Token（对齐 demo/app 独立 OkHttpClient）。 */
fun createExternalAiHttpClient(): HttpClient = createPlatformHttpClient { null }
