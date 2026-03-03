package com.magicvector.fragment

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.magicvector.viewModel.fragment.MineVm
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.activity.test.ComposeTestActivity
import com.magicvector.viewModel.fragment.MineIntent
import com.view.appview.R

@Composable
fun MineScreen(
    modifier: Modifier = Modifier,
    viewModel: MineVm = MineVm()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    MineContent(
        modifier = modifier,
        onTestClick = {
            // 发送 Intent 到 ViewModel
            viewModel.processIntent(MineIntent.TestButtonClick)
            // 实际跳转
            context.startActivity(Intent(context, ComposeTestActivity::class.java))
        }
    )
}


@Composable
fun MineContent(
    modifier: Modifier = Modifier,
    onTestClick: () -> Unit
) {
    Column(modifier = modifier
        .fillMaxSize()
        .padding(20.dp)) {
        Button(
            onClick = onTestClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            Text(
                text = stringResource(id = R.string.test),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}


// 预览函数：直接预览 MineContent，传入模拟的点击事件
@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun MineContentPreview() {
    // 模拟点击事件（预览中可写空实现，仅展示UI）
    MineContent(
        modifier = Modifier,
        onTestClick = {}
    )
}



















