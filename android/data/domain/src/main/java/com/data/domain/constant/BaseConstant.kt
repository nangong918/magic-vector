package com.data.domain.constant

import android.Manifest
import com.data.domain.constant.chat.VisionTypeEnum
import com.data.domain.constant.chat.VisionUploadTypeEnum

class BaseConstant {

    // 常量
    object Constant {
        const val START_DELAY_TIME = 1200L
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

        // 连接超时：2200ms
        const val CONNECT_TIMEOUT = 2200L
        // 读取超时：10s
        const val READ_TIMEOUT = 10_000L
        // 写入超时：10s
        const val WRITE_TIMEOUT = 10_000L
        // 响应处理超时时间：30s
        const val CALL_TIMEOUT = 30_000L
    }

    // url
    object ConstantUrl {
        const val LOCAL_HOST = "192.168.1.2"
        const val TEST_HOST = "192.168.1.2"
        const val LOCAL_ADDRESS = "$LOCAL_HOST:48888"
        const val TEST_ADDRESS = "$TEST_HOST:48888"


        const val LOCAL_URL = "http://$LOCAL_ADDRESS";
        const val TEST_URL = "http://$TEST_ADDRESS";
        const val PROD_URL = "https://api.github.com";

        const val LOCAL_WS_URL = "ws://$LOCAL_ADDRESS";
        const val TEST_WS_URL = "ws://$TEST_ADDRESS";
        const val PROD_WS_URL = "wss://api.github.com";
    }

    object WSConstantUrl {
        const val AGENT_REALTIME_CHAT_URL = "/agent/realtime/chat"
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

    object AUDIO {
        const val REALTIME_CHAT_SAMPLE_RATE = 24_000
    }

    object VAD {
        const val SILENCE_DURATION_MS = 100L
    }

    object YOLO {
        const val FILTER_SIZE = 0.1f * 0.1f
        const val PERSON_CLS = 0

        const val OBJECT_SDIFF_W = 0.1f
        const val PERSON_COUNT_W = 0.1f
        const val OBJECT_COUNT_W = 0.05f

        const val PERSON_THRESHOLD_VALUE = 0.10f
        const val OBJECT_THRESHOLD_VALUE = 0.40f
    }

    object VISION {
        // vision上传方法: http, ws-fragment, rtmp
        val UPLOAD_METHOD = VisionUploadTypeEnum.HTTP
        val VISION_TYPE = VisionTypeEnum.IMAGE
        // 分片大小: 兼容ws和mqtt的最大限制
        const val FRAGMENT_SIZE = 16 * 1024 // 16kB大小
        const val WS_SHARD_UPLOAD_DELAY = 20L // 分片上传延迟50ms，避免网络拥塞
    }

    object UDP {
        const val PORT = 45000
        const val CHUNK_SIZE = 4 * 1024
        const val MAX_PACKET_SIZE = 64 * 1024
        const val MIN_FRAME_INTERVAL = 100L
        const val BITMAP_QUALITY = 70
        const val UDP_TIMEOUT = 5000
    }
}