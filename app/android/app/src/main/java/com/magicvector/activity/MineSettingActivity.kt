package com.magicvector.activity

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
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.activity.ComposeMineSettingActivityScreen
import com.magicvector.utils.activity.BaseComponentActivity
import com.magicvector.viewModel.activity.MineSettingVm
import com.magicvector.viewModel.activity.MineSettingEffect
import com.magicvector.viewModel.activity.MineSettingIntent
import kotlinx.coroutines.launch

class MineSettingActivity : BaseComponentActivity() {

    private val vm: MineSettingVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeEffect()
        setContent {
            MagicVectorTheme {
                val state by vm.uiState.collectAsState()
                ComposeMineSettingActivityScreen(
                    state = state,
                    onOldPasswordChange = { vm.processIntent(MineSettingIntent.UpdateOldPassword(it)) },
                    onNewPasswordChange = { vm.processIntent(MineSettingIntent.UpdateNewPassword(it)) },
                    onSubmitPassword = { vm.processIntent(MineSettingIntent.SubmitPasswordUpdate) },
                    onLogout = { vm.processIntent(MineSettingIntent.Logout) }
                )
            }
        }
        vm.processIntent(MineSettingIntent.Initialize)
    }

    private fun observeEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        MineSettingEffect.NavigateToLogin -> {
                            startActivity(Intent(this@MineSettingActivity, LoginActivity::class.java))
                            finish()
                        }
                        is MineSettingEffect.ShowToast -> {
                            Toast.makeText(this@MineSettingActivity, effect.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
