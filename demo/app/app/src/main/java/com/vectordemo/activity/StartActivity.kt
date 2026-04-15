package com.vectordemo.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.ui.view.activity.StartScreen
import com.vectordemo.utils.activity.BaseComponentActivity
import com.vectordemo.viewModel.activity.StartEffect
import com.vectordemo.viewModel.activity.StartIntent
import com.vectordemo.viewModel.activity.StartVm
import kotlinx.coroutines.launch

class StartActivity : BaseComponentActivity() {
    private val vm: StartVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm.processIntent(StartIntent.Initialize)
        observeEffects()
        setContent {
            VectorDemoTheme {
                StartScreen()
            }
        }
    }

    private fun observeEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        StartEffect.NavigateToMain -> navigateToMain()
                        StartEffect.NavigateToLogin -> navigateToLogin()
                        is StartEffect.ShowToast -> Toast.makeText(this@StartActivity, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}