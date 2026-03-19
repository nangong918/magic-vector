package com.magicvector.utils.sort

import java.text.Collator
import java.util.Locale

object SortUtil {


    /**
     * 中文排序器（单例，避免重复创建）
     */
    private val chineseCollator: Collator = Collator.getInstance(Locale.CHINESE)

    /**
     * 根据排序模式查找插入位置
     */
    fun <T : SortItem> findInsertPosition(
        item: T,
        sortedList: MutableList<T>,
        mode: SortMode = SortMode.TIMESTAMP_DESC
    ): Int {
        return when (mode) {
            SortMode.TIMESTAMP_ASC -> ascFindInsertPosition(item.getIndex(), sortedList)
            SortMode.TIMESTAMP_DESC -> descFindInsertPosition(item.getIndex(), sortedList)
            SortMode.NAME_ASC -> ascFindInsertPosition(item.getStringIndex(), sortedList)
            SortMode.NAME_DESC -> descFindInsertPosition(item.getStringIndex(), sortedList)
            SortMode.CUSTOM -> sortedList.size // 自定义模式需单独处理
        }
    }

    /**
     * 二分查找找到插入位置（升序）
     * 二分查找：O(log m)，其中 m 是 chatList 的大小。
     * 插入操作：O(m)，在最坏情况下可能需要移动元素。
     * 总体时间复杂度：O(n + m)
     * O(n)，用于存储 timestampItemMap 和 chatList
     * 条件：对有序list进行排序
     * @param index 索引
     * @return  插入位置
     */
    fun <T : SortItem> ascFindInsertPosition(index: Long, sortItemList: MutableList<T>): Int {
        var low = 0
        var high = sortItemList.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            // 中间值比索引小：-----|--I---
            if (sortItemList[mid].getIndex() < index) {
                low = mid + 1 // 向右查找 (大的放右边)
            }
            // 中间值比索引大：--I---|-----
            else {
                high = mid - 1 // 向左查找
            }
        }
        return low // 返回插入位置
    }

    /**
     * 二分查找找到插入位置（降序）
     * 二分查找：O(log m)，其中 m 是 chatList 的大小。
     * 插入操作：O(m)，在最坏情况下可能需要移动元素。
     * 总体时间复杂度：O(n + m)
     * O(n)，用于存储 timestampItemMap 和 chatList
     * 条件：对有序list进行排序
     * @param index 索引
     * @return  插入位置
     */
    fun <T : SortItem> descFindInsertPosition(index: Long, sortItemList: MutableList<T>): Int {
        var low = 0
        var high = sortItemList.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            // 中间值比索引小：-----|--I---
            if (sortItemList[mid].getIndex() < index) {
                high = mid - 1 // 向左查找 (大的放左边)
            }
            // 中间值比索引大：--I---|-----
            else {
                low = mid + 1 // 向右查找
            }
        }
        return low // 返回插入位置
    }

    /**
     * 二分查找插入位置（字符串升序，支持中文）
     */
    fun <T : SortItem> ascFindInsertPosition(
        str: String,
        sortedList: MutableList<T>,
        collator: Collator = chineseCollator
    ): Int {
        var low = 0
        var high = sortedList.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            val compareResult = collator.compare(sortedList[mid].getStringIndex(), str)

            if (compareResult < 0) {  // 中间值 < 目标值
                low = mid + 1
            } else {  // 中间值 >= 目标值
                high = mid - 1
            }
        }
        return low
    }

    /**
     * 二分查找插入位置（字符串降序，支持中文）
     */
    fun <T : SortItem> descFindInsertPosition(
        str: String,
        sortedList: MutableList<T>,
        collator: Collator = chineseCollator
    ): Int {
        var low = 0
        var high = sortedList.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            val compareResult = collator.compare(sortedList[mid].getStringIndex(), str)

            if (compareResult < 0) {  // 中间值 < 目标值
                high = mid - 1  // 降序：大的放左边
            } else {  // 中间值 >= 目标值
                low = mid + 1   // 降序：小的放右边
            }
        }
        return low
    }

    /**
     * 先合并再整体排序
     */
    fun <T : SortItem> insertOrdered(
        newItems: List<T>,
        sortedList: MutableList<T>,
        mode: SortMode = SortMode.TIMESTAMP_DESC
    ): List<T> {
        // 先合并
        sortedList.addAll(newItems)

        // 再整体排序
        when (mode) {
            SortMode.TIMESTAMP_ASC -> sortedList.sortBy { it.getIndex() }
            SortMode.TIMESTAMP_DESC -> sortedList.sortByDescending { it.getIndex() }
            SortMode.NAME_ASC -> sortedList.sortWith(compareBy(chineseCollator) { it.getStringIndex() })
            SortMode.NAME_DESC -> sortedList.sortWith(compareByDescending(chineseCollator) { it.getStringIndex() })
            SortMode.CUSTOM -> {} // 自定义处理
        }

        return sortedList
    }
}