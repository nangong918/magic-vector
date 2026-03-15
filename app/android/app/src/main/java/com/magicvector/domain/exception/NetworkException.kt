package com.magicvector.domain.exception

/**
 * 网络请求基础异常（所有网络相关异常的父类）
 */
open class NetworkException(message: String? = null, cause: Throwable? = null) : Throwable(message, cause)

/**
 * 业务异常（后端返回非成功码）
 * @param code 后端返回的错误码
 * @param msg 后端返回的错误信息
 */
class NetworkBusinessException(
    val code: String? = null,
    val msg: String? = null
) : NetworkException(message = "业务异常：code=$code, msg=$msg")

/**
 * 参数不合法异常（参数格式错误/长度超限/取值范围错误等）
 */
class NetworkParamIllegalException(message: String? = null) : NetworkException(message = message)