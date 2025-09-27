package com.core.baseutil.network

interface OnBaseSuccessCallback<T> {
    fun onResponse(response: BaseResponse<T>?)
}