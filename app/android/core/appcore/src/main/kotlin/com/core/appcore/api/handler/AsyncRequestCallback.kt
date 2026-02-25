package com.core.appcore.api.handler

interface AsyncRequestCallback {
    companion object {
        const val RESPONSE_BASE_ERROR: String = "ResponseTool.handleResponse存在问题"
    }
    // 记录错误，并记录网络请求结束一条
    fun onThrowable(throwable: Throwable?)

    // 记录网络请求结束一条
    fun onSingleRequestSuccess()
}