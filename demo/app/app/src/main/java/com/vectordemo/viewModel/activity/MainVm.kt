package com.vectordemo.viewModel.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vectordemo.MainApplication
import com.vectordemo.domain.model.demo.DemoCatalogItem
import com.vectordemo.domain.model.demo.DemoRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainVm : ViewModel() {
    private val allItems: List<DemoCatalogItem> = listOf(
        DemoCatalogItem(
            id = "hello",
            title = "Hello Demo",
            subtitle = "Compose + MVI navigation sample",
            route = DemoRoute.HELLO
        ),
        DemoCatalogItem(
            id = "oss",
            title = "OSS Demo",
            subtitle = "上传、存储桶列表、图片下载/更换/删除",
            route = DemoRoute.OSS_DEMO
        )
    )

    private val _uiState = MutableStateFlow(
        MainState(
            query = "",
            items = allItems
        )
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
        }
    }

    private fun logout() {
        viewModelScope.launch {
            MainApplication.getUserManager().clearCurrentUser()
            MainApplication.clearUserId()
            sendEffect(MainEffect.NavigateToLogin)
        }
    }

    private fun loadCurrentUserDisplayName() {
        viewModelScope.launch {
            val currentUser = MainApplication.getUserManager().getCurrentUser()
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
        viewModelScope.launch {
            _effect.send(effect)
        }
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
    val displayUserName: String = "游客"
)

sealed class MainEffect {
    data object NavigateToHello : MainEffect()
    data object NavigateToOssDemo : MainEffect()
    data object NavigateToLogin : MainEffect()
}
