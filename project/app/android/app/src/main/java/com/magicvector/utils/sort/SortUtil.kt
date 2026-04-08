package com.magicvector.utils.sort

import java.text.Collator
import java.util.Locale

object SortUtil {


    /**
     * 中文排序器（单例，避免重复创建）
     */
    val chineseCollator: Collator = Collator.getInstance(Locale.CHINESE)

    /// ========== 时间戳排序（timestamp，允许重复，旁边插入） ==========

    /**
     * 二分查找找到插入位置（时间戳升序）
     * 二分查找：O(log m)，其中 m 是列表的大小。
     * 插入操作：O(m)，在最坏情况下可能需要移动元素。
     * 条件：对有序list进行排序
     * 说明：时间戳可以重复，相同值在旁边插入（向左查找，实现稳定排序）
     *
     * @param timestamp 时间戳
     * @param sortedList 已排序列表
     * @return 插入位置
     */
    fun <T : SortItem> ascFindInsertPositionByTimestamp(
        timestamp: Long,
        sortedList: MutableList<T>
    ): Int {
        var low = 0
        var high = sortedList.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            // 中间值比时间戳小：-----|--I---
            if (sortedList[mid].getTimestamp() < timestamp) {
                low = mid + 1 // 向右查找 (大的放右边)
            }
            // 中间值比时间戳大或相等：--I---|-----  (相等时也向左，实现旁边插入)
            else {
                high = mid - 1 // 向左查找
            }
        }
        return low // 返回插入位置
    }

    /**
     * 二分查找找到插入位置（时间戳降序）
     * 二分查找：O(log m)，其中 m 是列表的大小。
     * 插入操作：O(m)，在最坏情况下可能需要移动元素。
     * 条件：对有序list进行排序
     * 说明：时间戳可以重复，相同值在旁边插入（向右查找，实现稳定排序）
     *
     * @param timestamp 时间戳
     * @param sortedList 已排序列表
     * @return 插入位置
     */
    fun <T : SortItem> descFindInsertPositionByTimestamp(
        timestamp: Long,
        sortedList: MutableList<T>
    ): Int {
        var low = 0
        var high = sortedList.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            // 中间值比时间戳小：-----|--I---
            if (sortedList[mid].getTimestamp() < timestamp) {
                high = mid - 1 // 向左查找 (大的放左边)
            }
            // 中间值比时间戳大或相等：--I---|-----  (相等时也向右，实现旁边插入)
            else {
                low = mid + 1 // 向右查找
            }
        }
        return low // 返回插入位置
    }

    /// ========== UID排序（uid，唯一值，重复则覆盖） ==========

    /**
     * 二分查找找到插入位置或已存在位置（UID升序）
     * 二分查找：O(log m)，其中 m 是列表的大小。
     * 插入操作：O(m)，在最坏情况下可能需要移动元素。
     * 条件：对有序list进行排序
     * 说明：UID唯一，如果存在相同UID则返回该位置（用于覆盖），否则返回插入点
     *
     * @param uid UID
     * @param sortedList 已排序列表
     * @return FindPositionResult：isFound表示是否找到，position表示索引/插入点（均为非负数）
     */
    fun <T : SortItem> ascFindPositionByUid(
        uid: Long,
        sortedList: MutableList<T>
    ): FindPositionResult {
        var low = 0
        var high = sortedList.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            val midUid = sortedList[mid].getUid()
            // 中间值比UID小：向右查找
            if (midUid < uid) {
                low = mid + 1
            }
            // 中间值比UID大：向左查找
            else if (midUid > uid) {
                high = mid - 1
            }
            // UID相等：找到，返回「找到=true + 对应索引」
            else {
                return FindPositionResult(isFound = true, position = mid)
            }
        }
        // 未找到：返回「找到=false + 插入位置」
        return FindPositionResult(isFound = false, position = low)
    }

    /**
     * 二分查找找到插入位置或已存在位置（UID降序）
     * 二分查找：O(log m)，其中 m 是列表的大小。
     * 插入操作：O(m)，在最坏情况下可能需要移动元素。
     * 条件：对有序list进行排序（降序）
     * 说明：UID唯一，如果存在相同UID则返回该位置（用于覆盖），否则返回插入点
     *
     * @param uid UID
     * @param sortedList 已按UID降序排列的列表
     * @return FindPositionResult：isFound表示是否找到，position表示索引/插入点（均为非负数）
     */
    fun <T : SortItem> descFindPositionByUid(
        uid: Long,
        sortedList: MutableList<T>
    ): FindPositionResult {
        var low = 0
        var high = sortedList.size - 1

        while (low <= high) {
            val mid = (low + high) / 2
            val midUid = sortedList[mid].getUid()

            // 降序逻辑：大的UID在左侧，小的在右侧
            // 中间值比目标UID小 → 目标应该在左侧（high左移）
            if (midUid < uid) {
                high = mid - 1
            }
            // 中间值比目标UID大 → 目标应该在右侧（low右移）
            else if (midUid > uid) {
                low = mid + 1
            }
            // UID相等：找到，返回「找到=true + 对应索引」
            else {
                return FindPositionResult(isFound = true, position = mid)
            }
        }
        // 未找到：返回「找到=false + 插入位置」（low即为降序下的正确插入点，非负数）
        return FindPositionResult(isFound = false, position = low)
    }


    // ========== 字符串排序（name，允许重复，旁边插入） ==========

    /**
     * 二分查找插入位置（字符串升序，支持中文）
     * 二分查找：O(log m)，其中 m 是列表的大小。
     * 插入操作：O(m)，在最坏情况下可能需要移动元素。
     * 条件：对有序list进行排序
     * 说明：字符串可以重复，相同值在旁边插入（向左查找，实现稳定排序）
     *
     * @param str 目标字符串
     * @param sortedList 已排序列表
     * @param collator 排序器（默认支持中文）
     * @return 插入位置
     */
    fun <T : SortItem> ascFindInsertPositionByString(
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
            } else {  // 中间值 >= 目标值（相等时也向左，实现旁边插入）
                high = mid - 1
            }
        }
        return low
    }

    /**
     * 二分查找插入位置（字符串降序，支持中文）
     * 二分查找：O(log m)，其中 m 是列表的大小。
     * 插入操作：O(m)，在最坏情况下可能需要移动元素。
     * 条件：对有序list进行排序
     * 说明：字符串可以重复，相同值在旁边插入（向右查找，实现稳定排序）
     *
     * @param str 目标字符串
     * @param sortedList 已排序列表
     * @param collator 排序器（默认支持中文）
     * @return 插入位置
     */
    fun <T : SortItem> descFindInsertPositionByString(
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
            } else {  // 中间值 >= 目标值（相等时也向右，实现旁边插入）
                low = mid + 1   // 降序：小的放右边
            }
        }
        return low
    }

    /**
     * 批量有序插入（逐个插入版）
     *
     * 时间复杂度分析：
     * - UID排序：O(m * n)，其中 m 是新元素数量，n 是原列表大小
     * - 时间戳/字符串排序：O(m * n)
     *
     * 说明：ArrayList 的 add(position, element) 需要移动元素，因此每个插入为 O(n)
     *
     * 使用建议：
     * - 单条或少量数据（< 10条）：使用本方法（稳定插入）
     * - 大量数据（如分页加载，> 10条）：使用 insertOrderedBatch 方法（先合并再排序）
     *
     * @param newItems 待插入的新元素列表
     * @param sortedList 目标列表（会被修改）
     * @param mode 排序模式
     * @param conflictResolver UID冲突时的解决策略，默认覆盖（返回新元素）
     * @param duplicateStrategy 时间戳/字符串重复时的处理策略，默认插入旁边（保持重复）
     * @return 修改后的有序列表
     */
    fun <T : SortItem> insertOrderedBatch(
        newItems: List<T>,
        sortedList: MutableList<T>,
        mode: SortMode,
        conflictResolver: (existing: T, new: T) -> T = { _, new -> new },
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.INSERT_ADJACENT
    ): List<T> {
        // 优化：空列表直接添加
        if (sortedList.isEmpty()) {
            sortedList.addAll(newItems)
            return sortedList
        }

        when (mode) {
            // ========== UID排序：唯一值，二分查找 + 覆盖/插入 ==========
            is SortMode.UidSort -> {
                val isDesc = mode.isDesc
                newItems.forEach { newItem ->
                    val result = if (isDesc) {
                        descFindPositionByUid(newItem.getUid(), sortedList)
                    } else {
                        ascFindPositionByUid(newItem.getUid(), sortedList)
                    }

                    if (result.isFound) {
                        // UID已存在：根据策略更新
                        val existing = sortedList[result.position]
                        val merged = conflictResolver(existing, newItem)
                        sortedList[result.position] = merged
                    } else {
                        // UID不存在：插入到正确位置
                        sortedList.add(result.position, newItem)
                    }
                }
            }

            // ========== 时间戳排序：允许重复，二分查找插入位置 ==========
            is SortMode.TimestampSort -> {
                val isDesc = mode.isDesc
                newItems.forEach { newItem ->
                    val timestamp = newItem.getTimestamp()

                    // 可选：去重逻辑
                    if (duplicateStrategy == DuplicateStrategy.SKIP_DUPLICATE) {
                        val exists = if (isDesc) {
                            // 检查是否存在相同时间戳
                            sortedList.any { it.getTimestamp() == timestamp }
                        } else {
                            sortedList.any { it.getTimestamp() == timestamp }
                        }
                        if (exists) return@forEach
                    }

                    val position = if (isDesc) {
                        descFindInsertPositionByTimestamp(timestamp, sortedList)
                    } else {
                        ascFindInsertPositionByTimestamp(timestamp, sortedList)
                    }
                    sortedList.add(position, newItem)
                }
            }

            // ========== 字符串排序：允许重复，二分查找插入位置 ==========
            is SortMode.StringSort -> {
                val isDesc = mode.isDesc
                val collator = mode.collator
                newItems.forEach { newItem ->
                    val str = newItem.getStringIndex()

                    // 可选：去重逻辑
                    if (duplicateStrategy == DuplicateStrategy.SKIP_DUPLICATE) {
                        val exists = if (isDesc) {
                            sortedList.any { collator.compare(it.getStringIndex(), str) == 0 }
                        } else {
                            sortedList.any { collator.compare(it.getStringIndex(), str) == 0 }
                        }
                        if (exists) return@forEach
                    }

                    val position = if (isDesc) {
                        descFindInsertPositionByString(str, sortedList, collator)
                    } else {
                        ascFindInsertPositionByString(str, sortedList, collator)
                    }
                    sortedList.add(position, newItem)
                }
            }
        }

        return sortedList
    }
}