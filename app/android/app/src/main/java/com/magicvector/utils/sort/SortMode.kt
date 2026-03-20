package com.magicvector.utils.sort

import java.text.Collator

/**
 * 排序模式
 *
 * 设计思路：
 * - LONG 排序：需要传入具体使用哪个 Long 字段（时间戳或 UID）
 * - STRING 排序：使用字符串字段
 * - CUSTOM：自定义排序
 */
sealed class SortMode {

    /**
     * UID排序（唯一值，重复覆盖）
     * @param isDesc 是否降序
     */
    data class UidSort(
        val isDesc: Boolean = true
    ) : SortMode() {
        companion object {
            /**
             * 按 UID 降序
             */
            fun desc() = UidSort(true)

            /**
             * 按 UID 升序
             */
            fun asc() = UidSort(false)
        }
    }

    /**
     * 时间戳排序（允许重复，旁边插入）
     * @param isDesc 是否降序
     */
    data class TimestampSort(
        val isDesc: Boolean = true
    ) : SortMode() {
        companion object {
            /**
             * 按时间戳降序（最新优先）
             */
            fun desc() = TimestampSort(true)

            /**
             * 按时间戳升序（最早优先）
             */
            fun asc() = TimestampSort(false)
        }
    }


    /**
     * 字符串类型排序（名称、标题等）
     * @param isDesc 是否降序
     * @param collator 排序器（默认支持中文）
     */
    data class StringSort(
        val isDesc: Boolean = false,
        val collator: Collator = SortUtil.chineseCollator
    ) : SortMode() {
        companion object {
            /**
             * 字符串升序（A-Z，支持中文）
             */
            fun asc() = StringSort(false)

            /**
             * 字符串降序（Z-A，支持中文）
             */
            fun desc() = StringSort(true)
        }
    }
}