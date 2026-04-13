package com.vectordemo.domain.dto.http.response

/** [fileIdList] elements are decimal strings in JSON. */
data class OssUrlListResponse(
    val fileIdList: List<String>? = null,
    val urlList: List<String>? = null
)
