package com.vectordemo.domain.exception

open class NetworkException(message: String? = null, cause: Throwable? = null) : Throwable(message, cause)

class NetworkBusinessException(val code: String? = null, val msg: String? = null) :
    NetworkException("业务异常：code=$code, msg=$msg")

class NetworkParamIllegalException(message: String? = null) : NetworkException(message)
