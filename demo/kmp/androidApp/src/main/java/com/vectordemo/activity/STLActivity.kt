package com.vectordemo.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vectordemo.domain.entity.kni.IntMsg
import com.vectordemo.domain.entity.kni.KniEntity
import com.vectordemo.manager.KniManager
import com.vectordemo.manager.OnReceiveCppMessage

class STLActivity : AppCompatActivity() {
    companion object {
        init {
            System.loadLibrary("vectordemo")
        }
    }

    external fun testVector(): String
    external fun testMap(): String
    external fun testAlgorithm(): String
    external fun testSmartPointer(): String
    external fun testString(): String
    external fun testList(): String
    external fun testSet(): String
    external fun testDeque(): String
    external fun testStack(): String
    external fun testQueue(): String
    external fun testPriorityQueue(): String

    private var cppMsgDialog: AlertDialog? = null
    private var latestPushText: String = "等待推送..."

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "STL / JNI Demo"
        setContentView(buildContentView())
        KniManager.onReceiveCppMessage = OnReceiveCppMessage { msg: IntMsg ->
            latestPushText = "IntMsg.value = ${msg.value}\n\n（推送中，关闭弹窗后不再自动弹出）"
            if (cppMsgDialog?.isShowing == true) {
                cppMsgDialog?.setMessage(latestPushText)
            }
        }
    }

    override fun onDestroy() {
        KniManager.instance.stopPushIntMsg()
        KniManager.onReceiveCppMessage = null
        super.onDestroy()
    }

    private fun buildContentView(): ScrollView {
        val root = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 32)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
        }
        root.addView(container)

        container.addView(sectionTitle("STL 容器/算法"))
        container.addView(action("测试 String") { showResult("String 测试", testString()) })
        container.addView(action("测试 Vector") { showResult("Vector 测试", testVector()) })
        container.addView(action("测试 List") { showResult("List 测试", testList()) })
        container.addView(action("测试 Deque") { showResult("Deque 测试", testDeque()) })
        container.addView(action("测试 Map") { showResult("Map 测试", testMap()) })
        container.addView(action("测试 Set") { showResult("Set 测试", testSet()) })
        container.addView(action("测试 Stack") { showResult("Stack 测试", testStack()) })
        container.addView(action("测试 Queue") { showResult("Queue 测试", testQueue()) })
        container.addView(action("测试 PriorityQueue") { showResult("PriorityQueue 测试", testPriorityQueue()) })
        container.addView(action("测试 Algorithm") { showResult("Algorithm 测试", testAlgorithm()) })
        container.addView(action("测试 SmartPointer") { showResult("SmartPointer 测试", testSmartPointer()) })

        container.addView(sectionTitle("KNI 调用"))
        container.addView(action("测试 KNI 调 Kotlin") {
            val entity = KniEntity()
            val code = KniManager.changeKotlinValue(entity)
            val text = if (code == 0) {
                "KNI 调用成功:\n$entity"
            } else {
                "KNI 调用失败，返回码=$code"
            }
            showResult("KNI 调用 Kotlin", text)
        })
        container.addView(action("测试 C++ 抛异常") {
            try {
                KniManager.throwFakeException()
                showResult("C++ 异常", "未捕获到异常（不应出现）")
            } catch (e: Exception) {
                showResult(
                    "C++ 异常",
                    "Kotlin 捕获到 C++ 抛出的异常:\n类型=${e.javaClass.name}\n消息=${e.message}",
                )
            }
        })
        container.addView(action("Init C++ 推送") {
            KniManager.instance.initIntMsgCallback()
            showResult("推送初始化", "JNI 回调环境已初始化。")
        })
        container.addView(action("Start C++ 推送") {
            latestPushText = "等待推送..."
            cppMsgDialog = AlertDialog.Builder(this)
                .setTitle("C++ 推送")
                .setMessage(latestPushText)
                .setPositiveButton("确定", null)
                .show()
            KniManager.instance.startPushIntMsg(1000)
        })
        container.addView(action("Stop C++ 推送") {
            KniManager.instance.stopPushIntMsg()
            showResult("C++ 推送", "推送已停止。")
        })
        return root
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 18f
            setPadding(0, 20, 0, 10)
        }
    }

    private fun action(text: String, block: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { block() }
        }
    }

    private fun showResult(title: String, content: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(content)
            .setPositiveButton("确定", null)
            .show()
    }
}
