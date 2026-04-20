package com.vectordemo.activity

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vectordemo.config.ModuleKeyConfigStore
import com.vectordemo.service.ai.AliChatService
import com.vectordemo.service.ai.PromptAssetLoader
import com.vectordemo.service.ai.XfYunChatService
import com.vectordemo.service.voice.AliSttService
import com.vectordemo.service.voice.OfflineIvwService
import com.vectordemo.service.voice.VadService
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.ui.view.voice.VoiceAgentScreen
import com.vectordemo.utils.activity.BaseComponentActivity
import com.vectordemo.utils.permissions.ComposePermissionUtils
import com.vectordemo.utils.permissions.GainPermissionCallback
import com.vectordemo.viewModel.voice.VoiceAgentEffect
import com.vectordemo.viewModel.voice.VoiceAgentVm
import kotlinx.coroutines.launch

class VoiceAgentActivity : BaseComponentActivity() {
    private val permissionUtils = ComposePermissionUtils()
    private val appCtx by lazy { applicationContext }
    private val vm: VoiceAgentVm by viewModels {
        VoiceAgentVm.factory(
            ivwService = OfflineIvwService(appCtx),
            vadService = VadService(appCtx),
            sttService = AliSttService(appCtx),
            aliChatService = AliChatService { ModuleKeyConfigStore.load(appCtx).aliLlm },
            xfyunChatService = XfYunChatService { ModuleKeyConfigStore.load(appCtx).xfLlm }
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeEffects()
        registerPermissionLauncher()
        requestRecordPermissionAndInit()


        setContent {
            VectorDemoTheme {
                val state by vm.uiState.collectAsState()
                VoiceAgentScreen(
                    state = state,
                    serviceStatusText = vm.buildServiceStatusText(state),
                    onBack = { finish() },
                    onSelectLlm = { vm.setLlmProvider(it) }
                )
            }
        }
    }

    private fun registerPermissionLauncher() {
        permissionUtils.registerPermissionLauncher(
            activity = this,
            mustPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        )
    }

    private fun requestRecordPermissionAndInit() {
        permissionUtils.requestPermissions(this, object : GainPermissionCallback {
            override fun allGranted() {
                vm.updateRecordPermissionGranted(true)
                val prompt = runCatching { PromptAssetLoader.loadSystemPrompt(appCtx) }.getOrElse { "" }
                vm.initialize(prompt)
            }

            override fun notGranted(notGrantedPermissions: Array<String?>?) {
                vm.updateRecordPermissionGranted(false)
                finish()
            }

            override fun always() = Unit
        })
    }

    private fun observeEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        VoiceAgentEffect.FinishActivity -> finish()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        vm.close()
        super.onDestroy()
    }
}

