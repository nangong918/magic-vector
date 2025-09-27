package com.core.baseutil.network

import android.content.Context
import com.core.baseutil.ui.ToastUtils

open class ResponseUtil {
    val SUCCESS_CODE_STRING: String = "200"
    val SUCCESS_CODE: Int = 200

    fun <T> handleResponse(response: BaseResponse<T>?, context: Context?): Boolean {
        if (response != null) {
            if (SUCCESS_CODE_STRING == response.code) {
                return true
            } else {
                ToastUtils.showToastActivity(context, response.message)
                return false
            }
        } else {
            ToastUtils.showToastActivity(context, "Internet Error")
            return false
        }
    }

    fun <T> handleResponse(
        response: BaseResponse<T>?,
        onThrowableCallback: OnThrowableCallback
    ): Boolean {
        if (response != null) {
            if (SUCCESS_CODE_STRING == response.code) {
                return true
            } else {
                onThrowableCallback.callback(Throwable(response.message))
                return false
            }
        } else {
            onThrowableCallback.callback(Throwable("Internet Error"))
            return false
        }
    }
}