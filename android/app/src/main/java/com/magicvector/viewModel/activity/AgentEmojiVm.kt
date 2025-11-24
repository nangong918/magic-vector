package com.magicvector.viewModel.activity

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import com.core.appcore.api.handler.SyncRequestCallback
import com.core.appcore.utils.AppResponseUtil
import com.core.baseutil.file.FileUtil
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.magicvector.MainApplication
import com.magicvector.manager.RealtimeChatController
import com.magicvector.manager.vl.UdpVisionManager
import com.magicvector.service.ChatService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class AgentEmojiVm(
) : AndroidViewModel(
    application = MainApplication.getApp()
){

    companion object {
        val TAG: String = AgentEmojiVm::class.java.name
        val GSON = MainApplication.GSON
        val mApi = MainApplication.getApiRequestImplInstance()
    }

    //-----------------------Ao-----------------------

    // service
    // ❌ 不要这样：private var chatService: ChatService? = null 因为Service本身是一个Context而且生命周期大于ViewModel，ViewModel直接持有会造成内存泄露
    // ✅ 改为：只持有 Binder 或通过 Binder 调用方法
    private var chatServiceBinder: ChatService.ChatServiceBinder? = null
    val chatServiceBoundLd = MutableLiveData(false)
    private val chatServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            val binder = service as ChatService.ChatServiceBinder
            chatServiceBoundLd.postValue(true)
            // 连接成功使用之后
            // 获取ChatMessageHandler
            realtimeChatController = binder.getChatMessageHandler()

            // 成功绑定的回调
            onBoundChatService?.run()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            chatServiceBoundLd.postValue(false)
            chatServiceBinder = null
            realtimeChatController = null
        }

        override fun onBindingDied(name: ComponentName?) {
            super.onBindingDied(name)
            // 1. Service 所在的进程被系统杀死（内存不足）
            // 2. Service 进程崩溃
            // 3. 系统资源紧张时主动清理

            Log.w(TAG, "Service binding died - process was killed")
            chatServiceBoundLd.postValue(false)

            // 需要重新绑定
            // rebindService() // 可以在这里自动重试
        }

        override fun onNullBinding(name: ComponentName?) {
            super.onNullBinding(name)
            // Service 的 onBind() 返回了 null
            Log.e(TAG, "Service returned null binding - check Service implementation")
            chatServiceBoundLd.postValue(false)
        }
    }

    var onBoundChatService: kotlinx.coroutines.Runnable? = null

    fun initService(onBoundChatService: kotlinx.coroutines.Runnable){
        this.onBoundChatService = onBoundChatService
        // 启动Service的intent
        val intent = Intent(application, ChatService::class.java)
        // 尝试绑定服务，乐观认为mainActivity已经启动了service
        application.bindService(intent, chatServiceConnection, BIND_AUTO_CREATE)
    }

    fun disconnectService() {
        application.let { context ->
            if (chatServiceBoundLd.value == true) {
                try {
                    context.unbindService(chatServiceConnection)
                    Log.d(TAG, "Service disconnected successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Error disconnecting service", e)
                }
            }
        }
        chatServiceBoundLd.postValue(false)
        chatServiceBinder = null
        realtimeChatController = null
    }

    //-----------------------Repository-----------------------

    /**
     * vision任务，http方式上传single图片
     * @param context       上下文
     * @param images        图片文件
     * @param agentId       客服id
     * @param userId        用户id
     * @param messageId     消息id
     * @param callback      回调
     */
    fun doUploadImageVision(context: Context,
                            images: List<File>,
                            agentId: String,
                            userId: String,
                            messageId: String,
                            callback: SyncRequestCallback){

        // 创建 MultipartBody.Part 列表
        val imageParam: List<MultipartBody.Part>? = FileUtil.createImageMultipartBodyParts(
            images,
            "images"
        )

        if (imageParam == null){
            callback.onThrowable(Throwable("image is null"))
            return
        }

        // 创建其他参数请求体
        val agentIdParam = RequestBody.create("text/plain".toMediaTypeOrNull(), agentId)
        val userIdParam = RequestBody.create("text/plain".toMediaTypeOrNull(), userId)
        val messageIdParam = RequestBody.create("text/plain".toMediaTypeOrNull(), messageId)

        mApi.uploadImageVision(
            images = imageParam,
            agentId = agentIdParam,
            userId = userIdParam,
            messageId = messageIdParam,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<String>>{
                override fun onResponse(response: BaseResponse<String>?) {
                    AppResponseUtil.handleSyncResponseEx(
                        response = response,
                        context = context,
                        callback = callback,
                        handler = { response, context ->
                            handleUploadImageVision(context, response, callback)
                        }
                    )
                }

            },
            throwableCallback = object : OnThrowableCallback{
                override fun callback(throwable: Throwable?) {
                    callback.onThrowable(throwable)
                }
            }
        )
    }

    private fun handleUploadImageVision(context: Context, response: BaseResponse<String>?, callback: SyncRequestCallback){
        response?.let {
            println("response: ${it.data}")
            println("response: ${it.message}")
        }
        callback.onAllRequestSuccess()
    }

    // vision测试
    fun visionTest(realtimeChatController: RealtimeChatController){
        val userQuestion = "你表述一下现在看到的场景。"

        val dataMap = mapOf(
            RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.USER_TEXT_MESSAGE.type,
            RealtimeRequestDataTypeEnum.DATA to userQuestion
        )
        realtimeChatController.realtimeChatWsClient?.sendMessage(
            dataMap,
            true
        )
    }

    //-----------------------Logic-----------------------

    var realtimeChatController: RealtimeChatController? = null

    override fun onCleared() {
        super.onCleared()

        disconnectService()
    }
}