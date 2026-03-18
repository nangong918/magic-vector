package com.magicvector.activity

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.ui.ToastUtils
import com.data.domain.constant.VadChatState
import com.magicvector.MainApplication
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.activity.ComposeAgentChatScreen
import com.magicvector.utils.permissions.ComposePermissionUtils
import com.magicvector.viewModel.activity.AgentChatEffect
import com.magicvector.viewModel.activity.AgentChatIntent
import com.magicvector.viewModel.activity.AgentVoiceOrbPhase
import com.magicvector.viewModel.activity.ComposeAgentChatVm
import com.magicvector.viewModel.fragment.AgentEmojiFragmentIntent
import com.magicvector.viewModel.fragment.AgentEmojiFragmentVm
import com.magicvector.viewModel.fragment.AgentTextChatFragmentIntent
import com.magicvector.viewModel.fragment.AgentTextChatFragmentVm
import kotlinx.coroutines.launch

class ComposeAgentChatActivity : FragmentActivity() {

    private val vm: ComposeAgentChatVm by viewModels()
    private val emojiVm: AgentEmojiFragmentVm by viewModels()
    private val textVm: AgentTextChatFragmentVm by viewModels()
    private val recordPermissionUtils = ComposePermissionUtils()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        recordPermissionUtils.registerPermissionLauncher(
            activity = this,
            mustPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)
        )
        enableEdgeToEdge()
        observeEffects()
        setContent {
            MagicVectorTheme {
                val uiState by vm.uiState.collectAsState()
                val phaseText = mapPhaseText(uiState.orbPhase, uiState.vadChatState)
                LaunchedEffect(uiState.orbPhase, uiState.orbExpanded, phaseText) {
                    emojiVm.processIntent(
                        AgentEmojiFragmentIntent.SyncPhase(
                            phase = uiState.orbPhase,
                            expanded = uiState.orbExpanded,
                            statusText = phaseText
                        )
                    )
                }
                LaunchedEffect(uiState.isEnableSend) {
                    textVm.processIntent(
                        AgentTextChatFragmentIntent.SyncSendEnable(uiState.isEnableSend)
                    )
                }
                ComposeAgentChatScreen(
                    uiState = uiState,
                    emojiVm = emojiVm,
                    textVm = textVm,
                    onBackClick = { finish() },
                    onPageChanged = { page ->
                        vm.processIntent(AgentChatIntent.PageChanged(page))
                    },
                    onToggleMic = { vm.processIntent(AgentChatIntent.ToggleMic) },
                    onRequestWakeUp = { vm.processIntent(AgentChatIntent.RequestCall) },
                    onEndVoiceMode = { vm.processIntent(AgentChatIntent.EndVoiceMode) },
                    onSendTextToAgent = { message ->
                        vm.processIntent(AgentChatIntent.SendTextMessage(message))
                    },
                    onSwitchToEmojiPage = {
                        vm.processIntent(AgentChatIntent.SwitchToEmojiPage)
                    },
                    onAudioTouch = { isStart ->
                        if (isStart) {
                            vm.processIntent(AgentChatIntent.StartSendVoice(lifecycleScope))
                        } else {
                            vm.processIntent(AgentChatIntent.StopSendVoice)
                        }
                    }
                )
            }
        }
        vm.processIntent(AgentChatIntent.Initialize(intent, this))
    }

    private fun observeEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        AgentChatEffect.Finish -> finish()
                        AgentChatEffect.RequestRecordPermission -> {
                            recordPermissionUtils.requestPermissions(this@ComposeAgentChatActivity, object : GainPermissionCallback {
                                override fun allGranted() {
                                    vm.processIntent(AgentChatIntent.CallPermissionGranted(this@ComposeAgentChatActivity))
                                }

                                override fun notGranted(notGrantedPermissions: Array<String?>?) {
                                    vm.processIntent(AgentChatIntent.CallPermissionDenied)
                                }

                                override fun always() {
                                }
                            })
                        }

                        is AgentChatEffect.ShowToast -> {
                            ToastUtils.showToastActivity(this@ComposeAgentChatActivity, effect.message)
                        }

                        is AgentChatEffect.ShowToastRes -> {
                            ToastUtils.showToastActivity(this@ComposeAgentChatActivity, getString(effect.messageRes))
                        }

                        is AgentChatEffect.SyncTextMessages -> {
                            textVm.processIntent(AgentTextChatFragmentIntent.SyncMessages(effect.messages))
                        }
                    }
                }
            }
        }
    }

    private fun mapPhaseText(phase: AgentVoiceOrbPhase, vadChatState: VadChatState): String {
        val cameraText = if (MainApplication.getVisionManager().isUsingFrontCamera()) "前置摄像头" else "后置摄像头"
        val status = when (phase) {
            AgentVoiceOrbPhase.DISCONNECTED -> "未连接或已断开"
            AgentVoiceOrbPhase.ERROR -> "连接异常"
            AgentVoiceOrbPhase.READY -> {
                if (vadChatState is VadChatState.Muted) "麦克风关闭" else "就绪，等待唤醒"
            }
            AgentVoiceOrbPhase.USER_SPEAKING -> "用户正在说话"
            AgentVoiceOrbPhase.AGENT_REPLYING -> "Agent 正在回复"
        }
        return "$status · $cameraText"
    }

    override fun onResume() {
        super.onResume()
        vm.processIntent(AgentChatIntent.Resume)
    }
}

