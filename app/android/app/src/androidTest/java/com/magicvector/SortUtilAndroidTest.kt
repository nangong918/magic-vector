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
        val value: Long,
        val name: String = ""
    ) : SortItem {
        override fun getIndex(): Long = value
        override fun getStringIndex(): String = name
    }

    @Test
    fun testAllSortScenarios() {
        println("==================== SortUtil 完整测试 ====================")

        // 1. 数字升序测试
        println("\n===== 数字升序测试 =====")
        val numbers = listOf(9L, 5L, 2L, 1L, 5L, 6L, 3L)
        val ascNumberList = mutableListOf<TestItem>()
        numbers.forEach { value ->
            val position = SortUtil.ascFindInsertPosition(value, ascNumberList)
            ascNumberList.add(position, TestItem(value))
            println("插入 $value 到位置 $position → ${ascNumberList.map { it.value }}")
        }
        println("升序结果: ${ascNumberList.map { it.value }}\n")
        assert(ascNumberList.map { it.value } == listOf(1L, 2L, 3L, 5L, 5L, 6L, 9L))

        // 2. 数字降序测试
        println("===== 数字降序测试 =====")
        val descNumberList = mutableListOf<TestItem>()
        numbers.forEach { value ->
            val position = SortUtil.descFindInsertPosition(value, descNumberList)
            descNumberList.add(position, TestItem(value))
            println("插入 $value 到位置 $position → ${descNumberList.map { it.value }}")
        }
        println("降序结果: ${descNumberList.map { it.value }}\n")
        assert(descNumberList.map { it.value } == listOf(9L, 6L, 5L, 5L, 3L, 2L, 1L))

        // 3. 中文升序测试
        println("===== 中文升序测试 =====")
        val chineseNames = listOf("张三", "李四", "王五", "赵六", "阿宝", "刘洋", "陈晨")
        val ascNameList = mutableListOf<TestItem>()
        chineseNames.forEach { name ->
            val position = SortUtil.ascFindInsertPosition(name, ascNameList)
            ascNameList.add(position, TestItem(0, name))
            println("插入 \"$name\" 到位置 $position → ${ascNameList.map { it.name }}")
        }
        println("中文升序结果: ${ascNameList.map { it.name }}\n")

        // 4. 中文降序测试
        println("===== 中文降序测试 =====")
        val descNameList = mutableListOf<TestItem>()
        chineseNames.forEach { name ->
            val position = SortUtil.descFindInsertPosition(name, descNameList)
            descNameList.add(position, TestItem(0, name))
            println("插入 \"$name\" 到位置 $position → ${descNameList.map { it.name }}")
        }
        println("中文降序结果: ${descNameList.map { it.name }}\n")

        // 5. 批量插入测试（使用修正后的方法）
        println("===== 批量插入测试 =====")
        val batchList = listOf(
            TestItem(5, "王五"),
            TestItem(3, "张三"),
            TestItem(8, "刘八"),
            TestItem(1, "阿一")
        )

        // 5.1 按时间降序插入
        val existingList = mutableListOf(
            TestItem(4, "李四"),
            TestItem(6, "赵六"),
            TestItem(7, "陈七")
        )

        println("原列表: ${existingList.map { "${it.name}(${it.value})" }}")
        println("待插入: ${batchList.map { "${it.name}(${it.value})" }}")

        SortUtil.insertOrdered(batchList, existingList, SortMode.TIMESTAMP_DESC)
        println("时间降序结果: ${existingList.map { "${it.name}(${it.value})" }}")


        // 5.2 按名称升序插入
        val nameSortedList = mutableListOf(
            TestItem(4, "李四"),
            TestItem(6, "赵六"),
            TestItem(7, "陈七")
        )
        SortUtil.insertOrdered(batchList, nameSortedList, SortMode.NAME_ASC)
        println("名称升序结果: ${nameSortedList.map { it.name }}")


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