package com.magicvector.viewModel.fragment

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.core.baseutil.cache.HttpRequestManager
import com.core.baseutil.network.networkLoad.NetworkLoadUtils
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.permissions.PermissionUtil
import com.core.baseutil.ui.ToastUtils
import com.data.domain.OnPositionItemClick
import com.magicvector.domain.dto.http.response.AgentLastChatListResponse
import com.data.domain.fragmentActivity.fao.MessageFAo
import com.magicvector.MainApplication
import com.view.appview.message.MessageContactAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.Runnable


open class MessageListVm(
) : ViewModel(){

    companion object {
        val TAG: String = MessageListVm::class.java.name
    }

    fun initResource(activity: FragmentActivity){

        NetworkLoadUtils.showDialog(activity)
        viewModelScope.launch {
            runCatching { initNetworkRequest(activity) }
                .onFailure { Log.e(TAG, "initResource: onThrowable", it) }
            NetworkLoadUtils.dismissDialogSafety(activity)
        }
    }

    //---------------------------FAo Ld---------------------------

    lateinit var adapter : MessageContactAdapter

    val fao = MessageFAo()

    fun initFAo(){
        // 后续缓存的数据会加载到此处
    }

    fun initAdapter(onPositionItemClick : OnPositionItemClick){
        adapter = MessageContactAdapter(
            MainApplication.getMessageListManager().messageContactItemModels,
            onPositionItemClick
        )
    }

    //---------------------------NetWork---------------------------

    suspend fun initNetworkRequest(context: Context){
        if (HttpRequestManager.getIsFirstOpen(TAG)){
            // 第一次打开，初始化
            Log.i(TAG, "initNetworkRequest: 第一次打开")
            fetchLastAgentChatList()
        }
        else {
            Log.i(TAG, "initNetworkRequest: 不是第一次打开")
            NetworkLoadUtils.dismissDialogSafety(context)
            val messageContactItemAos = MainApplication.getMessageListManager().messageContactItemModels
            fao.messageContactCountLd.postValue(messageContactItemAos.size)
        }
    }

    suspend fun fetchLastAgentChatList() {
        val response = MainApplication.getRemoteApiSource()
            .getLastAgentChatList(MainApplication.getUserId())
        applyLastAgentChatList(response)
    }

    private fun applyLastAgentChatList(response: AgentLastChatListResponse?) {
        if (response != null){
            MainApplication.getMessageListManager().setAgentChatAos(response)
            fao.messageContactCountLd.postValue(
                MainApplication.getMessageListManager().messageContactItemModels.size
            )
        }
        else {
            MainApplication.getMessageListManager().clear()
            fao.messageContactCountLd.postValue(0)
        }
    }

    //---------------------------Logic---------------------------

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