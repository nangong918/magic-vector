package com.demo.cpp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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

        // 测试按钮
        Button(
            onClick = { setResult(activity.testString()) },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("测试 String")
        }

        Button(
            onClick = { setResult(activity.testVector()) },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("测试 Vector")
        }

        Button(
            onClick = { setResult(activity.testMap()) },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("测试 Map")
        }

        Button(
            onClick = { setResult(activity.testAlgorithm()) },
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("测试 Algorithm")
        }

        Button(
            onClick = { setResult(activity.testSmartPointer()) },
            modifier = Modifier.padding(bottom = 16.dp)
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
            modifier = Modifier.padding(top = 8.dp)
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