package com.magicvector.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.core.baseutil.fragmentActivity.ActivityLaunchUtils
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.theme.White
import com.magicvector.viewModel.activity.StartEffect
import com.magicvector.viewModel.activity.StartIntent
import com.magicvector.viewModel.activity.StartVm
import kotlinx.coroutines.launch

class StartActivity : ComponentActivity() {

    private val vm: StartVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // vm初始化
        vm.processIntent(StartIntent.Initialize)

        // 观察 Effect
        observeEffects()

        // initView
        setContent {
            MagicVectorTheme {
                StartScreen()
            }
        }
    }


    // 观察 Effect
    private fun observeEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        StartEffect.NavigateToMain -> {
                            navigateToMain()
                        }
                        StartEffect.NavigateToLogin -> {
                            navigateToLogin()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this@StartActivity, MainActivity::class.java)

        ActivityLaunchUtils.launchNewTask(
            this@StartActivity,
            intent,
            null
        )

        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this@StartActivity, ComposeLoginActivity::class.java)
        ActivityLaunchUtils.launchNewTask(
            this@StartActivity,
            intent,
            null
        )
        finish()
    }



    override fun onResume() {
        super.onResume()
        // 每次恢复时设置全屏
        setupFullScreen()
    }

    private fun setupFullScreen() {
        // 隐藏标题导航栏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // 隐藏状态栏和导航栏
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }


}


@Composable
private fun StartScreen() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(White), // 设置背景为紫色
            contentAlignment = Alignment.Center // 设置内容居中
        ) {
            Logo() // 使用 Logo 组件展示 logo
        }
    }
}

@Composable
private fun Logo(modifier: Modifier = Modifier) {
    // 这里假设你有一个 logo 的 drawable 资源
    Image(
        painter = painterResource(id = com.view.appview.R.mipmap.vector), // 替换为你的 logo 资源 ID
        contentDescription = "App Logo",
        modifier = modifier.size(220.dp)
            .clip(RoundedCornerShape(20.dp))
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun GreetingPreview() {
    MagicVectorTheme {
        StartScreen()
    }
}