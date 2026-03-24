package com.magicvector.utils.network

interface OnSuccessCallback<T> {
    fun onResponse(response: T?)
}