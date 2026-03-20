package com.magicvector.utils.sort

// 封装返回结果的数据类：包含是否找到、位置两个核心信息
data class FindPositionResult(
    val isFound: Boolean, // 是否找到对应UID的元素
    val position: Int     // 找到则是元素索引，没找到则是插入位置
)
