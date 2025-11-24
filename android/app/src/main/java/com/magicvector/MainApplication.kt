package com.magicvector

import android.app.Application
import com.core.appcore.api.ApiRequest
import com.core.appcore.api.ApiRequestProvider
import com.core.baseutil.image.ImageManager
import com.data.dao.api.ApiRequestImpl
import com.data.domain.ao.mixLLM.McpSwitch
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.magicvector.manager.ChatMapController
import com.magicvector.manager.MessageListController
import com.magicvector.manager.vl.UdpVisionManager
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

        // 请求接口实现
        private var apiRequestImplInstance: ApiRequestImpl? = null

        @Synchronized
        fun getApiRequestImplInstance(): ApiRequestImpl {
            if (apiRequestImplInstance == null) {
                apiRequestImplInstance = ApiRequestImpl(getApiRequestInstance()!!)
            }

            return apiRequestImplInstance!!
        }

        fun getUserId(): String{
            // todo 测试用的userId，正式的时候需要修改
            return "test_user"
        }

        private val messageListController = MessageListController()
        fun getMessageListManager(): MessageListController {
            return messageListController
        }

        // chatMapManager
        private var chatMapController: ChatMapController? = null
        fun getChatMapManager(): ChatMapController {
            if (chatMapController == null) {
                chatMapController = ChatMapController()
            }
            return chatMapController!!
        }

        // VisionManager
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

        // Udp vision
        private var udpVisionManager: UdpVisionManager? = null
        fun getUdpVisionManager(): UdpVisionManager {
            if (udpVisionManager == null) {
                udpVisionManager = UdpVisionManager.getInstance()
            }
            return udpVisionManager!!
        }
    }

    //----------------------------utils----------------------------



    //----------------------------APP终止的时候调用----------------------------

    override fun onTerminate() {
        super.onTerminate()
    }
}