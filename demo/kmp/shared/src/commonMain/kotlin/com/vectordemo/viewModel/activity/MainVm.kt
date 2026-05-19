package com.vectordemo.viewModel.activity

import androidx.lifecycle.viewModelScope
import com.vectordemo.di.AppContainer
import com.vectordemo.domain.model.demo.DemoCatalogItem
import com.vectordemo.domain.model.demo.DemoRoute
import com.vectordemo.manager.user.UserManager
import com.vectordemo.domain.platform.PlatformFeatures
import com.vectordemo.domain.platform.PlatformType
import com.vectordemo.viewModel.BaseVm
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainVm(
    private val userManager: UserManager,
) : BaseVm() {
    private val allItems: List<DemoCatalogItem> = buildDemoItems()

    private val _uiState = MutableStateFlow(
        MainState(
            query = "",
            items = allItems,
        ),
    )
    val uiState: StateFlow<MainState> = _uiState.asStateFlow()

    private val _effect = Channel<MainEffect>(Channel.BUFFERED)
    val effect: Flow<MainEffect> = _effect.receiveAsFlow()

    init {
        loadCurrentUserDisplayName()
    }

    fun processIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.UpdateQuery -> updateQuery(intent.query)
            is MainIntent.ClickDemo -> onClickDemo(intent.route)
            MainIntent.Logout -> logout()
        }
    }

    private fun updateQuery(query: String) {
        val filtered = if (query.isBlank()) {
            allItems
        } else {
            allItems.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.subtitle.contains(query, ignoreCase = true)
            }
        }
        _uiState.update { it.copy(query = query, items = filtered) }
    }

    private fun onClickDemo(route: DemoRoute) {
        when (route) {
            DemoRoute.HELLO -> sendEffect(MainEffect.NavigateToHello)
            DemoRoute.OSS_DEMO -> sendEffect(MainEffect.NavigateToOssDemo)
            DemoRoute.CHAT_LIST_DEMO -> sendEffect(MainEffect.NavigateToChatList)
            DemoRoute.VOICE_AGENT -> sendEffect(
                if (PlatformFeatures.supportsVoiceAgent) MainEffect.NavigateToVoiceAgent
                else MainEffect.ShowToast("iOS 暂不支持语音链路（Android Service/Native SDK）"),
            )
            DemoRoute.LIVE_PUSH -> sendEffect(
                if (PlatformFeatures.supportsLiveStreaming) MainEffect.NavigateToLivePush
                else MainEffect.ShowToast("iOS 暂不支持直播推流能力"),
            )
            DemoRoute.LIVE_PULL -> sendEffect(
                if (PlatformFeatures.supportsLiveStreaming) MainEffect.NavigateToLivePull
                else MainEffect.ShowToast("iOS 暂不支持直播拉流能力"),
            )
            DemoRoute.STL_CPP -> sendEffect(
                if (PlatformFeatures.supportsKni) MainEffect.NavigateToStlCpp
                else MainEffect.ShowToast("iOS 暂不支持 JNI/KNI（C++）能力"),
            )
        }
    }

    private fun logout() {
        viewModelScope.launch {
            userManager.clearCurrentUser()
            AppContainer.clearUserId()
            AppContainer.updateToken(null)
            sendEffect(MainEffect.NavigateToLogin)
        }
    }

    private fun loadCurrentUserDisplayName() {
        viewModelScope.launch {
            val currentUser = userManager.getCurrentUser()
            val displayName = when {
                currentUser == null -> "游客"
                currentUser.accessToken == "tourist" || currentUser.account == "tourist" || currentUser.userId == 1L -> "游客"
                currentUser.name.isNotBlank() -> currentUser.name
                else -> currentUser.account
            }
            _uiState.update { it.copy(displayUserName = displayName) }
        }
    }

    private fun sendEffect(effect: MainEffect) {
        viewModelScope.launch { _effect.send(effect) }
    }

    private fun buildDemoItems(): List<DemoCatalogItem> {
        val platformTag = when (PlatformFeatures.platformType) {
            PlatformType.ANDROID -> "Android"
            PlatformType.IOS -> "iOS"
        }
        return listOf(
            DemoCatalogItem("hello", "Hello Demo", "Compose + MVI navigation sample ($platformTag)", DemoRoute.HELLO),
            DemoCatalogItem("oss", "OSS Demo", "上传、存储桶列表、图片下载/更换/删除", DemoRoute.OSS_DEMO),
            DemoCatalogItem("chat_list", "ChatList Demo", "流式聊天对话（Flutter ChatPage迁移）", DemoRoute.CHAT_LIST_DEMO),
            DemoCatalogItem("voice_agent", "Voice Agent", "离线唤醒 + VAD + STT + LLM（$platformTag）", DemoRoute.VOICE_AGENT),
            DemoCatalogItem("live_push", "Live Push Demo", "RTMP + X264 实时推流（Android）", DemoRoute.LIVE_PUSH),
            DemoCatalogItem("live_pull", "Live Pull Demo", "RTMP/HLS 拉流播放（Android）", DemoRoute.LIVE_PULL),
            DemoCatalogItem("stl_cpp", "C++ STL / KNI", "STL 容器、KNI 互调、C++ 推消息与抛异常（Android）", DemoRoute.STL_CPP),
        )
    }
}

sealed class MainIntent {
    data class UpdateQuery(val query: String) : MainIntent()
    data class ClickDemo(val route: DemoRoute) : MainIntent()
    data object Logout : MainIntent()
}

data class MainState(
    val query: String,
    val items: List<DemoCatalogItem>,
    val displayUserName: String = "游客",
)

sealed class MainEffect {
    data object NavigateToHello : MainEffect()
    data object NavigateToOssDemo : MainEffect()
    data object NavigateToChatList : MainEffect()
    data object NavigateToVoiceAgent : MainEffect()
    data object NavigateToLivePush : MainEffect()
    data object NavigateToLivePull : MainEffect()
    data object NavigateToStlCpp : MainEffect()
    data object NavigateToLogin : MainEffect()
    data class ShowToast(val message: String) : MainEffect()
}
