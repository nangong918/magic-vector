package com.core.baseutil.network

import android.content.Context
import com.core.baseutil.ui.ToastUtils

open class ResponseUtil {

    companion object {
        val SUCCESS_CODE = 200
        val SUCCESS_CODE_STRING = "200"

        /**
         * 基础响应处理
         * @param response   响应
         * @param context   上下文
         * @return          处理结果
         * @param <T>       响应数据类型
         */
        fun <T> handleResponse(response: BaseResponse<T>?, context: Context): Boolean {
            if (response != null && response.code != null) {
                if (response.code == SUCCESS_CODE_STRING){
                    return true
                }
                else {
                    ToastUtils.showToastActivity(context, response.message)
                }
            }
            else {
                ToastUtils.showToastActivity(context, "服务器异常")
            }
            return false
        }

        /**
         * 带异常回调的响应处理
         * @param response   响应
         * @param context   上下文
         * @param onThrowableCallback   异常回调
         * @return          处理结果
         * @param <T>       响应数据类型
         */
        fun <T> handleResponse(
            response: BaseResponse<T>?,
            context: Context,
            onThrowableCallback: OnThrowableCallback
        ): Boolean {
            if (response != null && response.code != null) {
                if (response.code == SUCCESS_CODE_STRING){
                    return true
                }
                else {
                    ToastUtils.showToastActivity(context, response.message)
                    onThrowableCallback.callback(Throwable(response.message))
                }
            }
            else {
                ToastUtils.showToastActivity(context, "服务器异常")
                onThrowableCallback.callback(Throwable("服务器异常"))
            }
            return false
        }
    }

}