package com.magicvector

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.magicvector.utils.sort.SortItem
import com.magicvector.utils.sort.SortMode
import com.magicvector.utils.sort.SortUtil
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SortUtilAndroidTest {

    data class TestItem(
        val uid: Long,
        val timestamp: Long,
        val name: String = ""
    ) : SortItem {
        override fun getUid(): Long = uid
        override fun getTimestamp(): Long = timestamp
        override fun getStringIndex(): String = name
    }

    @Test
    fun testAllSortScenarios() {
        println("==================== SortUtil 完整测试 ====================")

        // 1. 时间戳升序测试
        testTimestampAsc()

        // 2. 时间戳降序测试
        testTimestampDesc()

        // 3. 字符串升序测试
        testStringAsc()

        // 4. 字符串降序测试
        testStringDesc()

        // 5. UID升序测试
        testUidAsc()

        // 6. UID降序测试
        testUidDesc()

        // 7. 批量插入测试（时间戳）
        testBatchInsertTimestamp()

        // 8. 批量插入测试（字符串）
        testBatchInsertString()

        // 9. 批量插入测试（UID去重覆盖）
        testBatchInsertUid()

        println("\n==================== 测试完成 ====================")
    }

    private fun testTimestampAsc() {
        println("\n===== 时间戳升序测试 =====")
        val numbers = listOf(9L, 5L, 2L, 1L, 5L, 6L, 3L)
        val list = mutableListOf<TestItem>()
        var counter = 0L

        numbers.forEach { timestamp ->
            val item = TestItem(++counter, timestamp)
            val position = SortUtil.ascFindInsertPositionByTimestamp(timestamp, list)
            list.add(position, item)
            println("插入 timestamp=$timestamp 到位置 $position → ${list.map { it.timestamp }}")
        }
        println("最终结果: ${list.map { it.timestamp }}")
    }

    private fun testTimestampDesc() {
        println("\n===== 时间戳降序测试 =====")
        val numbers = listOf(9L, 5L, 2L, 1L, 5L, 6L, 3L)
        val list = mutableListOf<TestItem>()
        var counter = 0L

        numbers.forEach { timestamp ->
            val item = TestItem(++counter, timestamp)
            val position = SortUtil.descFindInsertPositionByTimestamp(timestamp, list)
            list.add(position, item)
            println("插入 timestamp=$timestamp 到位置 $position → ${list.map { it.timestamp }}")
        }
        println("最终结果: ${list.map { it.timestamp }}")
    }

    private fun testStringAsc() {
        println("\n===== 字符串升序测试 =====")
        val names = listOf("张三", "李四", "王五", "赵六", "阿宝", "刘洋", "陈晨")
        val list = mutableListOf<TestItem>()
        var counter = 0L

        names.forEach { name ->
            val item = TestItem(++counter, counter, name)
            val position = SortUtil.ascFindInsertPositionByString(name, list)
            list.add(position, item)
            println("插入 \"$name\" 到位置 $position → ${list.map { it.name }}")
        }
        println("最终结果: ${list.map { it.name }}")
    }

    private fun testStringDesc() {
        println("\n===== 字符串降序测试 =====")
        val names = listOf("张三", "李四", "王五", "赵六", "阿宝", "刘洋", "陈晨")
        val list = mutableListOf<TestItem>()
        var counter = 0L

        names.forEach { name ->
            val item = TestItem(++counter, counter, name)
            val position = SortUtil.descFindInsertPositionByString(name, list)
            list.add(position, item)
            println("插入 \"$name\" 到位置 $position → ${list.map { it.name }}")
        }
        println("最终结果: ${list.map { it.name }}")
    }

    private fun testUidAsc() {
        println("\n===== UID升序测试 =====")
        val uids = listOf(9L, 5L, 2L, 1L, 5L, 6L, 3L)
        val list = mutableListOf<TestItem>()
        var counter = 0L

        uids.forEach { uid ->
            val item = TestItem(uid, ++counter)
            val result = SortUtil.ascFindPositionByUid(uid, list)
            if (result.isFound) {
                println("UID=$uid 已存在位置 ${result.position}，执行覆盖")
                list[result.position] = item
            } else {
                println("UID=$uid 不存在，插入到位置 ${result.position}")
                list.add(result.position, item)
            }
            println("当前列表: ${list.map { "uid=${it.uid}" }}")
        }
        println("最终结果: ${list.map { "uid=${it.uid}" }}")
    }

    private fun testUidDesc() {
        println("\n===== UID降序测试 =====")
        val uids = listOf(9L, 5L, 2L, 1L, 5L, 6L, 3L)
        val list = mutableListOf<TestItem>()
        var counter = 0L

        uids.forEach { uid ->
            val item = TestItem(uid, ++counter)
            val result = SortUtil.descFindPositionByUid(uid, list)
            if (result.isFound) {
                println("UID=$uid 已存在位置 ${result.position}，执行覆盖")
                list[result.position] = item
            } else {
                println("UID=$uid 不存在，插入到位置 ${result.position}")
                list.add(result.position, item)
            }
            println("当前列表: ${list.map { "uid=${it.uid}" }}")
        }
        println("最终结果: ${list.map { "uid=${it.uid}" }}")
    }

    private fun testBatchInsertTimestamp() {
        println("\n===== 批量插入测试（时间戳降序） =====")

        val existingList = mutableListOf(
            TestItem(1, 4, "李四"),
            TestItem(2, 6, "赵六"),
            TestItem(3, 7, "陈七")
        )

        val batchList = listOf(
            TestItem(101, 5, "王五"),
            TestItem(102, 3, "张三"),
            TestItem(103, 8, "刘八"),
            TestItem(104, 1, "阿一")
        )

        println("原列表: ${existingList.map { "${it.name}(${it.timestamp})" }}")
        println("待插入: ${batchList.map { "${it.name}(${it.timestamp})" }}")

        SortUtil.insertOrderedBatch(
            newItems = batchList,
            sortedList = existingList,
            mode = SortMode.TimestampSort.desc()
        )

        println("结果: ${existingList.map { "${it.name}(${it.timestamp})" }}")
    }

    private fun testBatchInsertString() {
        println("\n===== 批量插入测试（字符串升序） =====")

        val existingList = mutableListOf(
            TestItem(1, 4, "李四"),
            TestItem(2, 6, "赵六"),
            TestItem(3, 7, "陈七")
        )

        val batchList = listOf(
            TestItem(101, 5, "王五"),
            TestItem(102, 3, "张三"),
            TestItem(103, 8, "刘八"),
            TestItem(104, 1, "阿一")
        )

        println("原列表: ${existingList.map { it.name }}")
        println("待插入: ${batchList.map { it.name }}")

        SortUtil.insertOrderedBatch(
            newItems = batchList,
            sortedList = existingList,
            mode = SortMode.StringSort.asc()
        )

        println("结果: ${existingList.map { it.name }}")
    }

    private fun testBatchInsertUid() {
        println("\n===== 批量插入测试（UID升序，重复覆盖） =====")

        val existingList = mutableListOf(
            TestItem(1, 100, "旧数据1"),
            TestItem(2, 200, "旧数据2"),
            TestItem(3, 300, "旧数据3")
        )

        val batchList = listOf(
            TestItem(2, 250, "新数据2"),  // UID=2 重复，应该覆盖
            TestItem(4, 400, "新数据4"),  // UID=4 新数据
            TestItem(1, 150, "新数据1")   // UID=1 重复，应该覆盖
        )

        println("原列表: ${existingList.map { "uid=${it.uid}(${it.name})" }}")
        println("待插入: ${batchList.map { "uid=${it.uid}(${it.name})" }}")

        SortUtil.insertOrderedBatch(
            newItems = batchList,
            sortedList = existingList,
            mode = SortMode.UidSort.asc()
        )

        println("结果: ${existingList.map { "uid=${it.uid}(${it.name})" }}")
    }
}