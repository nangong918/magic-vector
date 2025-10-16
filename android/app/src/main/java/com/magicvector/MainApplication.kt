package com.magicvector

import android.app.Application
import android.util.Log
import com.core.appcore.api.ApiRequest
import com.core.appcore.api.ApiRequestProvider
import com.core.baseutil.image.ImageManager
import com.data.dao.api.ApiRequestImpl
import com.magicvector.manager.ChatMapManager
import com.magicvector.manager.MessageListManager

class MainApplication : Application() {

    val tag = MainApplication::class.simpleName

    lateinit var mApp: MainApplication

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

        private val messageListManager = MessageListManager()
        fun getMessageListManager(): MessageListManager {
            return messageListManager
        }

        // chatMapManager
        private val chatMapManager = ChatMapManager()
        fun getChatMapManager(): ChatMapManager {
            return chatMapManager
        }
    }

    //----------------------------utils----------------------------


    //----------------------------APP终止的时候调用----------------------------
    override fun onTerminate() {
        super.onTerminate()
    }
}