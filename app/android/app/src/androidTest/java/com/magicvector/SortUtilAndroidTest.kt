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
        val uid: Long,           // 唯一标识符
        val timestamp: Long,      // 时间戳，用于排序
        val name: String = ""     // 名称，用于字符串排序
    ) : SortItem {
        override fun getUid(): Long = uid
        override fun getTimestamp(): Long = timestamp
        override fun getStringIndex(): String = name
    }

    @Test
    fun testAllSortScenarios() {
        println("==================== SortUtil 完整测试 ====================")

        // 1. 数字升序测试（基于timestamp）
        println("\n===== 数字升序测试 (timestamp) =====")
        val numbers = listOf(9L, 5L, 2L, 1L, 5L, 6L, 3L)
        val ascNumberList = mutableListOf<TestItem>()
        var counter = 0L
        numbers.forEach { value ->
            // uid使用自增计数器，确保唯一性
            val item = TestItem(++counter, value)
            val position = SortUtil.ascFindInsertPosition(value, ascNumberList) { it.getTimestamp() }
            ascNumberList.add(position, item)
            println("插入 timestamp=$value (uid=${item.uid}) 到位置 $position → ${ascNumberList.map { it.timestamp }}")
        }
        println("升序结果: ${ascNumberList.map { it.timestamp }}\n")
        assert(ascNumberList.map { it.timestamp } == listOf(1L, 2L, 3L, 5L, 5L, 6L, 9L))

        // 2. 数字降序测试（基于timestamp）
        println("===== 数字降序测试 (timestamp) =====")
        val descNumberList = mutableListOf<TestItem>()
        counter = 0L
        numbers.forEach { value ->
            val item = TestItem(++counter, value)
            val position = SortUtil.descFindInsertPosition(value, descNumberList) { it.getTimestamp() }
            descNumberList.add(position, item)
            println("插入 timestamp=$value (uid=${item.uid}) 到位置 $position → ${descNumberList.map { it.timestamp }}")
        }
        println("降序结果: ${descNumberList.map { it.timestamp }}\n")
        assert(descNumberList.map { it.timestamp } == listOf(9L, 6L, 5L, 5L, 3L, 2L, 1L))

        // 3. 中文升序测试
        println("===== 中文升序测试 =====")
        val chineseNames = listOf("张三", "李四", "王五", "赵六", "阿宝", "刘洋", "陈晨")
        val ascNameList = mutableListOf<TestItem>()
        counter = 0L
        chineseNames.forEach { name ->
            val item = TestItem(++counter, counter, name)  // timestamp用counter，实际测试中不重要
            val position = SortUtil.ascFindInsertPosition(name, ascNameList)
            ascNameList.add(position, item)
            println("插入 \"$name\" 到位置 $position → ${ascNameList.map { it.name }}")
        }
        println("中文升序结果: ${ascNameList.map { it.name }}\n")
        // 验证升序结果（拼音顺序）
        val expectedAsc = listOf("阿宝", "陈晨", "李四", "刘洋", "王五", "张三", "赵六")
        assert(ascNameList.map { it.name } == expectedAsc)

        // 4. 中文降序测试
        println("===== 中文降序测试 =====")
        val descNameList = mutableListOf<TestItem>()
        counter = 0L
        chineseNames.forEach { name ->
            val item = TestItem(++counter, counter, name)
            val position = SortUtil.descFindInsertPosition(name, descNameList)
            descNameList.add(position, item)
            println("插入 \"$name\" 到位置 $position → ${descNameList.map { it.name }}")
        }
        println("中文降序结果: ${descNameList.map { it.name }}\n")
        // 验证降序结果（拼音逆序）
        val expectedDesc = listOf("赵六", "张三", "王五", "刘洋", "李四", "陈晨", "阿宝")
        assert(descNameList.map { it.name } == expectedDesc)

        // 5. 批量插入测试
        println("===== 批量插入测试 =====")

        // 准备批量插入的数据：每个元素都有唯一的uid和timestamp
        val batchList = listOf(
            TestItem(101, 5, "王五"),
            TestItem(102, 3, "张三"),
            TestItem(103, 8, "刘八"),
            TestItem(104, 1, "阿一")
        )

        // 5.1 按时间戳降序插入
        val existingList = mutableListOf(
            TestItem(1, 4, "李四"),
            TestItem(2, 6, "赵六"),
            TestItem(3, 7, "陈七")
        )

        println("原列表: ${existingList.map { "${it.name}(${it.timestamp})" }}")
        println("待插入: ${batchList.map { "${it.name}(${it.timestamp})" }}")

        // 使用insertOrdered，默认LONG_DESC基于timestamp排序
        SortUtil.insertOrdered(batchList, existingList, SortMode.LONG_DESC)
        println("时间降序结果: ${existingList.map { "${it.name}(${it.timestamp})" }}")

        // 验证时间降序结果（timestamp从大到小）
        val expectedTimeDesc = listOf(
            "刘八(8)", "陈七(7)", "赵六(6)", "王五(5)",
            "李四(4)", "张三(3)", "阿一(1)"
        )
        assert(existingList.map { "${it.name}(${it.timestamp})" } == expectedTimeDesc)

        // 5.2 按名称升序插入（重新创建列表）
        val nameSortedList = mutableListOf(
            TestItem(1, 4, "李四"),
            TestItem(2, 6, "赵六"),
            TestItem(3, 7, "陈七")
        )

        println("\n重新创建列表: ${nameSortedList.map { "${it.name}(${it.timestamp})" }}")

        // 使用insertOrdered按名称升序排序
        SortUtil.insertOrdered(batchList, nameSortedList, SortMode.STRING_ASC)
        println("名称升序结果: ${nameSortedList.map { it.name }}")

        // 验证名称升序结果（拼音顺序）
        val expectedNameAsc = listOf("阿一", "陈七", "李四", "刘八", "王五", "张三", "赵六")
        assert(nameSortedList.map { it.name } == expectedNameAsc)

        // 6. 测试基于UID的排序（特殊场景）
        println("\n===== 基于UID排序测试 =====")
        val uidList = mutableListOf(
            TestItem(5, 100, "Item5"),
            TestItem(2, 200, "Item2"),
            TestItem(8, 300, "Item8"),
            TestItem(1, 400, "Item1")
        )

        println("原始列表: ${uidList.map { "uid=${it.uid}(${it.name})" }}")

        // 按UID升序排序
        SortUtil.insertOrdered(
            newItems = emptyList(),  // 不添加新元素，只排序
            sortedList = uidList,
            mode = SortMode.LONG_ASC,
            longSelector = { it.getUid() }  // 使用uid排序
        )
        println("按UID升序结果: ${uidList.map { "uid=${it.uid}(${it.name})" }}")

        val expectedUidAsc = listOf("uid=1(Item1)", "uid=2(Item2)", "uid=5(Item5)", "uid=8(Item8)")
        assert(uidList.map { "uid=${it.uid}(${it.name})" } == expectedUidAsc)

        println("\n==================== 所有测试通过 ====================")
    }
    /**
     测试结果：
     ===== 数字升序测试 =====
     插入 9 到位置 0 → [9]
     插入 5 到位置 0 → [5, 9]
     插入 2 到位置 0 → [2, 5, 9]
     插入 1 到位置 0 → [1, 2, 5, 9]
     插入 5 到位置 2 → [1, 2, 5, 5, 9]
     插入 6 到位置 4 → [1, 2, 5, 5, 6, 9]
     插入 3 到位置 2 → [1, 2, 3, 5, 5, 6, 9]
     升序结果: [1, 2, 3, 5, 5, 6, 9]
     ===== 数字降序测试 =====
     插入 9 到位置 0 → [9]
     插入 5 到位置 1 → [9, 5]
     插入 2 到位置 2 → [9, 5, 2]
     插入 1 到位置 3 → [9, 5, 2, 1]
     插入 5 到位置 2 → [9, 5, 5, 2, 1]
     插入 6 到位置 1 → [9, 6, 5, 5, 2, 1]
     插入 3 到位置 4 → [9, 6, 5, 5, 3, 2, 1]
     降序结果: [9, 6, 5, 5, 3, 2, 1]
     ===== 中文升序测试 =====
     插入 "张三" 到位置 0 → [张三]
     插入 "李四" 到位置 0 → [李四, 张三]
     插入 "王五" 到位置 1 → [李四, 王五, 张三]
     插入 "赵六" 到位置 3 → [李四, 王五, 张三, 赵六]
     插入 "阿宝" 到位置 0 → [阿宝, 李四, 王五, 张三, 赵六]
     插入 "刘洋" 到位置 2 → [阿宝, 李四, 刘洋, 王五, 张三, 赵六]
     插入 "陈晨" 到位置 1 → [阿宝, 陈晨, 李四, 刘洋, 王五, 张三, 赵六]
     中文升序结果: [阿宝, 陈晨, 李四, 刘洋, 王五, 张三, 赵六]
     ===== 中文降序测试 =====
     插入 "张三" 到位置 0 → [张三]
     插入 "李四" 到位置 1 → [张三, 李四]
     插入 "王五" 到位置 1 → [张三, 王五, 李四]
     插入 "赵六" 到位置 0 → [赵六, 张三, 王五, 李四]
     插入 "阿宝" 到位置 4 → [赵六, 张三, 王五, 李四, 阿宝]
     插入 "刘洋" 到位置 3 → [赵六, 张三, 王五, 刘洋, 李四, 阿宝]
     插入 "陈晨" 到位置 5 → [赵六, 张三, 王五, 刘洋, 李四, 陈晨, 阿宝]
     中文降序结果: [赵六, 张三, 王五, 刘洋, 李四, 陈晨, 阿宝]
     ===== 批量插入测试 =====
     原列表: [李四(4), 赵六(6), 陈七(7)]
     待插入: [王五(5), 张三(3), 刘八(8), 阿一(1)]
     时间降序结果: [刘八(8), 陈七(7), 赵六(6), 王五(5), 李四(4), 张三(3), 阿一(1)]
     名称升序结果: [阿一, 陈七, 李四, 刘八, 王五, 张三, 赵六]
     */
}