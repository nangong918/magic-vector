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
import com.vectordemo.viewModel.activity.MainIntent
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
                    onQueryChange = { vm.processIntent(MainIntent.UpdateQuery(it)) },
                    onClickItem = { vm.processIntent(MainIntent.ClickDemo(it.route)) }
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
                    }
                }
            }
        }
    }
}
