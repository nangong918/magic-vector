package com.core.baseutil.network

interface OnSuccessCallback<T> {
    fun onResponse(response: T?)
}