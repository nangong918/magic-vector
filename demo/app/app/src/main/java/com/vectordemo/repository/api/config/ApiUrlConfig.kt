package com.vectordemo.repository.api.config

import com.vectordemo.domain.constant.BaseConstant

open class ApiUrlConfig {
    companion object {
        fun getUrl(): String = "${BaseConstant.ConstantUrl.LOCAL_URL}/"
    }
}
