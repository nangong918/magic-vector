package com.vectordemo.domain.model.oss

import com.vectordemo.domain.dto.http.request.MultipartPartPayload

/**
 * 相册选图结果：预览模型（Coil model，一般为 content Uri 字符串）+ 上传载荷。
 */
data class OssPickedImage(
    val previewModel: String,
    val uploadPayload: MultipartPartPayload,
)
