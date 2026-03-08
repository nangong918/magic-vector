package com.magicvector.activity

import android.content.Intent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.core.baseutil.fragmentActivity.ActivityLaunchUtils
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.activity.ComposeLoginScreen
import com.magicvector.viewModel.activity.LoginEffect
import com.magicvector.viewModel.activity.LoginIntent
import com.magicvector.viewModel.activity.ComposeLoginVm
import kotlinx.coroutines.launch

class ComposeLoginActivity : ComponentActivity() {
    private val vm: ComposeLoginVm by viewModels()

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeEffect()

        setContent {
            MagicVectorTheme {
                val state by vm.uiState.collectAsState()
                ComposeLoginScreen(
                    state = state,
                    onAccountChange = { vm.processIntent(LoginIntent.UpdateAccount(it)) },
                    onPasswordChange = { vm.processIntent(LoginIntent.UpdatePassword(it)) },
                    onSubmit = { vm.processIntent(LoginIntent.SubmitLogin) },
                    onGoRegister = { vm.processIntent(LoginIntent.NavigateToRegister) }
                )
            }
        }
    }

    private fun observeEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        LoginEffect.NavigateToMain -> navigateToMain()
                        LoginEffect.NavigateToRegister -> navigateToRegister()
                        is LoginEffect.ShowToast -> {
                            Toast.makeText(this@ComposeLoginActivity, effect.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun navigateToMain() {
        ActivityLaunchUtils.launchNewTask(
            this,
            Intent(this, MainActivity::class.java),
            null
        )
        finish()
    }

    private fun navigateToRegister() {
        startActivity(Intent(this, ComposeRegisterActivity::class.java))
    }
}
