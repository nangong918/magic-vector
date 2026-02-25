package com.magicvector.viewModel.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.core.appcore.api.handler.SyncRequestCallback
import com.core.appcore.utils.AppResponseUtil
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.permissions.PermissionUtil
import com.core.baseutil.photo.SelectPhotoUtil
import com.core.baseutil.ui.ToastUtils
import com.data.domain.dto.response.AgentResponse
import com.data.domain.fragmentActivity.aao.AgentInfoAAo
import com.magicvector.MainApplication

class AgentInfoVm(

): ViewModel() {

    companion object {
        val TAG: String = AgentInfoVm::class.java.name
    }

    fun initResource(activity: FragmentActivity, imvgAvatar: ImageView){
        initPictureSelectLauncher(activity, imvgAvatar)
    }

    //---------------------AAo---------------------

    val aao = AgentInfoAAo()

    //---------------------Network---------------------

    val api = MainApplication.getApiRequestImplInstance()

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
                    callback.onThrowable(throwable)
                }
            }
        )
    }

    private fun handleGetAgentInfo(response: BaseResponse<AgentResponse>?,
                                   context: Context,
                                   callback: SyncRequestCallback) {
        response?.data?.agentAo?.let { ao ->

            ao.agentVo?.let { vo ->
                aao.avatarUrlLd.postValue(vo.avatarUrl)
                aao.nameLd.postValue(vo.name)
                aao.descriptionLd.postValue(vo.description)
            }
        }
        callback.onAllRequestSuccess()
    }

    //---------------------Logic---------------------

    var selectImageLauncher: ActivityResultLauncher<Intent>? = null

    fun selectAgentAvatar(activity: FragmentActivity){
        PermissionUtil.requestPermissionSelectX(activity,
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
            ),
            arrayOf(
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            object : GainPermissionCallback{
                override fun allGranted() {
                    selectImageLauncher?.let {
                        SelectPhotoUtil.selectImageFromAlbum(it)
                    }
                }

                override fun notGranted(notGrantedPermissions: Array<String?>?) {
                    ToastUtils.showToastActivity(activity, activity.getString(com.view.appview.R.string.please_give_permission))
                    Log.w(TAG, "Permission not granted: $notGrantedPermissions")
                }

                override fun always() {
                }

            }
        )
    }

    fun initPictureSelectLauncher(activity: FragmentActivity, imvgAvatar: ImageView){
        selectImageLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data: Intent? = result.data
            if (data != null) {
                val imageUri = data.data
                aao.avatarAtomicUri.set(imageUri)
                val bitmap: Bitmap? = MainApplication.getImageManager()
                    ?.uriToBitmapMediaStore(activity, imageUri)
                if (bitmap != null) {
                    imvgAvatar.setImageBitmap(bitmap)
                }
            }
        }
    }
}