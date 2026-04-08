package com.magicvector.activity

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.magicvector.utils.fragmentActivity.ActivityLaunchUtils
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.activity.ComposeLoginScreen
import com.magicvector.utils.activity.BaseComponentActivity
import com.magicvector.viewModel.activity.LoginEffect
import com.magicvector.viewModel.activity.LoginIntent
import com.magicvector.viewModel.activity.LoginVm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginActivity : BaseComponentActivity() {
    private val vm: LoginVm by viewModels()

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeEffect()

        setContent {
            MagicVectorTheme {
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
        // 在ui线程执行ui操作，避免出现io线程绘制ui的错误
        lifecycleScope.launch(Dispatchers.Main) {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        LoginEffect.NavigateToMain -> navigateToMain()
                        LoginEffect.NavigateToRegister -> navigateToRegister()
                        is LoginEffect.ShowToast -> {
                            Toast.makeText(this@LoginActivity, effect.message, Toast.LENGTH_SHORT).show()
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
        startActivity(Intent(this, RegisterActivity::class.java))
    }
}
