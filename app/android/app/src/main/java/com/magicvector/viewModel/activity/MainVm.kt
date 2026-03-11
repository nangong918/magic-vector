package com.magicvector.viewModel.activity

import androidx.lifecycle.ViewModel
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.dto.request.AgentDeleteRequest
import com.data.domain.dto.response.AgentResponse
import com.magicvector.MainApplication
import com.magicvector.manager.RealtimeChatController
import com.view.appview.MainSelectItemEnum
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

// todo 1.界定effect和intent 2.确认intent再去调用effect是否多余行为
class MainVm : ViewModel() {

    companion object {
        val TAG: String = MainVm::class.java.name
    }

    // 保留对外控制器引用，避免影响其他页面后续接入。
    var realtimeChatController: RealtimeChatController? = null

    // StateFlow (UI State) 存储 UI 状态
    private val _uiState = MutableStateFlow(MainState())
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

    private val api = MainApplication.getApiRequestImplInstance()
    private val _agentListEvent = MutableSharedFlow<AgentListEvent>(extraBufferCapacity = 16)
    val agentListEvent: SharedFlow<AgentListEvent> = _agentListEvent.asSharedFlow()

    fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.Initialize -> {
                _uiState.update {
                    it.copy(
                        currentSelected = intent.selected
                    )
                }
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
                    realtimeChatController?.ensureUserConnection(MainApplication.getUserId())
                }
                _uiState.update { it.copy(isChatServiceBound = true) }
            }

            MainIntent.ChatServiceUnbound -> {
                MainApplication.getNetworkManager().unbindWsReconnectAction()
                realtimeChatController = null
                _uiState.update { it.copy(isChatServiceBound = false) }
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
        api.getAgentInfo(
            agentId,
            object : OnSuccessCallback<BaseResponse<AgentResponse>> {
                override fun onResponse(response: BaseResponse<AgentResponse>?) {
                    val vo = response?.data?.agentAo?.agentVo
                    _uiState.update {
                        it.copy(
                            agentEditor = it.agentEditor.copy(
                                isLoading = false,
                                name = vo?.name ?: "",
                                description = vo?.description ?: ""
                            )
                        )
                    }
                }
            },
            object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    _uiState.update { it.copy(agentEditor = AgentEditorState()) }
                }
            }
        )
    }

    private fun submitAgentEditor() {
        val editor = _uiState.value.agentEditor
        if (editor.name.isBlank() || editor.description.isBlank()) {
            return
        }
        _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = true)) }
        val plain = "text/plain".toMediaTypeOrNull()
        val userId = MainApplication.getUserId().toRequestBody(plain)
        val name = editor.name.toRequestBody(plain)
        val description = editor.description.toRequestBody(plain)
        if (editor.mode == AgentEditorMode.CREATE) {
            api.createAgent(
                avatar = null,
                userId = userId,
                name = name,
                description = description,
                onSuccessCallback = object : OnSuccessCallback<BaseResponse<AgentResponse>> {
                    override fun onResponse(response: BaseResponse<AgentResponse>?) {
                        _uiState.update { it.copy(agentEditor = AgentEditorState()) }
                        _agentListEvent.tryEmit(AgentListEvent.Created(response?.data?.agentAo?.agentId.orEmpty()))
                    }
                },
                throwableCallback = object : OnThrowableCallback {
                    override fun callback(throwable: Throwable?) {
                        _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = false)) }
                    }
                }
            )
            return
        }
        val agentId = editor.agentId?.toRequestBody(plain) ?: run {
            _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = false)) }
            return
        }
        api.updateAgent(
            avatar = null,
            agentId = agentId,
            userId = userId,
            name = name,
            description = description,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<AgentResponse>> {
                override fun onResponse(response: BaseResponse<AgentResponse>?) {
                    _uiState.update { it.copy(agentEditor = AgentEditorState()) }
                    _agentListEvent.tryEmit(AgentListEvent.Updated(response?.data?.agentAo?.agentId.orEmpty()))
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = false)) }
                }
            }
        )
    }

    private fun deleteCurrentAgent() {
        val editor = _uiState.value.agentEditor
        val agentId = editor.agentId ?: return
        val request = AgentDeleteRequest().apply {
            this.agentId = agentId
            this.userId = MainApplication.getUserId()
        }
        _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = true)) }
        api.deleteAgent(
            request = request,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<AgentResponse>> {
                override fun onResponse(response: BaseResponse<AgentResponse>?) {
                    _uiState.update { it.copy(agentEditor = AgentEditorState()) }
                    _agentListEvent.tryEmit(AgentListEvent.Deleted(agentId))
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    _uiState.update { it.copy(agentEditor = it.agentEditor.copy(isSubmitting = false)) }
                }
            }
        )
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
    val isChatServiceBound: Boolean = false,
    val agentEditor: AgentEditorState = AgentEditorState()
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

sealed class AgentListEvent {
    data class Created(val agentId: String) : AgentListEvent()
    data class Updated(val agentId: String) : AgentListEvent()
    data class Deleted(val agentId: String) : AgentListEvent()
}