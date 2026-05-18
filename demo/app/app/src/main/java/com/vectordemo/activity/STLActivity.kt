package com.vectordemo.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vectordemo.domain.entity.kni.KniEntity
import com.vectordemo.manager.KniManager
import com.vectordemo.manager.OnReceiveCppMessage
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.ui.view.stl.StlResultDialog
import com.vectordemo.utils.activity.BaseComponentActivity

class STLActivity : BaseComponentActivity() {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VectorDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    STLDemoScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

private const val PUSH_RESULT_TITLE = "C++ 推送"

@SuppressLint("ContextCastToActivity")
@Composable
fun STLDemoScreen(modifier: Modifier = Modifier) {
    val activity = LocalContext.current as STLActivity
    var showResultDialog by remember { mutableStateOf(false) }
    var resultTitle by remember { mutableStateOf("") }
    var resultText by remember { mutableStateOf("") }

    fun presentResult(title: String, content: String, openDialog: Boolean = true) {
        resultTitle = title
        resultText = content
        if (openDialog) {
            showResultDialog = true
        }
    }

    val onReceiveCppMessage = OnReceiveCppMessage { msg ->
        val content = buildString {
            appendLine("IntMsg.value = ${msg.value}")
            appendLine()
            append("（推送中，数值会持续更新；关闭弹窗后不再自动弹出）")
        }
        presentResult(
            title = PUSH_RESULT_TITLE,
            content = content,
            openDialog = showResultDialog && resultTitle == PUSH_RESULT_TITLE
        )
    }

    KniManager.onReceiveCppMessage = onReceiveCppMessage

    StlResultDialog(
        visible = showResultDialog,
        title = resultTitle,
        content = resultText,
        onDismiss = { showResultDialog = false }
    )

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(text = "C++ STL Demo", modifier = Modifier.padding(bottom = 16.dp))

        Text(text = "基础容器测试:", modifier = Modifier.padding(bottom = 8.dp))

        Button(
            onClick = { presentResult("String 测试", activity.testString()) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("测试 String") }

        Button(
            onClick = { presentResult("Vector 测试", activity.testVector()) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("测试 Vector") }

        Button(
            onClick = { presentResult("List 测试", activity.testList()) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("测试 List") }

        Button(
            onClick = { presentResult("Deque 测试", activity.testDeque()) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) { Text("测试 Deque") }

        Text(text = "关联容器测试:", modifier = Modifier.padding(bottom = 8.dp))

        Button(
            onClick = { presentResult("Map 测试", activity.testMap()) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("测试 Map") }

        Button(
            onClick = { presentResult("Set 测试", activity.testSet()) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) { Text("测试 Set") }

        Text(text = "容器适配器测试:", modifier = Modifier.padding(bottom = 8.dp))

        Button(
            onClick = { presentResult("Stack 测试", activity.testStack()) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("测试 Stack") }

        Button(
            onClick = { presentResult("Queue 测试", activity.testQueue()) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("测试 Queue") }

        Button(
            onClick = { presentResult("PriorityQueue 测试", activity.testPriorityQueue()) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) { Text("测试 PriorityQueue") }

        Text(text = "算法和智能指针:", modifier = Modifier.padding(bottom = 8.dp))

        Button(
            onClick = { presentResult("Algorithm 测试", activity.testAlgorithm()) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("测试 Algorithm") }

        Button(
            onClick = { presentResult("Smart Pointer 测试", activity.testSmartPointer()) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) { Text("测试 Smart Pointer") }

        Text(text = "KNI 调用测试:", modifier = Modifier.padding(bottom = 8.dp))

        Button(
            onClick = {
                val entity = KniEntity()
                val code = KniManager.changeKotlinValue(entity)
                presentResult(
                    title = "KNI 调用 Kotlin",
                    content = if (code == 0) entity.toString() else "KNI 调用失败！返回码：$code"
                )
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("测试 KNI 调用 Kotlin 方法") }

        Button(
            onClick = {
                try {
                    KniManager.throwFakeException()
                    presentResult("C++ 假异常", "未捕获到异常（不应出现）")
                } catch (e: Exception) {
                    presentResult(
                        title = "C++ 假异常",
                        content = "Kotlin 捕获 C++ 假异常:\n\n" +
                            "类型: ${e.javaClass.name}\n" +
                            "消息: ${e.message}"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) { Text("测试 C++ 主动抛假异常") }

        Button(
            onClick = {
                KniManager.instance.initIntMsgCallback()
                presentResult("C++ 推送 Init", "JNI/KNI 回调环境已初始化。\n\n请先 Init，再 Send 开始推送。")
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("测试 C++ 主动推消息给 Kotlin Init") }

        Button(
            onClick = {
                presentResult(PUSH_RESULT_TITLE, "等待推送…")
                KniManager.instance.startPushIntMsg(1000)
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) { Text("测试 C++ 主动推消息给 Kotlin Send") }

        Button(
            onClick = {
                KniManager.instance.stopPushIntMsg()
                presentResult(PUSH_RESULT_TITLE, "推送已停止。")
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) { Text("测试 C++ 主动推消息给 Kotlin Stop") }
    }
}
