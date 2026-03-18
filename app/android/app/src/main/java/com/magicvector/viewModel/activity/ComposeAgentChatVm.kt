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
import com.data.domain.Do.ChatMessageDo
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.ao.chat.ChatItemAo
import com.data.domain.constant.VadChatState
import com.data.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.data.domain.fragmentActivity.aao.ChatAAo
import com.data.domain.fragmentActivity.intentAo.ChatIntentAo
import com.data.domain.vo.test.RealtimeChatState
import com.magicvector.MainApplication
import com.magicvector.callback.OnReceiveAgentTextCallback
import com.magicvector.callback.OnVadChatStateChange
import com.magicvector.manager.RealtimeChatController
import com.magicvector.manager.network.NetworkState
import com.magicvector.service.ChatService
import com.magicvector.ui.view.chat.MessageItem
import com.view.appview.R
import com.view.appview.recycler.RecyclerViewWhereNeedUpdate
import com.view.appview.recycler.UpdateRecyclerViewItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

class ComposeAgentChatVm : ViewModel() {

    companion object {
        val TAG: String = ComposeAgentChatVm::class.java.name
        val application = MainApplication.getApp()
    }

    /** MVI: UI 渲染状态。 */
    private val _uiState = MutableStateFlow(AgentChatUiState())
    val uiState: StateFlow<AgentChatUiState> = _uiState.asStateFlow()

    /** MVI: 一次性副作用。 */
    private val _effect = Channel<AgentChatEffect>(Channel.BUFFERED)
    val effect: Flow<AgentChatEffect> = _effect.receiveAsFlow()

    private val chatAAo = ChatAAo()
    private var messageAo: MessageContactItemAo? = null
    private val isCalling = AtomicBoolean(false)

    private var chatServiceBinder: ChatService.ChatServiceBinder? = null
    val chatServiceBoundLd = MutableLiveData(false)

    var realtimeChatController: RealtimeChatController? = null
        private set

    private var observedRealtimeController: RealtimeChatController? = null
    private var onBoundChatService: Runnable? = null
    private var lastObservedNetworkState: NetworkState = MainApplication.getNetworkManager().state.value

    private val realtimeStateObserver = Observer<RealtimeChatState> { state ->
        val enableSend = when (state) {
            is RealtimeChatState.InitializedConnected -> true
            is RealtimeChatState.RecordingAndSending -> true
            else -> false
        }
        _uiState.update {
            it.copy(
                isEnableSend = enableSend,
                orbPhase = mapOrbPhase(vadState = it.vadChatState, realtimeState = state)
            )
        }
    }

    private val onReceiveAgentTextCallback = object : OnReceiveAgentTextCallback {
        override fun onText(text: String) {
            _uiState.update { it.copy(agentText = text) }
            syncConversationMessagesToUi()
        }
    }

    private val onVadChatStateChange = object : OnVadChatStateChange {
        override fun onChange(state: VadChatState) {
            val actual = if (_uiState.value.isMicClosed &&
                (state is VadChatState.Silent || state is VadChatState.Speaking)
            ) {
                VadChatState.Muted
            } else {
                state
            }
            _uiState.update {
                it.copy(
                    vadChatState = actual,
                    orbPhase = mapOrbPhase(actual, realtimeChatController?.realtimeChatState?.value),
                    orbExpanded = (actual is VadChatState.Speaking || actual is VadChatState.Replying)
                )
            }
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
            _uiState.update {
                it.copy(
                    isChatServiceBound = false,
                    orbPhase = AgentVoiceOrbPhase.DISCONNECTED
                )
            }
            chatServiceBinder = null
            realtimeChatController = null
        }
    }

