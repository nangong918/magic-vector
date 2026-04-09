package com.vectordemo.domain.constant

class BaseConstant {
    object Constant {
        const val START_DELAY_TIME = 1200L
        const val BITMAP_MAX_SIZE_AVATAR = 200
    }

    object HttpConstant {
        const val CONNECT_TIMEOUT = 2200L
        const val READ_TIMEOUT = 10_000L
        const val WRITE_TIMEOUT = 10_000L
        const val CALL_TIMEOUT = 30_000L
    }

    object ConstantUrl {
        const val LOCAL_HOST = "192.168.1.2"
        const val LOCAL_ADDRESS = "$LOCAL_HOST:48888"
        const val LOCAL_URL = "http://$LOCAL_ADDRESS"
    }

    object NetworkCode {
        const val SUCCESS_CODE = "200"
    }
}
