package com.core.appcore.utils

import android.content.Context
import com.core.appcore.api.handler.AsyncRequestCallback
import com.core.appcore.api.handler.SyncRequestCallback
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.ResponseUtil

object AppResponseUtil : ResponseUtil(){

    val TAG = AppResponseUtil::class.simpleName

    /**
     * 基础链式同步响应处理
     * @param response      响应
     * @param context       上下文
     * @param callback      回调
     * @return              处理结果
     * @param <T>           响应数据类型
     */
    fun <T> handleAsyncResponse(response: BaseResponse<T>?, context: Context, callback: AsyncRequestCallback) : Boolean {
        val result = handleResponse(response, context)
        if (result) {
            callback.onSingleRequestSuccess()
        }
        else {
            callback.onThrowable(Throwable(AsyncRequestCallback.RESPONSE_BASE_ERROR))
        }
        return result
    }

    /**
     * 基础并发同步响应处理
     * @param response      响应
     * @param context       上下文
     * @param callback      并发回调
     * @param handler       处理方法
     * @param <T>           响应数据类型
     */
    fun <T> handleAsyncResponseEx(
        response: BaseResponse<T>?,
        context: Context,
        callback: AsyncRequestCallback,
        handler: (BaseResponse<T>?, Context) -> Unit
    ){
        val result = handleResponse(response, context)
        if (result) {
            handler(response, context)
        }
        else {
            callback.onThrowable(Throwable(AsyncRequestCallback.RESPONSE_BASE_ERROR))
        }
    }

    /**
     * 带参数的并发同步响应处理
     * @param response      响应
     * @param context       上下文
     * @param param         参数
     * @param callback      并发回调
     * @param handler       处理方法
     * @param <T>           响应数据类型
     */
    fun <T> handleAsyncResponseEx(
        response: BaseResponse<T>?,
        context: Context,
        param: Any?,
        callback: AsyncRequestCallback,
        handler: (BaseResponse<T>?, Context, Any?) -> Unit
    ){
        val result = handleResponse(response, context)
        if (result) {
            handler(response, context, param)
            callback.onSingleRequestSuccess()
        }
        else {
            callback.onThrowable(Throwable(AsyncRequestCallback.RESPONSE_BASE_ERROR))
        }
    }

    /**
     * 手动控制响应结果的带参数的并发同步响应处理
     * @param response      响应
     * @param context       上下文
     * @param param         参数
     * @param callback      并发回调
     * @param handler       处理方法
     * @param <T>           响应数据类型
     */
    fun <T> handleAsyncResponseEx(
        response: BaseResponse<T>?,
        context: Context?,
        param: Any?,
        callback: AsyncRequestCallback,
        handler: (BaseResponse<T>?, Context?, AsyncRequestCallback, Any?) -> Unit
    ) {
        val result = ResponseTool.handleResponse<T>(response, context)
        if (result) {
            handler(response, context, callback, param)
            // 此处改为手动回调
//            callback.onSingleRequestSuccess();
        } else {
            callback.onThrowable(Throwable(AsyncRequestCallback.RESPONSE_BASE_ERROR))
        }
    }

    /**
     * 同步响应处理
     * @param response      响应
     * @param context       上下文
     * @param callback      回调
     * @param handler       处理方法
     * @param <T>           响应数据类型
     */
    fun <T> handleSyncResponseEx(
        response: BaseResponse<T>?,
        context: Context,
        callback: SyncRequestCallback,
        handler: (BaseResponse<T>?, Context) -> Unit
    ){
        val result = handleResponse(response, context)
        if (result) {
            handler(response, context)
        }
        else {
            callback.onThrowable(Throwable(AsyncRequestCallback.RESPONSE_BASE_ERROR))
        }
    }

    /**
     * 同步响应处理
     * @param response      响应
     * @param context       上下文
     * @param callback      回调
     * @param handler       处理方法
     * @param <T>           响应数据类型
     */
    fun <T> handleSyncResponseEx(
        response: BaseResponse<T>?,
        context: Context,
        callback: SyncRequestCallback,
        handler: (BaseResponse<T>?, Context, SyncRequestCallback) -> Unit
    ){
        val result = handleResponse(response, context)
        if (result) {
            handler(response, context, callback)
        }
        else {
            callback.onThrowable(Throwable(AsyncRequestCallback.RESPONSE_BASE_ERROR))
        }
    }

    /**
     * 同步响应处理
     * @param response      响应
     * @param context       上下文
     * @param callback      回调
     * @param param         参数
     * @param handler       处理方法
     * @param <T>           响应数据类型
     */
    fun <T> handleSyncResponseEx(
        response: BaseResponse<T>?,
        context: Context,
        callback: SyncRequestCallback,
        param: Any?,
        handler: (BaseResponse<T>?, Context, SyncRequestCallback, Any?) -> Unit,
    ){
        val result = handleResponse(response, context)
        if (result) {
            handler(response, context, callback, param)
        }
        else {
            callback.onThrowable(Throwable(AsyncRequestCallback.RESPONSE_BASE_ERROR))
        }
    }
}