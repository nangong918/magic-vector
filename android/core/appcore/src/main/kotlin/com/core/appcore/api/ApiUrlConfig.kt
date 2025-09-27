package com.core.appcore.api

import android.util.Log
import com.core.baseutil.debug.DebugEnvironment
import com.data.domain.constant.BaseConstant


open class ApiUrlConfig {

    // static 方法
    companion object {
        private val TAG: String = ApiUrlConfig::class.java.name

        // 获取 URL
        fun getMainUrl(): String {
            return when (DebugEnvironment.projectEnvironment) {
                DebugEnvironment.Environment.LOCAL -> BaseConstant.ConstantUrl.LOCAL_URL
                DebugEnvironment.Environment.TEST -> BaseConstant.ConstantUrl.TEST_URL
                DebugEnvironment.Environment.STAGING, DebugEnvironment.Environment.PRODUCTION -> BaseConstant.ConstantUrl.PROD_URL
            }
        }

        // Retrofit2 的baseUlr 必须以 /结束，不然会抛出一个IllegalArgumentException
        fun getUrl(): String {
            val builder = StringBuilder()
            builder.append(getMainUrl()) //.append("/api/")
                .append("/")

            Log.d(TAG, "builder:$builder")
            return builder.toString()
        }
    }
}