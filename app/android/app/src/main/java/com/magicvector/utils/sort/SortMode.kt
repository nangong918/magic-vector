package com.magicvector.utils.sort

/**
 * 排序模式
 */
enum class SortMode {
    TIMESTAMP_ASC,      // 时间升序（旧到新）
    TIMESTAMP_DESC,     // 时间降序（新到旧）- 默认
    NAME_ASC,           // 名称升序（A-Z，支持中文）
    NAME_DESC,          // 名称降序（Z-A）
    CUSTOM              // 自定义排序
}