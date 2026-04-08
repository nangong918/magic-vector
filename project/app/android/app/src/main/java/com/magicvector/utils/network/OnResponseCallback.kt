package com.magicvector.utils.network

interface OnResponseCallback<T> {
    fun onSuccess(response: T?)
    fun onError(throwable: Throwable?)
}