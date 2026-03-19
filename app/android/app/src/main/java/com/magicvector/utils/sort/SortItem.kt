package com.magicvector.utils.sort

/**
 * 排序项接口
 *
 * 设计原则：
 * - 排序字段（timestamp/name）可能重复，用于决定元素在列表中的位置
 * - 唯一标识符（uid）必须唯一，用于精确匹配和更新/删除操作
 */
interface SortItem {
    /**
     * 唯一标识符 - 用于精确匹配、更新、删除
     * 每个Item必须有一个唯一的ID
     */
    fun getUid(): Long

    /**
     * 时间戳 - 用于时间排序
     * 可能重复（如同一个时间点发送的多条消息）
     */
    fun getTimestamp(): Long = 0L

    /**
     * 字符串索引（名称、标题等）- 用于名称排序
     * 可能重复（如同名用户、相同标题）
     */
    fun getStringIndex(): String = ""

    /**
     * 获取排序值（用于多字段排序的默认实现）
     */
    fun getSortValue(mode: SortMode): Comparable<*> {
        return when (mode) {
            SortMode.LONG_ASC, SortMode.LONG_DESC -> getTimestamp()
            SortMode.STRING_ASC, SortMode.STRING_DESC -> getStringIndex()
            else -> getTimestamp()
        }
    }
}