package com.data.domain.constant

import android.Manifest

class BaseConstant {

    // 常量
    object Constant {
        const val START_DELAY_TIME = 1500L
        // 头像最大大小 200 * 200 = 160 KB
        const val BITMAP_MAX_SIZE_AVATAR: Int = 200
        const val PACKAGE_NAME: String = "com.magicvector"

        const val CHAT_HISTORY_LIMIT_COUNT = 20
        const val MAX_AGENT_NAME_LENGTH = 20
    }

    // http
    object HttpConstant {
        // 请求是否加认证token前缀 最后要在拦截器检查去掉; 定义一些不像url的命名避免出现与后端路由重合
        const val AUTH_TOKEN_PREFIX: String = "/has-0!0-token"

        // 连接超时：5s
        const val CONNECT_TIMEOUT = 5L
        // 读取超时：10s
        const val READ_TIMEOUT = 10L
        // 写入超时：10s
        const val WRITE_TIMEOUT = 10L
        // 响应处理超时时间：30s
        const val CALL_TIMEOUT = 30L
    }

    // url
    object ConstantUrl {
        private const val LOCAL_ADDRESS = "192.168.1.7:48888"
        private const val TEST_ADDRESS = "192.168.1.7:48888"


        const val LOCAL_URL = "http://$LOCAL_ADDRESS";
        const val TEST_URL = "http://$TEST_ADDRESS";
        const val PROD_URL = "https://api.github.com";

        const val LOCAL_WS_URL = "ws://$LOCAL_ADDRESS";
        const val TEST_WS_URL = "ws://$TEST_ADDRESS";
        const val PROD_WS_URL = "wss://api.github.com";
    }

    object WSConstantUrl {
        const val AGENT_REALTIME_CHAT_URL = ConstantUrl.LOCAL_WS_URL + "/agent/realtime/chat"
    }

    object PermissionConstant {
        // permission
        val MUST_PERMISSIONS: Array<String> = arrayOf(
            Manifest.permission.CAMERA,
        )
        val NOT_MUST_PERMISSIONS: Array<String> = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
    }

    object NetworkCode {
        const val SUCCESS = 200
        const val SUCCESS_CODE: String = "200"
    }

    object VAD {
        const val SILENCE_DURATION_MS = 100L
    }
}