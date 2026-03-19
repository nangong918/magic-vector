package com.magicvector.viewModel.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.Stable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magicvector.domain.model.message.MessageContactItemModel
import com.data.domain.constant.VadChatState
import com.data.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.data.domain.fragmentActivity.aao.ChatAAo
import com.data.domain.fragmentActivity.intentAo.ChatIntentAo
import com.data.domain.vo.test.RealtimeChatState
import com.magicvector.MainApplication
import com.magicvector.callback.OnReceiveAgentTextCallback
import com.magicvector.callback.OnVadChatStateChange
import com.magicvector.manager.RealtimeChatController
import com.magicvector.service.ChatService
import com.view.appview.R
import com.view.appview.recycler.RecyclerViewWhereNeedUpdate
import com.view.appview.recycler.UpdateRecyclerViewItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class ComposeChatVm : ViewModel() {

    companion object {
        val TAG: String = ComposeChatVm::class.java.name
        val application = MainApplication.getApp()
    }

    // MVI State: Compose UI 渲染状态
    private val _uiState = MutableStateFlow(ChatState())
    val uiState: StateFlow<ChatState> = _uiState.asStateFlow()

    // MVI Effect: 一次性事件（权限/跳转/Toast）
    private val _effect = Channel<ChatEffect>(Channel.BUFFERED)
    val effect: Flow<ChatEffect> = _effect.receiveAsFlow()

    private val chatAAo = ChatAAo()
    private var messageAo: MessageContactItemModel? = null
    private val isCalling = AtomicBoolean(false)

    private var chatServiceBinder: ChatService.ChatServiceBinder? = null
    val chatServiceBoundLd = MutableLiveData(false)

    var realtimeChatController: RealtimeChatController? = null
        private set

    private var observedRealtimeController: RealtimeChatController? = null
    private var onBoundChatService: Runnable? = null

    private val realtimeStateObserver = Observer<RealtimeChatState> { state ->
        val enableSend = when (state) {
            is RealtimeChatState.InitializedConnected -> true
            is RealtimeChatState.RecordingAndSending -> true
            else -> false
        }
        _uiState.update { it.copy(isEnableSend = enableSend) }
    }

    private val onReceiveAgentTextCallback = object : OnReceiveAgentTextCallback {
        override fun onText(text: String) {
            _uiState.update { it.copy(callMessage = text) }
        }
    }

    private val onVadChatStateChange = object : OnVadChatStateChange {
        override fun onChange(state: VadChatState) {
            val newState = if (_uiState.value.isMicClosed &&
                (state is VadChatState.Silent || state is VadChatState.Speaking)
            ) {
                VadChatState.Muted
            } else {
                state
            }
            _uiState.update { it.copy(callVadState = newState) }
        }
    }

    private val chatServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? ChatService.ChatServiceBinder ?: return
            chatServiceBinder = binder
            chatServiceBoundLd.postValue(true)
            realtimeChatController = binder.getChatMessageHandler()
            _uiState.update { it.copy(isChatServiceBound = true) }

            observedRealtimeController?.realtimeChatState?.removeObserver(realtimeStateObserver)
            realtimeChatController?.realtimeChatState?.observeForever(realtimeStateObserver)
            observedRealtimeController = realtimeChatController
            realtimeChatController?.setCurrentVADStateChange(onVadChatStateChange)
            realtimeChatController?.initIsChatCalling(isCalling)

            onBoundChatService?.run()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            chatServiceBoundLd.postValue(false)
            _uiState.update { it.copy(isChatServiceBound = false) }
            chatServiceBinder = null
            realtimeChatController = null
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun processIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.Initialize -> initialize(intent.intent, intent.activity)
            ChatIntent.Resume -> realtimeChatController?.setCurrentVADStateChange(onVadChatStateChange)
            is ChatIntent.SendTextMessage -> sendTextMessage(intent.message)
            is ChatIntent.StartSendVoice -> startSendVoice(intent.scope)
            ChatIntent.StopSendVoice -> stopSendVoice()
            ChatIntent.RequestCall -> sendEffect(ChatEffect.RequestCallPermission)
            is ChatIntent.CallPermissionGranted -> openCallDialog(intent.context)
            ChatIntent.CallPermissionDenied -> sendEffect(ChatEffect.ShowToastRes(R.string.permission_denied))
            ChatIntent.ToggleCallMute -> toggleMicState()
            ChatIntent.EndCall -> endCall()
            ChatIntent.RequestVideoCall -> sendEffect(ChatEffect.RequestVideoPermission)
            ChatIntent.VideoPermissionDenied -> sendEffect(ChatEffect.ShowToastRes(R.string.permission_denied))
            ChatIntent.VideoPermissionGranted -> gotoVideoCall()
        }
    }

    private fun initialize(intent: Intent, activity: FragmentActivity) {
        val intentAo = try {
            intent.getSerializableExtra(ChatIntentAo::class.simpleName) as ChatIntentAo
        } catch (e: Exception) {
            Log.e(TAG, "ComposeChatActivity::intentAo转换失败", e)
            sendEffect(ChatEffect.Finish)
            return
        }

        messageAo = intentAo.ao
        _uiState.update {
            it.copy(
                title = messageAo?.vo?.name ?: "",
                avatarUrl = messageAo?.vo?.avatarUrl,
            )
        }

        initService(
            Runnable @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO) {
                initResource(activity)
            }
        )
    }

    private fun initService(onBoundChatService: Runnable) {
        this.onBoundChatService = onBoundChatService
        val serviceIntent = Intent(application, ChatService::class.java)
        application.bindService(serviceIntent, chatServiceConnection, BIND_AUTO_CREATE)
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initResource(activity: FragmentActivity) {
        val ao = messageAo ?: run {
            sendEffect(ChatEffect.ShowToast("Agent信息为空，初始化失败"))
            sendEffect(ChatEffect.Finish)
            return
        }

        try {
            _uiState.update { it.copy(isLoading = true) }
            realtimeChatController?.initResource(
                chatActivity = activity,
                ao = ao,
                chatAAo = chatAAo,
                initNetworkRunnable = { Job() },
                whereNeedUpdate = object : RecyclerViewWhereNeedUpdate {
                    override fun whereNeedUpdate(updateInfos: List<UpdateRecyclerViewItem>) {
                    }
                },
                onReceiveAgentTextCallback = onReceiveAgentTextCallback,
                onVadChatStateChange = onVadChatStateChange
            )
        } catch (e: Exception) {
            Log.e(TAG, "ComposeChatActivity::initResource失败", e)
            sendEffect(ChatEffect.ShowToastRes(R.string.init_agent_failed))
            sendEffect(ChatEffect.Finish)
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun sendTextMessage(message: String) {
        val isAllWhitespaceOrSpecialChars = message.all { it.isWhitespace() || !it.isLetterOrDigit() }
        if (message.isBlank() || isAllWhitespaceOrSpecialChars) {
            sendEffect(ChatEffect.ShowToastRes(com.view.appview.R.string.please_input_legal_content))
            return
        }

        val dataMap = mapOf(
            RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.USER_TEXT_MESSAGE.type,
            RealtimeRequestDataTypeEnum.DATA to message
        )
        realtimeChatController?.realtimeChatWsClient?.sendMessage(dataMap, true)
            ?: sendEffect(ChatEffect.ShowToast("连接未建立，发送失败"))
    }

    private fun startSendVoice(scope: CoroutineScope) {
        val weakScope = WeakReference(scope)
        realtimeChatController?.startRecordRealtimeChatAudio(weakScope)
    }

    private fun stopSendVoice() {
        realtimeChatController?.stopAndSendRealtimeChatAudio()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun openCallDialog(context: Context) {
        realtimeChatController?.initVadCall(WeakReference(context))
        isCalling.set(true)
        _uiState.update {
            it.copy(
                isCallDialogVisible = true,
                isMicClosed = false,
                callVadState = VadChatState.Silent
            )
        }
    }

    private fun toggleMicState() {
        val current = _uiState.value
        if (current.isMicClosed) {
            realtimeChatController?.startVadCall()
            _uiState.update { it.copy(isMicClosed = false, callVadState = VadChatState.Silent) }
        } else {
            realtimeChatController?.stopVadCall()
            _uiState.update { it.copy(isMicClosed = true, callVadState = VadChatState.Muted) }
        }
    }

    private fun endCall() {
        realtimeChatController?.destroyVadCall()
        isCalling.set(false)
        _uiState.update {
            it.copy(
                isCallDialogVisible = false,
                isMicClosed = true,
                callVadState = VadChatState.Muted,
                callMessage = ""
            )
        }
    }

    private fun gotoVideoCall() {
        val ao = messageAo
        if (ao == null) {
            sendEffect(ChatEffect.ShowToastRes(com.view.appview.R.string.system_error))
            return
        }
        sendEffect(
            ChatEffect.NavigateToVideoCall(
                agentId = ao.contactId ?: "",
                agentName = ao.vo.name
            )
        )
    }

    private fun disconnectService() {
        application.let { context ->
            if (chatServiceBoundLd.value == true) {
                try {
                    context.unbindService(chatServiceConnection)
                } catch (e: Exception) {
                    Log.e(TAG, "disconnectService failed", e)
                }
            }
        }
        chatServiceBoundLd.postValue(false)
        chatServiceBinder = null
        realtimeChatController = null
    }

    private fun sendEffect(effect: ChatEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    override fun onCleared() {
        super.onCleared()
        observedRealtimeController?.realtimeChatState?.removeObserver(realtimeStateObserver)
        disconnectService()
    }
}

@Stable
data class ChatState(
    // 加载与连接状态
    val isLoading: Boolean = false,
    val isChatServiceBound: Boolean = false,
    val isEnableSend: Boolean = false,
    // 头部信息
    val title: String = "",
    val avatarUrl: String? = null,
    // 通话弹窗状态
    val isCallDialogVisible: Boolean = false,
    val isMicClosed: Boolean = true,
    val callVadState: VadChatState = VadChatState.Muted,
    val callMessage: String = "",
)

sealed class ChatIntent {
    // 初始化 / 生命周期
    data class Initialize(val intent: Intent, val activity: FragmentActivity) : ChatIntent()
    data object Resume : ChatIntent()

    // 消息与语音
    data class SendTextMessage(val message: String) : ChatIntent()
    data class StartSendVoice(val scope: CoroutineScope) : ChatIntent()
    data object StopSendVoice : ChatIntent()

    // 语音通话
    data object RequestCall : ChatIntent()
    data class CallPermissionGranted(val context: Context) : ChatIntent()
    data object CallPermissionDenied : ChatIntent()
    data object ToggleCallMute : ChatIntent()
    data object EndCall : ChatIntent()

    // 视频通话
    data object RequestVideoCall : ChatIntent()
    data object VideoPermissionGranted : ChatIntent()
    data object VideoPermissionDenied : ChatIntent()
}

sealed class ChatEffect {
    // 页面动作
    data object Finish : ChatEffect()
    // 权限请求
    data object RequestCallPermission : ChatEffect()
    data object RequestVideoPermission : ChatEffect()
    // 跳转与提示
    data class NavigateToVideoCall(val agentId: String, val agentName: String) : ChatEffect()
    data class ShowToast(val message: String) : ChatEffect()
    data class ShowToastRes(val messageRes: Int) : ChatEffect()
}














