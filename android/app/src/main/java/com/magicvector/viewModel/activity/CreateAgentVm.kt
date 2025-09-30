package com.magicvector.viewModel.activity

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import com.core.appcore.api.handler.SyncRequestCallback
import com.core.appcore.utils.AppResponseUtil
import com.core.baseutil.file.FileUtil
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.constant.BaseConstant
import com.data.domain.dto.response.AgentResponse
import com.data.domain.fragmentActivity.aao.CreateAgentAAo
import com.magicvector.MainApplication
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class CreateAgentVm(

) : ViewModel(){

    companion object {
        val TAG: String = CreateAgentVm::class.java.name
    }

    //---------------------AAo---------------------

    val aao = CreateAgentAAo()

    //---------------------Network---------------------

    val api = MainApplication.getApiRequestImplInstance()

    // 创建Agent
    fun doCreateAgent(context: Context, callback: SyncRequestCallback){
        var bitmap: Bitmap? = MainApplication.getImageManager()!!.
        uriToBitmapMediaStore(context, aao.atomicUrl.get())
        bitmap = MainApplication.getImageManager()!!.
        processImage(bitmap, BaseConstant.Constant.BITMAP_MAX_SIZE_AVATAR)

        // Http Send
        var imageFile: File? = null
        // 确保您在这里传入正确的 Uri
//        Uri uri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), imageName));
        imageFile = MainApplication.getImageManager()!!.
        bitmapToFile(bitmap, aao.atomicUrl.get(), context)

        if (imageFile == null || !imageFile.exists()) {
            // 处理文件未创建或路径不正确的情况
            Log.e(TAG, "Image file creation failed")
            return
        }

        // 获取文件名
        // 使用 getName() 获取文件名
        val originalFilename = imageFile.name
        val filePart: MultipartBody.Part = FileUtil.createMultipartBodyPart(imageFile, "img")

        val nameBody = RequestBody.create(
            "text/plain".toMediaTypeOrNull(),
            aao.nameLd.value!!
        )
        val descriptionBody = RequestBody.create(
            "text/plain".toMediaTypeOrNull(),
            aao.descriptionLd.value!!
        )

        api.createAgent(
            filePart,
            nameBody,
            descriptionBody,
            object : OnSuccessCallback<BaseResponse<AgentResponse>> {
                override fun onResponse(response: BaseResponse<AgentResponse>?) {
                    AppResponseUtil.handleSyncResponseEx(
                        response,
                        context,
                        callback,
                        ::handleCreateAgent
                    )
                }
            },
            object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    callback(throwable)
                }
            }
        )
    }

    private fun handleCreateAgent(response: BaseResponse<AgentResponse>?,
                                  context: Context,
                                  callback: SyncRequestCallback) {

    }

    // 查询Agent
    fun doGetAgentInfo(context: Context, agentId: String, callback: SyncRequestCallback){
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
                    callback(throwable)
                }
            }
        )
    }

    private fun handleGetAgentInfo(response: BaseResponse<AgentResponse>?,
                                  context: Context,
                                  callback: SyncRequestCallback) {

    }

    //---------------------Logic---------------------

}