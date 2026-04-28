package com.vectordemo.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.ui.view.activity.MainScreen
import com.vectordemo.utils.activity.BaseComponentActivity
import com.vectordemo.viewModel.activity.MainEffect
import com.vectordemo.viewModel.activity.MainVm
import kotlinx.coroutines.launch

class MainActivity : BaseComponentActivity() {
    private val vm: MainVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeEffects()
        setContent {
            VectorDemoTheme {
                val state by vm.uiState.collectAsState()
                MainScreen(
                    state = state,
                    processIntent = { vm.processIntent(it) }
                )
            }
        }
    }

    private fun observeEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        is MainEffect.NavigateToHello -> {
                            startActivity(Intent(this@MainActivity, HelloActivity::class.java))
                        }
                        MainEffect.NavigateToOssDemo -> {
                            startActivity(Intent(this@MainActivity, OssDemoActivity::class.java))
                        }
                        MainEffect.NavigateToChatList -> {
                            startActivity(Intent(this@MainActivity, ChatListActivity::class.java))
                        }
                        MainEffect.NavigateToVoiceAgent -> {
                            startActivity(Intent(this@MainActivity, VoiceAgentActivity::class.java))
                        }
                        MainEffect.NavigateToLivePush -> {
                            startActivity(Intent(this@MainActivity, LivePushDemoActivity::class.java))
                        }
                        MainEffect.NavigateToLivePull -> {
                            startActivity(Intent(this@MainActivity, LivePullDemoActivity::class.java))
                        }
                        MainEffect.NavigateToLogin -> {
                            startActivity(Intent(this@MainActivity, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            finish()
                        }
                    }
                }
            }
        }
    }
}
