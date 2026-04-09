package com.vectordemo.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.ui.view.activity.ComposeLoginScreen
import com.vectordemo.utils.activity.BaseComponentActivity
import com.vectordemo.viewModel.activity.LoginEffect
import com.vectordemo.viewModel.activity.LoginIntent
import com.vectordemo.viewModel.activity.LoginVm
import kotlinx.coroutines.launch

class LoginActivity : BaseComponentActivity() {
    private val vm: LoginVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeEffect()
        setContent {
            VectorDemoTheme {
                val state by vm.uiState.collectAsState()
                val dataState by vm.dataState.collectAsState()
                ComposeLoginScreen(
                    state = state,
                    savedAccounts = dataState.savedUserSessions,
                    onAccountChange = { vm.processIntent(LoginIntent.UpdateAccount(it)) },
                    onPasswordChange = { vm.processIntent(LoginIntent.UpdatePassword(it)) },
                    onSelectSavedAccount = { vm.processIntent(LoginIntent.SelectSavedAccount(it)) },
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
                        LoginEffect.NavigateToMain -> {
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            finish()
                        }

                        LoginEffect.NavigateToRegister -> startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
                        is LoginEffect.ShowToast -> Toast.makeText(this@LoginActivity, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
