package com.vectordemo.repository.api

import com.vectordemo.domain.constant.BaseConstant
import io.ktor.client.HttpClient

fun createApiRequest(
    http: HttpClient,
    baseUrl: String = BaseConstant.ConstantUrl.LOCAL_URL,
): ApiRequest = ApiRequestImpl(http, baseUrl)
