package com.vectordemo.domain.constant

object BaseConstant {
    object Constant {
        const val START_DELAY_TIME = 1200L
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
