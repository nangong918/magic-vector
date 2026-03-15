package com.magicvector.viewModel.activity

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.magicvector.repository.api.handler.SyncRequestCallback
import com.magicvector.repository.api.utils.AppResponseUtil
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.dto.response.AgentResponse
import com.magicvector.MainApplication
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ComposeAgentInfoVm : ViewModel() {

    companion object {
        val TAG: String = ComposeAgentInfoVm::class.java.name
    }

    // MVI State
    private val _uiState = MutableStateFlow(ComposeAgentInfoState())
    val uiState: StateFlow<ComposeAgentInfoState> = _uiState.asStateFlow()

    // MVI Effect
    private val _effect = Channel<ComposeAgentInfoEffect>(Channel.BUFFERED)
    val effect: Flow<ComposeAgentInfoEffect> = _effect.receiveAsFlow()

    private val api = MainApplication.getApiRequestImplInstance()

    fun processIntent(intent: ComposeAgentInfoIntent) {
        when (intent) {
            is ComposeAgentInfoIntent.Initialize -> initialize(intent.agentId, intent.context)
            is ComposeAgentInfoIntent.UpdateName -> _uiState.update { it.copy(name = intent.value) }
            is ComposeAgentInfoIntent.UpdateDescription -> _uiState.update { it.copy(description = intent.value) }
            ComposeAgentInfoIntent.ClickBack -> sendEffect(ComposeAgentInfoEffect.Finish)
            ComposeAgentInfoIntent.ClickAvatar -> sendEffect(ComposeAgentInfoEffect.RequestStoragePermission)
            is ComposeAgentInfoIntent.StoragePermissionResult -> {
                if (intent.granted) {
                    sendEffect(ComposeAgentInfoEffect.OpenImagePicker)
                } else {
                    sendEffect(ComposeAgentInfoEffect.ShowToastRes(com.view.appview.R.string.please_give_permission))
                }
            }
            is ComposeAgentInfoIntent.OnAvatarSelected -> {
                _uiState.update { it.copy(avatarUri = intent.uri) }
            }
            ComposeAgentInfoIntent.ClickConfirm -> {
                sendEffect(ComposeAgentInfoEffect.ShowToastRes(com.view.appview.R.string.under_development))
            }
            ComposeAgentInfoIntent.ClickDelete -> {
                sendEffect(ComposeAgentInfoEffect.ShowToastRes(com.view.appview.R.string.under_development))
            }
        }
    }

    private fun initialize(agentId: String?, context: Context) {
        if (agentId.isNullOrEmpty()) {
            sendEffect(ComposeAgentInfoEffect.ShowToastRes(com.view.appview.R.string.agent_is_not_found))
            sendEffect(ComposeAgentInfoEffect.Finish)
            return
        }
        _uiState.update { it.copy(agentId = agentId, isLoading = true) }
        sendEffect(ComposeAgentInfoEffect.ShowLoading)
        doGetAgentInfo(context, agentId, object : SyncRequestCallback {
            override fun onThrowable(throwable: Throwable?) {
                Log.e(TAG, "getAgentInfo error", throwable)
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(ComposeAgentInfoEffect.HideLoading)
            }

            override fun onAllRequestSuccess() {
                _uiState.update { it.copy(isLoading = false) }
                sendEffect(ComposeAgentInfoEffect.HideLoading)
            }
        })
    }

    // 复用原 AgentInfoVm 网络逻辑
    private fun doGetAgentInfo(context: Context, agentId: String, callback: SyncRequestCallback) {
        api.getAgentInfo(
            agentId,
            object : OnSuccessCallback<BaseResponse<AgentResponse>> {
                override fun onResponse(response: BaseResponse<AgentResponse>?) {
                    AppResponseUtil.handleSyncResponseEx(
                        response,
                        context,
                        callback,
                        ::handleGetAgentInfo
                    )
                }
            },
            object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    callback.onThrowable(throwable)
                }
            }
        )
    }

    private fun handleGetAgentInfo(
        response: BaseResponse<AgentResponse>?,
        context: Context,
        callback: SyncRequestCallback
    ) {
        response?.data?.agentAo?.agentVo?.let { vo ->
            _uiState.update {
                it.copy(
                    avatarUrl = vo.avatarUrl,
                    name = vo.name ?: "",
                    description = vo.description ?: ""
                )
            }
        }
        callback.onAllRequestSuccess()
    }

    private fun sendEffect(effect: ComposeAgentInfoEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }
}

@Stable
data class ComposeAgentInfoState(
    val isLoading: Boolean = false,
    val agentId: String = "",
    val avatarUrl: String? = null,
    val avatarUri: Uri? = null,
    val name: String = "",
    val description: String = "",
)

sealed class ComposeAgentInfoIntent {
    data class Initialize(val agentId: String?, val context: Context) : ComposeAgentInfoIntent()
    data class UpdateName(val value: String) : ComposeAgentInfoIntent()
    data class UpdateDescription(val value: String) : ComposeAgentInfoIntent()
    data object ClickBack : ComposeAgentInfoIntent()
    data object ClickAvatar : ComposeAgentInfoIntent()
    data class StoragePermissionResult(val granted: Boolean) : ComposeAgentInfoIntent()
    data class OnAvatarSelected(val uri: Uri?) : ComposeAgentInfoIntent()
    data object ClickConfirm : ComposeAgentInfoIntent()
    data object ClickDelete : ComposeAgentInfoIntent()
}

sealed class ComposeAgentInfoEffect {
    data object Finish : ComposeAgentInfoEffect()
    data object RequestStoragePermission : ComposeAgentInfoEffect()
    data object OpenImagePicker : ComposeAgentInfoEffect()
    data object ShowLoading : ComposeAgentInfoEffect()
    data object HideLoading : ComposeAgentInfoEffect()
    data class ShowToastRes(val resId: Int) : ComposeAgentInfoEffect()
}
