package com.magicvector.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.magicvector.MainApplication
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.theme.White
import com.magicvector.utils.activity.BaseComponentActivity
import com.magicvector.viewModel.activity.StartEffect
import com.magicvector.viewModel.activity.StartIntent
import com.magicvector.viewModel.activity.StartVm
import kotlinx.coroutines.launch

class StartActivity : BaseComponentActivity() {
    companion object {
        private const val TAG = "StartActivity"
    }

    private val vm: StartVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // vm初始化
        vm.processIntent(StartIntent.Initialize)
        logLocalUserSessions()

        // 观察 Effect
        observeEffects()

        // initView
        setContent {
            MagicVectorTheme {
                StartScreen()
            }
        }
    }

    private fun logLocalUserSessions() {
        lifecycleScope.launch {
            val sessions = MainApplication.getUserManager().getAllUsers()
            if (sessions.isEmpty()) {
                Log.d(TAG, "[debug] local user_session is empty.")
                return@launch
            }
            sessions.forEachIndexed { index, session ->
                val maskedToken = if (session.accessToken.length <= 8) {
                    session.accessToken
                } else {
                    session.accessToken.take(4) + "***" + session.accessToken.takeLast(4)
                }
                val passwordStatus = if (session.password.isBlank()) "empty" else "saved"
                Log.d(
                    TAG,
                    "[debug] user_session[$index]; maskedToken = [$maskedToken]; " +
                            "passwordStatus: [$passwordStatus] session=${session.toJsonString()}"
                )
            }
        }
    }


    // 观察 Effect
    private fun observeEffects() {
        lifecycleScope.launch {
            // STARTED协程启动，并在STOPPED协程自动取消；否者协程不会自动取消
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