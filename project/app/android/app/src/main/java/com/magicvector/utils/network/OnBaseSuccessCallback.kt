package com.magicvector.utils.network

interface OnBaseSuccessCallback<T> {
    fun onResponse(response: BaseResponse<T>?)
}