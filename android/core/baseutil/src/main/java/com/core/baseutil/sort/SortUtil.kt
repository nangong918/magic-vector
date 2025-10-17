package com.core.baseutil.sort

object SortUtil {

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


    data class TestItem(val value: Long) : SortItem {
        override fun getIndex(): Long = value
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val items = listOf(9L, 5L, 2L, 1L, 5L, 6L, 3L)
        val ascSortedList = mutableListOf<TestItem>()
        val descSortedList = mutableListOf<TestItem>()

        items.forEach { value ->
            val ascPosition = ascFindInsertPosition(value, ascSortedList)
            ascSortedList.add(ascPosition, TestItem(value))
            println("Asc插入 $value 到位置 $ascPosition → ${ascSortedList.map { it.value }}")
        }

        items.forEach { value ->
            val descPosition = descFindInsertPosition(value, descSortedList)
            descSortedList.add(descPosition, TestItem(value))
            println("Desc插入 $value 到位置 $descPosition → ${descSortedList.map { it.value }}")
        }

        println("最终结果: 升序：${ascSortedList.map { it.value }}, 降序：${descSortedList.map { it.value }}")

        /**
         * Asc插入 9 到位置 0 → [9]
         * Asc插入 5 到位置 0 → [5, 9]
         * Asc插入 2 到位置 0 → [2, 5, 9]
         * Asc插入 1 到位置 0 → [1, 2, 5, 9]
         * Asc插入 5 到位置 2 → [1, 2, 5, 5, 9]
         * Asc插入 6 到位置 4 → [1, 2, 5, 5, 6, 9]
         * Asc插入 3 到位置 2 → [1, 2, 3, 5, 5, 6, 9]
         * Desc插入 9 到位置 0 → [9]
         * Desc插入 5 到位置 1 → [9, 5]
         * Desc插入 2 到位置 2 → [9, 5, 2]
         * Desc插入 1 到位置 3 → [9, 5, 2, 1]
         * Desc插入 5 到位置 2 → [9, 5, 5, 2, 1]
         * Desc插入 6 到位置 1 → [9, 6, 5, 5, 2, 1]
         * Desc插入 3 到位置 4 → [9, 6, 5, 5, 3, 2, 1]
         * 最终结果: 升序：[1, 2, 3, 5, 5, 6, 9], 降序：[9, 6, 5, 5, 3, 2, 1]
         */
    }
}