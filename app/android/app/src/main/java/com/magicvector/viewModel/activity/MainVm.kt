package com.magicvector.viewModel.activity

import android.Manifest
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magicvector.domain.dto.http.request.AgentDeleteRequest
import com.magicvector.MainApplication
import com.magicvector.manager.RealtimeChatController
import com.magicvector.manager.agent.AgentsEffect
import com.magicvector.manager.event.EventSourceType
import com.magicvector.viewModel.fragment.ControlVm
import com.magicvector.viewModel.fragment.MessageListIntent
import com.magicvector.viewModel.fragment.MessageListMviVm
import com.magicvector.viewModel.fragment.MineVm
import com.view.appview.MainSelectItemEnum
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody


class MainVm : ViewModel() {

    companion object {
        val TAG: String = MainVm::class.java.name
        private val api = MainApplication.getRemoteApiSource()
    }

    // 保留对外控制器引用，避免影响其他页面后续接入。
    var realtimeChatController: RealtimeChatController? = null
    // MainVm 统一持有各 Tab 子 VM，避免 Composable 重建时状态丢失。
    val messageListVm: MessageListMviVm = MessageListMviVm()
    val controlVm: ControlVm = ControlVm()
    val mineVm: MineVm = MineVm()

    // StateFlow (UI State) 存储 UI 状态
    private val _uiState = MutableStateFlow(MainState())
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()
    private val _dataState = MutableStateFlow(MainDataState())
    val dataState: StateFlow<MainDataState> = _dataState.asStateFlow()
    private val _effect = Channel<MainEffect>(Channel.BUFFERED)
    val effect: Flow<MainEffect> = _effect.receiveAsFlow()
    private val agentsManager = MainApplication.getAgentsManager()

    init {
        syncAgentsDataFromManager()
        observeAgentsEffect()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.Initialize -> {
                _uiState.update {
                    it.copy(
                        currentSelected = intent.selected
                    )
                }
                syncAgentsDataFromManager()
            }

            is MainIntent.SelectTab -> {
                _uiState.update {
                    it.copy(
                        currentSelected = intent.tab
                    )
                }
            }

            MainIntent.OpenCreateAgent -> {
                _uiState.update { it.copy(agentEditor = AgentEditorState(isVisible = true, mode = AgentEditorMode.CREATE)) }
            }

            MainIntent.CloseAgentEditor -> {
                _uiState.update { it.copy(agentEditor = AgentEditorState()) }
            }

            is MainIntent.OpenEditAgent -> {
                openEditAgent(intent.agentId)
            }

            is MainIntent.UpdateEditorName -> {
                _uiState.update { it.copy(agentEditor = it.agentEditor.copy(name = intent.name)) }
            }

            is MainIntent.UpdateEditorDescription -> {
                _uiState.update { it.copy(agentEditor = it.agentEditor.copy(description = intent.description)) }
            }

            MainIntent.SubmitAgentEditor -> submitAgentEditor()

            MainIntent.DeleteAgent -> deleteCurrentAgent()

            is MainIntent.ChatServiceBound -> {
                realtimeChatController = intent.handler
                realtimeChatController?.ensureUserConnection(MainApplication.getUserId())
                MainApplication.getNetworkManager().bindWsReconnectAction {
                    realtimeChatController?.reconnectUserConnectionIfNeeded()
                }
                _dataState.update { it.copy(isChatServiceBound = true) }
            }

            MainIntent.ChatServiceUnbound -> {
                MainApplication.getNetworkManager().unbindWsReconnectAction()
                realtimeChatController = null
                _dataState.update { it.copy(isChatServiceBound = false) }
            }
        }
    }

    private fun openEditAgent(agentId: String) {
        _uiState.update {
            it.copy(
                agentEditor = AgentEditorState(
                    isVisible = true,
                    mode = AgentEditorMode.EDIT,
                    agentId = agentId,
                    isLoading = true
                )
            )
        }
        viewModelScope.launch {
            try {
                val response = api.getAgentInfo(agentId)
                val vo = response.agentModel?.agentVo
                _uiState.update {
                    it.copy(
                        agentEditor = it.agentEditor.copy(
                            isLoading = false,
                            name = vo?.name ?: "",
                            description = vo?.description ?: ""
                        )
                    )
                }
            } catch (_: Throwable) {
                _uiState.update { it.copy(agentEditor = AgentEditorState()) }
                sendEffect(MainEffect.ShowToast("获取 Agent 详情失败"))
            }
        }
    }

    private fun submitAgentEditor() {
        val editor = _uiState.value.agentEditor
        if (editor.name.isBlank() || editor.description.isBlank()) {
            sendEffect(MainEffect.ShowToast("名称和描述不能为空"))
            return
        }
        _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = true)) }
        val plain = "text/plain".toMediaTypeOrNull()
        val userId = MainApplication.getUserId().toRequestBody(plain)
        val name = editor.name.toRequestBody(plain)
        val description = editor.description.toRequestBody(plain)
        if (editor.mode == AgentEditorMode.CREATE) {
            viewModelScope.launch {
                try {
                    val response = api.createAgent(
                        avatar = null,
                        userId = userId,
                        name = name,
                        description = description
                    )
                    _uiState.update { it.copy(agentEditor = AgentEditorState()) }
                    response.agentModel?.let {
                        MainApplication.getAgentEventManager().upsert(it, EventSourceType.USER_ACTION)
                    }
                    messageListVm.processIntent(MessageListIntent.Refresh)
                } catch (_: Throwable) {
                    _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = false)) }
                    sendEffect(MainEffect.ShowToast("创建 Agent 失败"))
                }
            }
            return
        }
        val agentId = editor.agentId?.toRequestBody(plain) ?: run {
            _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = false)) }
            sendEffect(MainEffect.ShowToast("缺少 AgentId"))
            return
        }
        viewModelScope.launch {
            try {
                val response = api.updateAgent(
                    avatar = null,
                    agentId = agentId,
                    userId = userId,
                    name = name,
                    description = description
                )
                _uiState.update { it.copy(agentEditor = AgentEditorState()) }
                response.agentModel?.let {
                    MainApplication.getAgentEventManager().upsert(it, EventSourceType.USER_ACTION)
                }
                messageListVm.processIntent(MessageListIntent.Refresh)
            } catch (_: Throwable) {
                _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = false)) }
                sendEffect(MainEffect.ShowToast("更新 Agent 失败"))
            }
        }
    }

    private fun deleteCurrentAgent() {
        val editor = _uiState.value.agentEditor
        val agentId = editor.agentId ?: return
        val request = AgentDeleteRequest().apply {
            this.agentId = agentId
            this.userId = MainApplication.getUserId()
        }
        _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = true)) }
        viewModelScope.launch {
            try {
                api.deleteAgent(request)
                _uiState.update { it.copy(agentEditor = AgentEditorState()) }
                MainApplication.getAgentEventManager().remove(agentId, EventSourceType.USER_ACTION)
                messageListVm.processIntent(MessageListIntent.Refresh)
            } catch (_: Throwable) {
                _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = false)) }
                sendEffect(MainEffect.ShowToast("删除 Agent 失败"))
            }
        }
    }

    private fun observeAgentsEffect() {
        viewModelScope.launch {
            agentsManager.effect.collect { effect ->
                when (effect) {
                    AgentsEffect.AgentListChanged -> {
                        syncAgentsDataFromManager()
                    }
                }
            }
        }
    }

    // 从manager获取数据
    private fun syncAgentsDataFromManager() {
        val list = agentsManager.agentList.value
        _dataState.update {
            it.copy(
                agentCount = list.size,
                agentListVersion = System.currentTimeMillis()
            )
        }
    }

    private fun sendEffect(effect: MainEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}


