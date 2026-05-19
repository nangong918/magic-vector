package com.vectordemo.repository.api

import com.vectordemo.domain.constant.BaseConstant
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient

fun createApiRequest(
    http: HttpClient,
    baseUrl: String = BaseConstant.ConstantUrl.LOCAL_URL,
): ApiRequest {
    val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    return Ktorfit.Builder()
        .baseUrl(normalizedBaseUrl)
        .httpClient(http)
        .build()
        .createApiRequest()
}
