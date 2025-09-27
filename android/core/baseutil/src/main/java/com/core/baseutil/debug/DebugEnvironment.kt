package com.core.baseutil.debug

object DebugEnvironment {
    // Debug状态或者Release状态
    var projectEnvironment: Environment = Environment.TEST

    enum class Environment(val code: String, val description: String) {
        LOCAL("local", "本地环境"),
        TEST("test", "测试环境"),
        STAGING("staging", "正式(测试)环境"),
        PRODUCTION("production", "正式(线上)环境");

        companion object {
            fun fromCode(code: String): Environment {
                for (env in entries) {
                    if (env.code.equals(code, ignoreCase = true)) {
                        return env
                    }
                }
                throw IllegalArgumentException("未知的环境代码: $code")
            }
        }
    }
}
