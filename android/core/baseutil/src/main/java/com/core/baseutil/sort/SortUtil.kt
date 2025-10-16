package com.core.baseutil.sort

object SortUtil {

    /**
     * 二分查找找到插入位置
     * 二分查找：O(log m)，其中 m 是 chatList 的大小。
     * 插入操作：O(m)，在最坏情况下可能需要移动元素。
     * 总体时间复杂度：O(n + m)
     * O(n)，用于存储 timestampItemMap 和 chatList
     * 条件：对有序list进行排序
     * @param index 索引
     * @return  插入位置
     */
    fun <T : SortItem> findInsertPosition(index: Long, sortItemList: MutableList<T>): Int {
        var low = 0
        var high = sortItemList.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            if (sortItemList[mid].getIndex() < index) {
                low = mid + 1 // 向右查找
            } else {
                high = mid - 1 // 向左查找
            }
        }
        return low // 返回插入位置
    }

    fun <T : SortItem> findInsertPosition(item: T?, sortItemList: MutableList<T>): Int? {
        if (item == null) {
            return null
        }

        var low = 0
        var high = sortItemList.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            val midItem = sortItemList[mid]

            // 检查是否存在相同的消息
            if (item == midItem) {
                return null // 找到相同的消息，返回 null
            }

            if (midItem.getIndex() < item.getIndex()) {
                low = mid + 1 // 向右查找
            } else {
                high = mid - 1 // 向左查找
            }
        }
        return low // 返回插入位置
    }

}