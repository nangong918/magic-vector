package com.vectordemo.di

import com.vectordemo.dataSource.local.OssLocalSource
import com.vectordemo.dataSource.local.UserLocalSource
import com.vectordemo.dataSource.remote.OssRemoteApiSource
import com.vectordemo.dataSource.remote.UserRemoteApiSource
import com.vectordemo.domain.config.ModuleKeyConfigStore
import com.vectordemo.manager.OssManager
import com.vectordemo.manager.UserManager
import com.vectordemo.repository.api.ApiClient
import com.vectordemo.repository.api.createPlatformHttpClient
import com.vectordemo.service.ai.AliChatService
import com.vectordemo.service.ai.ChatService
import com.vectordemo.service.ai.XfYunChatService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppContainer {
    private val userIdFlow = MutableStateFlow("")
    private var cachedToken: String? = null

    private val httpClient = createPlatformHttpClient { cachedToken }
    private val apiClient = ApiClient(httpClient)

    private val userLocal = UserLocalSource()
    private val ossLocal = OssLocalSource()
    val userRemote = UserRemoteApiSource(apiClient)
    val ossRemote = OssRemoteApiSource(apiClient)

    val userManager = UserManager(userLocal)
    val ossManager = OssManager(ossRemote, ossLocal)

    private val moduleConfig = ModuleKeyConfigStore.loadFromJson()
    private val aliChatServiceImpl = AliChatService(
        configProvider = { moduleConfig.llmAli },
        httpClient = httpClient,
    )
    val aliChatService: ChatService = aliChatServiceImpl
    val xfYunChatService: ChatService = XfYunChatService(
        configProvider = { moduleConfig.xfyun.llm },
        aliFallback = aliChatServiceImpl,
    )

    val currentUserId = userIdFlow.asStateFlow()

    fun updateUserId(userId: Long) {
        userIdFlow.value = if (userId > 0) userId.toString() else ""
    }

    fun clearUserId() {
        userIdFlow.value = ""
    }

    fun updateToken(token: String?) {
        cachedToken = token?.takeIf { it.isNotBlank() }
    }
}
