package com.magicvector

import android.app.Application
import com.magicvector.repository.api.ApiRequest
import com.magicvector.repository.api.config.ApiRequestProvider
import com.core.baseutil.image.ImageManager
import com.data.domain.ao.mixLLM.McpSwitch
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.magicvector.dataSource.local.AgentLocalSource
import com.magicvector.dataSource.local.ChatLocalSource
import com.magicvector.dataSource.remote.RemoteApiSource
import com.magicvector.manager.control.ControlAgentLogManager
import com.magicvector.manager.control.ControlConsoleManager
import com.magicvector.manager.event.agent.AgentEventManager
import com.magicvector.manager.event.chat.ChatEventMapManager
import com.magicvector.manager.network.NetworkManager
import com.magicvector.manager.user.UserManager
import com.magicvector.manager.yolo.VisionManager

class MainApplication : Application() {

    val tag = "MainApplication"

    //----------------------------启动APP调用----------------------------

    override fun onCreate() {
        super.onCreate()
        mApp = this
        initGlobal()
    }

    //----------------------------global----------------------------

    private fun initGlobal() {
        apiRequestInstance = getApiRequestInstance()
        getNetworkManager().register()
    }

    companion object {
        //==========App
        private lateinit var mApp: MainApplication
        fun getApp(): MainApplication {
            return mApp
        }

        //==========Gson

        val GSON: Gson = GsonBuilder().setPrettyPrinting().create()

        //==========ApiRequest

        private var apiRequestInstance: ApiRequest? = null

        // 请求接口
        @Synchronized
        private fun getApiRequestInstance(): ApiRequest? {
            if (apiRequestInstance == null) {
                apiRequestInstance = ApiRequestProvider.getApiRequest()
            }
            return apiRequestInstance
        }

        private var imageManager: ImageManager? = null

        @Synchronized
        fun getImageManager(): ImageManager? {
            if (imageManager == null) {
                imageManager = ImageManager()
            }
            return imageManager
        }

        // 远程数据源
        private var remoteApiSource: RemoteApiSource? = null
        @Volatile
        private var cachedUserId: String = ""

        @Synchronized
        fun getRemoteApiSource(): RemoteApiSource {
            if (remoteApiSource == null) {
                remoteApiSource = RemoteApiSource(getApiRequestInstance()!!)
            }
            return remoteApiSource!!
        }

        // 兼容旧调用入口，逐步迁移到 getRemoteApiSource()
        fun getApiRequestImplInstance(): RemoteApiSource {
            return getRemoteApiSource()
        }

        fun getUserId(): String{
            return cachedUserId
        }

        fun updateUserId(userId: Long) {
            cachedUserId = if (userId > 0) userId.toString() else ""
        }

        fun clearUserId() {
            cachedUserId = ""
        }

        private var userManager: UserManager? = null
        fun getUserManager(): UserManager {
            if (userManager == null) {
                userManager = UserManager.getInstance(getApp())
            }
            return userManager!!
        }

        private var agentEventManager: AgentEventManager? = null
        fun getAgentEventManager(): AgentEventManager {
            if (agentEventManager == null) {
                agentEventManager = AgentEventManager()
            }
            return agentEventManager!!
        }

        private var chatEventMapManager: ChatEventMapManager? = null
        fun getChatEventMapManager(): ChatEventMapManager {
            if (chatEventMapManager == null) {
                chatEventMapManager = ChatEventMapManager.getInstance()
            }
            return chatEventMapManager!!
        }

        private var agentLocalSource: AgentLocalSource? = null
        fun getAgentLocalSource(): AgentLocalSource {
            if (agentLocalSource == null) {
                agentLocalSource = AgentLocalSource.getInstance(getApp())
            }
            return agentLocalSource!!
        }

        private var chatLocalSource: ChatLocalSource? = null
        fun getChatLocalSource(): ChatLocalSource {
            if (chatLocalSource == null) {
                chatLocalSource = ChatLocalSource.getInstance(getApp())
            }
            return chatLocalSource!!
        }

        private var networkManager: NetworkManager? = null
        fun getNetworkManager(): NetworkManager {
            if (networkManager == null) {
                networkManager = NetworkManager(getApp())
            }
            return networkManager!!
        }

        private var controlConsoleManager: ControlConsoleManager? = null
        fun getControlConsoleManager(): ControlConsoleManager {
            if (controlConsoleManager == null) {
                controlConsoleManager = ControlConsoleManager()
            }
            return controlConsoleManager!!
        }

        private var controlAgentLogManager: ControlAgentLogManager? = null
        fun getControlAgentLogManager(): ControlAgentLogManager {
            if (controlAgentLogManager == null) {
                controlAgentLogManager = ControlAgentLogManager()
            }
            return controlAgentLogManager!!
        }

        /**
         * VisionManager
         * 本地CameraX管理，无ws传输，无需放入realtimeChatController；
         * 而udpVisionManager涉及到Udp传输，需要放入realtimeChatController
         * 不涉及到跨Activity任务，无需使用Service
         */
        private var visionManager: VisionManager? = null
        fun getVisionManager(): VisionManager {
            if (visionManager == null) {
                visionManager = VisionManager()
            }
            return visionManager!!
        }

        // McpSwitch
        private var mcpSwitch: McpSwitch? = null
        fun getMcpSwitch(): McpSwitch {
            if (mcpSwitch == null) {
                mcpSwitch = McpSwitch()
                mcpSwitch!!.camera = McpSwitch.McpSwitchMode.COMMANDS.code
                mcpSwitch!!.motion = McpSwitch.McpSwitchMode.FREELY.code
                mcpSwitch!!.emojiAndMood = McpSwitch.McpSwitchMode.FREELY.code
                mcpSwitch!!.equipment = McpSwitch.McpEquipment.PHONE.code
            }
            return mcpSwitch!!
        }
    }

    //----------------------------utils----------------------------



    //----------------------------APP终止的时候调用----------------------------

    override fun onTerminate() {
        getNetworkManager().unregister()
        super.onTerminate()
    }
}