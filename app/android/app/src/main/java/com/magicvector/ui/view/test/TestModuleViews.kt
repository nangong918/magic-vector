package com.magicvector.ui.view.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun TestSectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .background(Color(0xFFEFEFEF), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
fun TestStateLine(title: String, value: String) {
    Text("$title$value", style = MaterialTheme.typography.bodyMedium)
}

@Composable
fun TwoActionButtons(
    leftText: String,
    rightText: String,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onLeftClick) { Text(leftText) }
        Button(onClick = onRightClick) { Text(rightText) }
    }
}

@Composable
fun RealtimeQuestionInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("输入问题") }
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TestSectionCardPreview() {
    TestSectionCard(
        title = "Realtime Chat2",
        description = "测试实时语音聊天模块"
    ) {
        TestStateLine("状态: ", "已连接")
        Spacer(modifier = Modifier.height(8.dp))
        TwoActionButtons(
            leftText = "初始化",
            rightText = "开始录音",
            onLeftClick = {},
            onRightClick = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun RealtimeQuestionInputPreview() {
    RealtimeQuestionInput(value = "你好", onValueChange = {})
}
