package com.core.appcore.api.handler

interface SyncRequestCallback {
    // 记录错误，并记录网络请求结束一条
    fun onThrowable(throwable: Throwable?)

    // 全部请求执行完成
    fun onAllRequestSuccess()

    companion object {
        const val RESPONSE_BASE_ERROR: String = "ResponseTool.handleResponse存在问题"
    }
}