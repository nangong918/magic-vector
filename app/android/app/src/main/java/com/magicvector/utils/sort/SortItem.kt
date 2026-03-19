package com.magicvector.utils.sort

interface SortItem {
    /**
     * 数字索引（时间戳、ID等）
     */
    fun getIndex(): Long = 0L

    /**
     * 字符串索引（名称、标题等）
     */
    fun getStringIndex(): String = ""

    /**
     * 获取排序值（用于多字段排序的默认实现）
     */
    fun getSortValue(mode: SortMode): Comparable<*> {
        return when (mode) {
            SortMode.TIMESTAMP_ASC, SortMode.TIMESTAMP_DESC -> getIndex()
            SortMode.NAME_ASC, SortMode.NAME_DESC -> getStringIndex()
            else -> getIndex()
        }
    }
}