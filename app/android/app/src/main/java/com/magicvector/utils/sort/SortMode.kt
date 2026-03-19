package com.magicvector.utils.sort

/**
 * 排序模式
 */
enum class SortMode {
    LONG_ASC,      // Long升序（旧到新）
    LONG_DESC,     // Long降序（新到旧）- 默认
    STRING_ASC,           // 名称升序（A-Z，支持中文）
    STRING_DESC,          // 名称降序（Z-A）
    CUSTOM              // 自定义排序
}