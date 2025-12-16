package com.demo.cpp

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

@Composable
fun STLDemoScreen(modifier: Modifier = Modifier) {
    val activity = androidx.compose.ui.platform.LocalContext.current as STLActivity
    val (result, setResult) = remember { mutableStateOf("点击按钮运行STL测试") }

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