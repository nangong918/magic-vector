package com.vectordemo.di

import com.vectordemo.dataSource.local.OssLocalSource
import com.vectordemo.dataSource.local.UserLocalSource
import com.vectordemo.dataSource.remote.OssRemoteApiSource
import com.vectordemo.dataSource.remote.UserRemoteApiSource
import com.vectordemo.domain.config.ModuleKeyConfig
import com.vectordemo.domain.config.ModuleKeyConfigLoader
import com.vectordemo.domain.config.ModuleKeyConfigStore
import com.vectordemo.manager.OssManager
import com.vectordemo.manager.UserManager
import com.vectordemo.repository.api.ApiClient
import com.vectordemo.repository.api.createExternalAiHttpClient
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
    private val aiHttpClient = createExternalAiHttpClient()
    private val apiClient = ApiClient(httpClient)

    private val userLocal = UserLocalSource()
    private val ossLocal = OssLocalSource()
    val userRemote = UserRemoteApiSource(apiClient)
    val ossRemote = OssRemoteApiSource(apiClient)

    val userManager = UserManager(userLocal)
    val ossManager = OssManager(ossRemote, ossLocal)

    private var moduleConfig: ModuleKeyConfig? = null
    private var aliChatServiceImpl: AliChatService? = null
    private var xfYunChatServiceImpl: XfYunChatService? = null

    val aliChatService: ChatService
        get() = aliChatServiceImpl ?: error("AppContainer 未初始化，请先调用 initialize()")
    val xfYunChatService: ChatService
        get() = xfYunChatServiceImpl ?: error("AppContainer 未初始化，请先调用 initialize()")

    val isInitialized: Boolean
        get() = moduleConfig != null

    suspend fun initialize() {
        if (isInitialized) return
        val content = ModuleKeyConfigLoader.loadContent()
        configureFromJson(content)
    }

    fun configureFromJson(content: String) {
        moduleConfig = ModuleKeyConfigStore.loadFromJson(content)
        aliChatServiceImpl = createAliChatService()
        xfYunChatServiceImpl = createXfYunChatService()
    }

    private fun requireModuleConfig(): ModuleKeyConfig {
        return moduleConfig ?: error("AppContainer 未初始化，请先调用 initialize()")
    }

    private fun createAliChatService(): AliChatService {
        return AliChatService(
            configProvider = { requireModuleConfig().llmAli },
            httpClient = aiHttpClient,
        )
    }

    private fun createXfYunChatService(): XfYunChatService {
        return XfYunChatService(
            configProvider = { requireModuleConfig().xfyun.llm },
            aliFallback = aliChatServiceImpl ?: createAliChatService(),
        )
    }

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
