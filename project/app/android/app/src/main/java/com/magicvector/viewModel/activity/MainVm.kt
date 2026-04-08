package com.magicvector.viewModel.activity

import android.Manifest
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magicvector.domain.dto.http.request.AgentDeleteRequest
import com.magicvector.MainApplication
import com.magicvector.domain.constant.MainSelectEnum
import com.magicvector.manager.event.agent.AgentEventManager
import com.magicvector.manager.realtime.RealtimeChatController
import com.magicvector.viewModel.fragment.ControlVm
import com.magicvector.viewModel.fragment.MessageListIntent
import com.magicvector.viewModel.fragment.MessageListVm
import com.magicvector.viewModel.fragment.MineVm
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
        private val agentEventManager: AgentEventManager = MainApplication.getAgentEventManager()
    }

    // ========== 子 ViewModel ==========
    // MainVm 统一持有各 Tab 子 VM，避免 Composable 重建时状态丢失。
    val messageListVm: MessageListVm = MessageListVm()
    val controlVm: ControlVm = ControlVm()
    val mineVm: MineVm = MineVm()

    // ========== RealtimeChatController 引用 ==========
    // 保留对外控制器引用，避免影响其他页面后续接入。
    private var _realtimeChatController: RealtimeChatController? = null
    val realtimeChatController: RealtimeChatController? get() = _realtimeChatController

    // ========== StateFlow (UI State) ==========
    private val _uiState = MutableStateFlow(MainState())
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

    private val _dataState = MutableStateFlow(MainDataState())
    val dataState: StateFlow<MainDataState> = _dataState.asStateFlow()

    private val _effect = Channel<MainEffect>(Channel.BUFFERED)
    val effect: Flow<MainEffect> = _effect.receiveAsFlow()

    init {
        observeAgentManager()
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
                _uiState.update {
                    it.copy(agentEditor = AgentEditorState(isVisible = true, mode = AgentEditorMode.CREATE))
                }
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
                _realtimeChatController = intent.chatController
                _realtimeChatController?.ensureUserConnection(MainApplication.getUserId())
                MainApplication.getNetworkManager().bindWsReconnectAction {
                    _realtimeChatController?.reconnectUserConnectionIfNeeded()
                }
                _dataState.update { it.copy(isChatServiceBound = true) }
            }

            MainIntent.ChatServiceUnbound -> {
                MainApplication.getNetworkManager().unbindWsReconnectAction()
                _realtimeChatController = null
                _dataState.update { it.copy(isChatServiceBound = false) }
            }
        }
    }

    // ========== Agent 编辑相关 ==========
    /**
     * 打开编辑 Agent 弹窗
     * @param agentId Agent ID
     */
    private fun openEditAgent(agentId: Long) {
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
                val agentModel = api.getAgentInfo(agentId.toString())
                _uiState.update {
                    it.copy(
                        agentEditor = it.agentEditor.copy(
                            isLoading = false,
                            name = agentModel.agentChatVo?.agentVo?.name?: "",
                            description = agentModel.agentChatVo?.agentVo?.description?: ""
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "openEditAgent failed", e)
                _uiState.update { it.copy(agentEditor = AgentEditorState()) }
                sendEffect(MainEffect.ShowToast("获取 Agent 详情失败"))
            }
        }
    }

    /**
     * 提交 Agent 编辑（创建或更新）
     */
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
            createAgent(userId, name, description)
        } else {
            updateAgent(editor.agentId, userId, name, description)
        }
    }

    /**
     * 创建 Agent
     */
    private fun createAgent(
        userId: okhttp3.RequestBody,
        name: okhttp3.RequestBody,
        description: okhttp3.RequestBody
    ) {
        viewModelScope.launch {
            try {
                val agentModel = api.createAgent(
                    avatar = null,
                    userId = userId,
                    name = name,
                    description = description
                )
                _uiState.update { it.copy(agentEditor = AgentEditorState()) }
                agentModel.let { agent ->
                    // 使用新的 AgentEventManager 添加 Agent
                    agentEventManager.onUserAddOne(agent)
                }
                // 刷新消息列表
                messageListVm.processIntent(MessageListIntent.Refresh)
                sendEffect(MainEffect.ShowToast("创建成功"))
            } catch (e: Exception) {
                Log.e(TAG, "createAgent failed", e)
                _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = false)) }
                sendEffect(MainEffect.ShowToast("创建 Agent 失败"))
            }
        }
    }

    /**
     * 更新 Agent
     */
    private fun updateAgent(
        agentId: Long?,
        userId: okhttp3.RequestBody,
        name: okhttp3.RequestBody,
        description: okhttp3.RequestBody
    ) {
        val agentIdBody = agentId?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
        if (agentIdBody == null) {
            _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = false)) }
            sendEffect(MainEffect.ShowToast("缺少 AgentId"))
            return
        }

        viewModelScope.launch {
            try {
                val agentModel = api.updateAgent(
                    avatar = null,
                    agentId = agentIdBody,
                    userId = userId,
                    name = name,
                    description = description
                )
                _uiState.update { it.copy(agentEditor = AgentEditorState()) }
                agentModel.let { it ->
                    // 使用新的 AgentEventManager 更新 Agent
                    agentEventManager.onUserAddOne(it)
                }
                messageListVm.processIntent(MessageListIntent.Refresh)
                sendEffect(MainEffect.ShowToast("更新成功"))
            } catch (e: Exception) {
                Log.e(TAG, "updateAgent failed", e)
                _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = false)) }
                sendEffect(MainEffect.ShowToast("更新 Agent 失败"))
            }
        }
    }

    /**
     * 删除当前编辑的 Agent
     */
    private fun deleteCurrentAgent() {
        val editor = _uiState.value.agentEditor
        val agentId = editor.agentId ?: run {
            sendEffect(MainEffect.ShowToast("缺少 AgentId"))
            return
        }

        val request = AgentDeleteRequest().apply {
            this.agentId = agentId.toString()
            this.userId = MainApplication.getUserId()
        }

        _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = true)) }

        viewModelScope.launch {
            try {
                api.deleteAgent(request)
                _uiState.update { it.copy(agentEditor = AgentEditorState()) }
                // 使用新的 AgentEventManager 删除 Agent
                agentEventManager.onUserDelete(agentId)
                messageListVm.processIntent(MessageListIntent.Refresh)
                sendEffect(MainEffect.ShowToast("删除成功"))
            } catch (e: Exception) {
                Log.e(TAG, "deleteCurrentAgent failed", e)
                _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = false)) }
                sendEffect(MainEffect.ShowToast("删除 Agent 失败"))
            }
        }
    }

    // ========== Agent 数据同步 ==========
    /**
     * 监听 AgentEventManager 的数据变化
     */
    private fun observeAgentManager() {
        viewModelScope.launch {
            // 监听 Agent 列表变化，同步到 dataState
            agentEventManager.items.collect { agents ->
                _dataState.update {
                    it.copy(
                        agentCount = agents.size,
                        agentListVersion = System.currentTimeMillis()
                    )
                }
            }
        }

        viewModelScope.launch {
            // 监听 Agent 事件，用于调试或特殊处理
            agentEventManager.events.collect { event ->
                Log.d(TAG, "Agent event: $event")
            }
        }
    }

    /**
     * 从 Manager 同步 Agent 数据（用于初始化）
     */
    private fun syncAgentsDataFromManager() {
        val agents = agentEventManager.items.value
        _dataState.update {
            it.copy(
                agentCount = agents.size,
                agentListVersion = System.currentTimeMillis()
            )
        }
    }

    // ========== 辅助方法 ==========
    private fun sendEffect(effect: MainEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}

