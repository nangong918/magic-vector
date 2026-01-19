package com.demo.cpp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.demo.cpp.domain.entity.jni.IntMsg
import com.demo.cpp.domain.entity.jni.JniEntity
import com.demo.cpp.manager.JniManager
import com.demo.cpp.manager.OnReceiveCppMessage
import com.demo.cpp.ui.theme.CppDemoTheme

class STLActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("cpp")
        }
    }

    // Native方法声明
    external fun testVector(): String
    external fun testMap(): String
    external fun testAlgorithm(): String
    external fun testSmartPointer(): String
    external fun testString(): String

    // 新增的数据结构测试
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
            CppDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    STLDemoScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@SuppressLint("ContextCastToActivity")
@Composable
fun STLDemoScreen(modifier: Modifier = Modifier) {
    val activity = androidx.compose.ui.platform.LocalContext.current as STLActivity
    val (result, setResult) = remember { mutableStateOf("点击按钮运行STL测试") }

    val onReceiveCppMessage = object : OnReceiveCppMessage{
        override fun onReceiveCppMessage(msg: IntMsg) {
            setResult(msg.value.toString())
        }
    }

    // 设置
    JniManager.onReceiveCppMessage = onReceiveCppMessage

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "C++ STL Demo",
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 基础容器测试
        Text(
            text = "基础容器测试:",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = { setResult(activity.testString()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("测试 String")
        }

        Button(
            onClick = { setResult(activity.testVector()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("测试 Vector")
        }

        Button(
            onClick = { setResult(activity.testList()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("测试 List")
        }

        Button(
            onClick = { setResult(activity.testDeque()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("测试 Deque")
        }

        // 关联容器测试
        Text(
            text = "关联容器测试:",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = { setResult(activity.testMap()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("测试 Map")
        }

        Button(
            onClick = { setResult(activity.testSet()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("测试 Set")
        }

        // 容器适配器测试
        Text(
            text = "容器适配器测试:",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = { setResult(activity.testStack()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("测试 Stack")
        }

        Button(
            onClick = { setResult(activity.testQueue()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("测试 Queue")
        }

        Button(
            onClick = { setResult(activity.testPriorityQueue()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("测试 PriorityQueue")
        }

        // 算法和智能指针
        Text(
            text = "算法和智能指针:",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = { setResult(activity.testAlgorithm()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Text("测试 Algorithm")
        }

        Button(
            onClick = { setResult(activity.testSmartPointer()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("测试 Smart Pointer")
        }

        // JNI 调用测试
        Text(
            text = "JNI调用测试:",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = {
                val entity = JniEntity()
                val result = JniManager.changeJavaValue(entity)
                if (result == 0) {
                    setResult(entity.toString())
                } else {
                    setResult("JNI调用失败！返回码：$result")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("测试 JNI 调用 Java 方法")
        }

        Button(
            onClick = {
                JniManager.getInstance().initIntMsgCallback()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("测试 C++ 主动推消息给 Java Init")
        }

        Button(
            onClick = {
                JniManager.getInstance().startPushIntMsg(1000)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("测试 C++ 主动推消息给 Java Send")
        }


        Button(
            onClick = {
                JniManager.getInstance().stopPushIntMsg()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("测试 C++ 主动推消息给 Java Stop")
        }

        // 结果显示
        Text(
            text = "测试结果:",
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = result,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun STLDemoScreenPreview() {
    CppDemoTheme {
        STLDemoScreen()
    }
}