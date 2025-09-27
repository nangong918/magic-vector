package com.data.dao

import com.core.appcore.api.ApiRequest
import com.core.baseutil.network.BaseApiRequestImpl

open class ApiRequestImpl(apiRequest: ApiRequest) : BaseApiRequestImpl() {

    // mApi 可以直接使用构造函数参数
    private val mApi: ApiRequest = apiRequest

}