// ========== Intent ==========
sealed class MainIntent {
    /** 初始化意图 - 页面启动时调用，选中某个 Tab */
    data class Initialize(val selected: MainSelectEnum) : MainIntent()

    /** 切换 Tab 意图 - 用户点击底部导航栏 */
    data class SelectTab(val tab: MainSelectEnum) : MainIntent()

    /** Service 绑定成功意图 - 系统回调 */
    data class ChatServiceBound(val chatController: RealtimeChatController) : MainIntent()

    /** Service 解绑意图 - 系统回调 */
    data object ChatServiceUnbound : MainIntent()

    /** 打开创建 Agent 意图 - 用户点击按钮 */
    data object OpenCreateAgent : MainIntent()

    /** 打开编辑 Agent 意图 */
    data class OpenEditAgent(val agentId: Long) : MainIntent()

    /** 关闭 Agent 编辑弹窗 */
    data object CloseAgentEditor : MainIntent()

    /** 更新编辑框名称 */
    data class UpdateEditorName(val name: String) : MainIntent()

    /** 更新编辑框描述 */
    data class UpdateEditorDescription(val description: String) : MainIntent()

    /** 提交 Agent 编辑（创建或更新） */
    data object SubmitAgentEditor : MainIntent()

    /** 删除当前 Agent */
    data object DeleteAgent : MainIntent()
}

// ========== State ==========
data class MainState(
    val currentSelected: MainSelectEnum = MainSelectEnum.AGENT,
    val agentEditor: AgentEditorState = AgentEditorState()
)

data class MainDataState(
    val isChatServiceBound: Boolean = false,
    val agentCount: Int = 0,
    val agentListVersion: Long = 0L
)

// ========== Agent 编辑相关 ==========
enum class AgentEditorMode {
    CREATE,   // 创建模式
    EDIT      // 编辑模式
}

data class AgentEditorState(
    val isVisible: Boolean = false,
    val mode: AgentEditorMode = AgentEditorMode.CREATE,
    val agentId: Long? = null,  // 改为可空类型，创建模式时为 null
    val name: String = "",
    val description: String = "",
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false
)

// ========== Effect ==========
sealed class MainEffect {
    data class ShowToast(val message: String) : MainEffect()
}