package com.core.baseutil.network

interface OnResponseCallback<T> {
    fun onSuccess(response: T?)
    fun onError(throwable: Throwable?)
}