sealed class MainIntent {
    // 初始化意图 - 页面启动时调用
    data class Initialize(val selected: MainSelectItemEnum) : MainIntent()
    // 作用：告诉 ViewModel 页面启动了，并且需要选中某个 Tab
    // 触发时机：onCreate 时调用

    // 切换 Tab 意图 - 用户点击底部导航栏
    data class SelectTab(val tab: MainSelectItemEnum) : MainIntent()
    // 作用：用户想要切换到某个页面（首页/应用/我的）
    // 触发时机：底部导航栏点击时

    // Service 绑定成功意图 - 系统回调
    data class ChatServiceBound(val handler: RealtimeChatController) : MainIntent()
    // 作用：ChatService 已经连接成功，把通信处理器传给 ViewModel
    // 触发时机：ServiceConnection.onServiceConnected

    // Service 解绑意图 - 系统回调
    data object ChatServiceUnbound : MainIntent()
    // 作用：ChatService 断开连接了，需要更新 UI 状态
    // 触发时机：ServiceConnection.onServiceDisconnected 或 onDestroy

    // 打开创建 Agent 意图 - 用户点击按钮
    data object OpenCreateAgent : MainIntent()
    // 作用：用户想要跳转到创建 Agent 页面
    // 触发时机：点击"创建Agent"按钮
    data class OpenEditAgent(val agentId: String) : MainIntent()
    data object CloseAgentEditor : MainIntent()
    data class UpdateEditorName(val name: String) : MainIntent()
    data class UpdateEditorDescription(val description: String) : MainIntent()
    data object SubmitAgentEditor : MainIntent()
    data object DeleteAgent : MainIntent()
}

data class MainState(
    val currentSelected: MainSelectItemEnum = MainSelectItemEnum.HOME,
    val agentEditor: AgentEditorState = AgentEditorState()
)

data class MainDataState(
    val isChatServiceBound: Boolean = false,
    val agentCount: Int = 0,
    val agentListVersion: Long = 0L
)

enum class AgentEditorMode {
    CREATE,
    EDIT
}

data class AgentEditorState(
    val isVisible: Boolean = false,
    val mode: AgentEditorMode = AgentEditorMode.CREATE,
    val agentId: String? = null,
    val name: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false
)

sealed class MainEffect {
    data class ShowToast(val message: String) : MainEffect()
}