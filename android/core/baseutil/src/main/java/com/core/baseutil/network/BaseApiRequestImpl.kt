package com.core.baseutil.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class BaseApiRequestImpl {

    // 协程
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun <T> sendRequestCallback(
        // suspend: 可挂起; 协程特性
        apiCall: suspend () -> T,
        successCallback: OnSuccessCallback<T>?,
        throwableCallback: OnThrowableCallback?
    ) {
        coroutineScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { apiCall() }
                // 返回成功的响应
                successCallback?.onResponse(response)
            } catch (throwable: Throwable) {
                // 返回错误信息
                throwableCallback?.callback(throwable)
            }
        }
    }

    // 取消协程作用域
    fun cancelRequests() {
        coroutineScope.cancel()
    }
}