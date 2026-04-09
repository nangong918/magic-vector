package com.vectordemo.utils.network

import com.vectordemo.utils.json.GsonBean
import java.io.Serializable

class BaseResponse<T> : GsonBean, Serializable {
    var code: String? = null
    var message: String? = null
    var data: T? = null
}
