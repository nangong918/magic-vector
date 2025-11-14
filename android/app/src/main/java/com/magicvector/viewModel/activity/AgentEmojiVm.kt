package com.magicvector.viewModel.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import com.core.appcore.api.handler.SyncRequestCallback
import com.core.appcore.utils.AppResponseUtil
import com.core.baseutil.file.FileUtil
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.magicvector.MainApplication
import com.magicvector.manager.ChatMessageHandler
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class AgentEmojiVm(

) : ViewModel(){

    companion object {
        val TAG: String = AgentEmojiVm::class.java.name
        val GSON = MainApplication.GSON
        val mApi = MainApplication.getApiRequestImplInstance()
    }

    /**
     * vision任务，http方式上传single图片
     * @param context       上下文
     * @param image         图片文件
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
    fun visionTest(chatMessageHandler: ChatMessageHandler){
        val userQuestion = "你表述一下现在看到的场景。"

        val dataMap = mapOf(
            RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.USER_TEXT_MESSAGE.type,
            RealtimeRequestDataTypeEnum.DATA to userQuestion
        )
        chatMessageHandler.realtimeChatWsClient?.sendMessage(
            dataMap,
            true
        )
    }
}