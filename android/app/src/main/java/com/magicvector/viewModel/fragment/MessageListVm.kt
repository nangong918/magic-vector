package com.magicvector.viewModel.fragment

import android.Manifest
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.core.appcore.api.handler.SyncRequestCallback
import com.core.appcore.utils.AppResponseUtil
import com.core.baseutil.cache.HttpRequestManager
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.core.baseutil.network.networkLoad.NetworkLoadUtils
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.permissions.PermissionUtil
import com.core.baseutil.ui.ToastUtils
import com.data.domain.OnPositionItemClick
import com.data.domain.dto.response.AgentLastChatListResponse
import com.data.domain.fragmentActivity.fao.MessageFAo
import com.magicvector.MainApplication
import com.magicvector.activity.CreateAgentActivity
import com.view.appview.message.MessageContactAdapter
import kotlinx.coroutines.Runnable


open class MessageListVm(
) : ViewModel(){

    companion object {
        val TAG: String = MessageListVm::class.java.name
    }

    fun initResource(activity: FragmentActivity){
        initCreateAgentLuncher(activity)

        NetworkLoadUtils.showDialog(activity)
        initNetworkRequest(activity, object : SyncRequestCallback {
            override fun onThrowable(throwable: Throwable?) {
                NetworkLoadUtils.dismissDialogSafety(activity)
                Log.e(TAG, "initResource: onThrowable", throwable)
            }

            override fun onAllRequestSuccess() {
                NetworkLoadUtils.dismissDialogSafety(activity)
            }
        })
    }

    //---------------------------FAo Ld---------------------------

    lateinit var adapter : MessageContactAdapter

    val fao = MessageFAo()

    fun initFAo(){
        // 后续缓存的数据会加载到此处
    }

    fun initAdapter(onPositionItemClick : OnPositionItemClick){
        adapter = MessageContactAdapter(
            MainApplication.getMessageListManager().messageContactItemAos,
            onPositionItemClick
        )
    }

    //---------------------------NetWork---------------------------

    fun initNetworkRequest(context: Context, callback: SyncRequestCallback){
        if (HttpRequestManager.getIsFirstOpen(TAG)){
            // 第一次打开，初始化
            doGetLastAgentChatList(context, callback)
        }
        else {
            val messageContactItemAos = MainApplication.getMessageListManager().messageContactItemAos
            fao.messageContactCountLd.postValue(messageContactItemAos.size)
        }
    }

    private fun doGetLastAgentChatList(context: Context, callback: SyncRequestCallback){
        MainApplication.getApiRequestImplInstance().getLastAgentChatList(
            MainApplication.getUserId(),
            object : OnSuccessCallback<BaseResponse<AgentLastChatListResponse>>{
                override fun onResponse(response: BaseResponse<AgentLastChatListResponse>?) {
                    AppResponseUtil.handleSyncResponseEx(
                        response,
                        context,
                        callback,
                        ::handleGetLastAgentChatList
                    )
                }

            },
            object : OnThrowableCallback{
                override fun callback(throwable: Throwable?) {
                    callback(throwable)
                }
            }
        )
    }

    private fun handleGetLastAgentChatList(response: BaseResponse<AgentLastChatListResponse>?,
                                           context: Context,
                                           callback: SyncRequestCallback){
        if (response?.data != null){
            MainApplication.getMessageListManager().setAgentChatAos(response.data!!)
            fao.messageContactCountLd.postValue(
                MainApplication.getMessageListManager().messageContactItemAos.size
            )
        }
        else {
            MainApplication.getMessageListManager().clear()
            fao.messageContactCountLd.postValue(0)
        }
        callback.onAllRequestSuccess()
    }

    //---------------------------Logic---------------------------

    var createAgentLauncher: ActivityResultLauncher<Intent>? = null

    fun turnToCreateAgent(activity: FragmentActivity) {
        val intent = Intent(activity, CreateAgentActivity::class.java)
        createAgentLauncher?.launch(intent)
    }

    fun initCreateAgentLuncher(activity: FragmentActivity){
        createAgentLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // 如果intent 返回值中包括ok，则表明创建成功，需要进行刷新list
            val backIntent: Intent? = result.data
            if (backIntent != null) {
                val createResult: Boolean = backIntent.getBooleanExtra(
                    CreateAgentActivity::class.simpleName,
                    false
                )
                // 创建成功
                if (createResult) {
                    doGetLastAgentChatList(activity, object : SyncRequestCallback {
                        override fun onThrowable(throwable: Throwable?) {
                            NetworkLoadUtils.dismissDialogSafety(activity)
                            Log.e(TAG, "initResource: onThrowable", throwable)
                        }

                        override fun onAllRequestSuccess() {
                            NetworkLoadUtils.dismissDialogSafety(activity)
                        }
                    })
                }
            }
        }
    }

    fun startChatActivity(activity: FragmentActivity, successRunnable: Runnable) {
        PermissionUtil.requestPermissionSelectX(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            object : GainPermissionCallback{
                @RequiresPermission(Manifest.permission.RECORD_AUDIO)
                override fun allGranted() {
                    Log.i(TAG, "获取录音权限成功")
                    successRunnable.run()
                }

                override fun notGranted(notGrantedPermissions: Array<String?>?) {
                    Log.w(TAG, "没有获取录音权限: ${notGrantedPermissions?.contentToString()}")
                    ToastUtils.showToastActivity(activity, "没有获取录音权限")
                }

                override fun always() {
                }

            }
        )
    }
}