    init {
        observeNetworkState()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun processIntent(intent: AgentChatIntent) {
        when (intent) {
            is AgentChatIntent.Initialize -> initialize(intent.intent, intent.activity)
            AgentChatIntent.Resume -> realtimeChatController?.setCurrentVADStateChange(onVadChatStateChange)
            is AgentChatIntent.SendTextMessage -> sendTextMessage(intent.message)
            is AgentChatIntent.StartSendVoice -> startSendVoice(intent.scope)
            AgentChatIntent.StopSendVoice -> stopSendVoice()
            AgentChatIntent.RequestCall -> sendEffect(AgentChatEffect.RequestRecordPermission)
            is AgentChatIntent.CallPermissionGranted -> openVoiceMode(intent.context)
            AgentChatIntent.CallPermissionDenied -> {
                sendEffect(AgentChatEffect.ShowToastRes(R.string.permission_denied))
            }
            AgentChatIntent.ToggleMic -> toggleMicState()
            AgentChatIntent.EndVoiceMode -> endVoiceMode()
            is AgentChatIntent.PageChanged -> _uiState.update { it.copy(currentPage = intent.page) }
            AgentChatIntent.SwitchToEmojiPage -> _uiState.update { it.copy(currentPage = 0) }
        }
    }

    private fun initialize(intent: Intent, activity: FragmentActivity) {
        val intentAo = try {
            intent.getSerializableExtra(ChatIntentAo::class.simpleName) as ChatIntentAo
        } catch (e: Exception) {
            Log.e(TAG, "ComposeAgentChatActivity::intentAo转换失败", e)
            sendEffect(AgentChatEffect.Finish)
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
            sendEffect(AgentChatEffect.ShowToast("Agent信息为空，初始化失败"))
            sendEffect(AgentChatEffect.Finish)
            return
        }

        try {
            _uiState.update { it.copy(isLoading = true) }
            realtimeChatController?.initResource(
                chatActivity = activity,
                ao = ao,
                chatAAo = chatAAo,
                initNetworkRunnable = {
                    viewModelScope.launch {
                        syncConversationHistory(showLoading = true)
                    }
                } ,
                whereNeedUpdate = object : RecyclerViewWhereNeedUpdate {
                    override fun whereNeedUpdate(updateInfos: List<UpdateRecyclerViewItem>) {
                    }
                },
                onReceiveAgentTextCallback = onReceiveAgentTextCallback,
                onVadChatStateChange = onVadChatStateChange
            )
        } catch (e: Exception) {
            Log.e(TAG, "ComposeAgentChatActivity::initResource失败", e)
            sendEffect(AgentChatEffect.ShowToastRes(R.string.init_agent_failed))
            sendEffect(AgentChatEffect.Finish)
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun observeNetworkState() {
        viewModelScope.launch {
            MainApplication.getNetworkManager().state.collect { networkState ->
                val previous = lastObservedNetworkState
                lastObservedNetworkState = networkState
                if (messageAo?.contactId.isNullOrBlank()) {
                    return@collect
                }
                if (!previous.isNetworkOnline && networkState.isNetworkOnline) {
                    syncConversationHistory(showLoading = false)
                } else if (previous.isNetworkOnline && !networkState.isNetworkOnline) {
                    loadConversationFromRoom()
                }
            }
        }
    }

    private suspend fun syncConversationHistory(showLoading: Boolean) {
        val ao = messageAo ?: return
        val agentId = ao.contactId ?: return
        if (showLoading) {
            _uiState.update { it.copy(isLoading = true) }
        }
        try {
            val latestNetworkState = MainApplication.getNetworkManager().refreshNetworkState()
            if (!latestNetworkState.isNetworkOnline) {
                loadConversationFromRoom()
                return
            }
            val chatMessages = MainApplication.getRemoteApiSource().getLastChat(agentId).chatMessages.orEmpty()
            MainApplication.getChatCacheManager().upsertRemoteMessages(chatMessages)
            val controller = MainApplication.getChatMapManager().getChatManager(agentId)
            controller.clear()
            controller.setResponsesToViews(chatMessages)
            syncConversationMessagesToUi()
        } catch (e: Exception) {
            Log.e(TAG, "syncConversationHistory: remote failed", e)
            loadConversationFromRoom()
            sendEffect(AgentChatEffect.ShowToast("聊天记录同步失败，已回退到本地缓存"))
        } finally {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private suspend fun loadConversationFromRoom() {
        val agentId = messageAo?.contactId ?: return
        val parsedAgentId = agentId.toLongOrNull() ?: return
        val cachedMessages = MainApplication.getChatCacheManager().queryLastMessages(parsedAgentId, 100)
        val controller = MainApplication.getChatMapManager().getChatManager(agentId)
        controller.clear()
        controller.setResponsesToViews(
            cachedMessages.map { entity ->
                ChatMessageDo().apply {
                    id = entity.id.toString()
                    this.agentId = entity.agentId.toString()
                    userId = entity.userId.toString()
                    content = entity.content
                    chatTime = entity.chatTime
                    chatTimestamp = entity.chatTimestamp
                    role = entity.role
                }
            }
        )
        syncConversationMessagesToUi()
    }

    private fun syncConversationMessagesToUi() {
        val agentId = messageAo?.contactId ?: return
        val items = MainApplication.getChatMapManager()
            .getChatManager(agentId)
            .getViewChatMessageList()
            .sortedBy { it.timestamp }
            .map { it.toMessageItem() }
        sendEffect(AgentChatEffect.SyncTextMessages(items))
    }

    private fun ChatItemAo.toMessageItem(): MessageItem {
        return if (vo.viewType == 0) {
            MessageItem.Received(
                id = messageId.orEmpty(),
                messageText = vo.content,
                chatTime = vo.time.orEmpty()
            )
        } else {
            MessageItem.Sent(
                id = messageId.orEmpty(),
                messageText = vo.content,
                timeText = vo.time.orEmpty()
            )
        }
    }

    private fun sendTextMessage(message: String) {
        val isAllWhitespaceOrSpecialChars = message.all { it.isWhitespace() || !it.isLetterOrDigit() }
        if (message.isBlank() || isAllWhitespaceOrSpecialChars) {
            sendEffect(AgentChatEffect.ShowToastRes(com.view.appview.R.string.please_input_legal_content))
            return
        }

        val dataMap = mapOf(
            RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.USER_TEXT_MESSAGE.type,
            RealtimeRequestDataTypeEnum.DATA to message
        )
        realtimeChatController?.realtimeChatWsClient?.sendMessage(dataMap, true)
            ?: sendEffect(AgentChatEffect.ShowToast("连接未建立，发送失败"))
    }

    private fun startSendVoice(scope: CoroutineScope) {
        val weakScope = WeakReference(scope)
        realtimeChatController?.startRecordRealtimeChatAudio(weakScope)
    }

    private fun stopSendVoice() {
        realtimeChatController?.stopAndSendRealtimeChatAudio()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun openVoiceMode(context: Context) {
        realtimeChatController?.initVadCall(WeakReference(context))
        isCalling.set(true)
        _uiState.update {
            it.copy(
                isMicClosed = false,
                vadChatState = VadChatState.Silent,
                orbPhase = AgentVoiceOrbPhase.READY
            )
        }
    }

    private fun toggleMicState() {
        val current = _uiState.value
        if (current.isMicClosed) {
            realtimeChatController?.startVadCall()
            _uiState.update { it.copy(isMicClosed = false, vadChatState = VadChatState.Silent) }
        } else {
            realtimeChatController?.stopVadCall()
            _uiState.update { it.copy(isMicClosed = true, vadChatState = VadChatState.Muted) }
        }
    }

    private fun endVoiceMode() {
        realtimeChatController?.destroyVadCall()
        isCalling.set(false)
        _uiState.update {
            it.copy(
                isMicClosed = true,
                vadChatState = VadChatState.Muted,
                agentText = "",
                orbExpanded = false,
                orbPhase = AgentVoiceOrbPhase.DISCONNECTED
            )
        }
    }

    private fun mapOrbPhase(
        vadState: VadChatState,
        realtimeState: RealtimeChatState?
    ): AgentVoiceOrbPhase {
        if (realtimeState is RealtimeChatState.Error || vadState is VadChatState.Error) {
            return AgentVoiceOrbPhase.ERROR
        }
        if (realtimeState !is RealtimeChatState.InitializedConnected &&
            realtimeState !is RealtimeChatState.RecordingAndSending &&
            realtimeState !is RealtimeChatState.Receiving
        ) {
            return AgentVoiceOrbPhase.DISCONNECTED
        }
        return when (vadState) {
            is VadChatState.Speaking -> AgentVoiceOrbPhase.USER_SPEAKING
            is VadChatState.Replying -> AgentVoiceOrbPhase.AGENT_REPLYING
            else -> AgentVoiceOrbPhase.READY
        }
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

    private fun sendEffect(effect: AgentChatEffect) {
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
data class AgentChatUiState(
    /** 顶部标题 */
    val title: String = "",
    /** 顶部头像 */
    val avatarUrl: String? = null,
    /** 页面级加载态。 */
    val isLoading: Boolean = false,
    /** 发送区可用态 */
    val isEnableSend: Boolean = false,
    /** ChatService 绑定状态 */
    val isChatServiceBound: Boolean = false,
    /** 当前页面索引（0: Emoji，1: Text） */
    val currentPage: Int = 0,
    /** 麦克风是否关闭 */
    val isMicClosed: Boolean = true,
    /** 当前 VAD 状态 */
    val vadChatState: VadChatState = VadChatState.Muted,
    /** Agent 回复文本（流式片段） */
    val agentText: String = "",
    /** 底部状态球阶段 */
    val orbPhase: AgentVoiceOrbPhase = AgentVoiceOrbPhase.DISCONNECTED,
    /** 底部状态球弹性缩放开关 */
    val orbExpanded: Boolean = false,
)

enum class AgentVoiceOrbPhase {
    DISCONNECTED,
    ERROR,
    READY,
    USER_SPEAKING,
    AGENT_REPLYING
}

sealed class AgentChatIntent {
    /** 初始化页面与会话资源。 */
    data class Initialize(val intent: Intent, val activity: FragmentActivity) : AgentChatIntent()

    /** 生命周期恢复。 */
    data object Resume : AgentChatIntent()

    /** 文本发送。 */
    data class SendTextMessage(val message: String) : AgentChatIntent()

    /** 开始录音发送。 */
    data class StartSendVoice(val scope: CoroutineScope) : AgentChatIntent()

    /** 结束录音发送。 */
    data object StopSendVoice : AgentChatIntent()

    /** 请求进入语音唤醒模式。 */
    data object RequestCall : AgentChatIntent()

    /** 录音权限通过。 */
    data class CallPermissionGranted(val context: Context) : AgentChatIntent()

    /** 录音权限拒绝。 */
    data object CallPermissionDenied : AgentChatIntent()

    /** 切换 Mic 开关。 */
    data object ToggleMic : AgentChatIntent()

    /** 结束语音模式。 */
    data object EndVoiceMode : AgentChatIntent()

    /** Pager 页面变化。 */
    data class PageChanged(val page: Int) : AgentChatIntent()

    /** 切换到 Emoji 页面。 */
    data object SwitchToEmojiPage : AgentChatIntent()
}

sealed class AgentChatEffect {
    data object Finish : AgentChatEffect()
    data object RequestRecordPermission : AgentChatEffect()
    data class ShowToast(val message: String) : AgentChatEffect()
    data class ShowToastRes(val messageRes: Int) : AgentChatEffect()
    data class SyncTextMessages(val messages: List<MessageItem>) : AgentChatEffect()
}
