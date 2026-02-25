package com.core.baseutil.network

import com.core.baseutil.json.GsonBean
import java.io.Serializable

class BaseResponse<T> : GsonBean, Serializable {
    // 泛型字段，用于存放具体的数据
    var code: String? = null
    var message: String? = null
    var data: T? = null
}