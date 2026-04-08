package com.magicvector.utils.sort

/**
 * 重复数据处理策略
 */
enum class DuplicateStrategy {
    INSERT_ADJACENT,  // 插入在旁边（允许重复）
    SKIP_DUPLICATE,   // 跳过重复项
    REPLACE_OLD       // 替换旧项（仅用于UID）
}