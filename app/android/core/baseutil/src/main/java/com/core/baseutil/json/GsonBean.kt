package com.core.baseutil.json

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject

interface GsonBean {

    companion object {
        // 使用 lazy 初始化 Gson 实例
        val GSON: Gson by lazy {
            GsonBuilder()
                .setPrettyPrinting() // 可根据需要添加更多配置
                .create()
        }

        // 可用于自定义 Gson 实例
        fun createCustomGson(configure: GsonBuilder.() -> Unit): Gson {
            return GsonBuilder().apply(configure).create()
        }
    }

    fun toJsonString(gson: Gson): String? {
        try {
            // 使用格式化输出
            return gson.toJson(toJson())
        } catch (e: Exception) {
            Log.e(
                GsonBean::class.java.simpleName,
                "JSON 转换异常: " + e.message,
                e
            )
            return ""
        }
    }

    fun toJsonString(): String? {
        try {
            // 使用格式化输出
            return GSON.toJson(toJson())
        } catch (e: Exception) {
            Log.e(
                GsonBean::class.java.simpleName,
                "JSON 转换异常: " + e.message,
                e
            )
            return ""
        }
    }

    fun toJson(): JsonObject? {
        // 将当前对象转换为 JsonObject
        return GSON.toJsonTree(this).getAsJsonObject()
    }

}