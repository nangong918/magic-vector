package com.vectordemo.domain.exception

class NetworkBusinessException(val code: String? = null, val msg: String? = null) :
    NetworkException("业务异常：code=$code, msg=$msg")
