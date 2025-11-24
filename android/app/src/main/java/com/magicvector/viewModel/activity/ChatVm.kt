package com.magicvector.viewModel.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import com.core.appcore.api.handler.SyncRequestCallback
import com.core.appcore.utils.AppResponseUtil
import com.core.baseutil.cache.HttpRequestManager
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.core.baseutil.network.networkLoad.NetworkLoadUtils
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.permissions.PermissionUtil
import com.core.baseutil.photo.SelectPhotoUtil
import com.core.baseutil.ui.ToastUtils
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.constant.BaseConstant
import com.data.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.data.domain.dto.response.ChatMessageResponse
import com.data.domain.fragmentActivity.aao.ChatAAo
import com.magicvector.MainApplication
import com.magicvector.callback.OnVadChatStateChange
import com.magicvector.callback.OnReceiveAgentTextCallback
import com.magicvector.manager.RealtimeChatController
import com.magicvector.service.ChatService
import com.view.appview.R
import com.view.appview.call.CallAo
import com.view.appview.recycler.RecyclerViewWhereNeedUpdate
import com.view.appview.chat.ChatMessageAdapter
import com.view.appview.chat.OnChatMessageClick


/**
 * 录音：
 *  RealTime
 *      只有手动按住录制和发送语音才能录音，不用关心其stop和start生命周期
 *  VAD
 *      一旦是VADCall状态之后就需要启动，关闭VADCall之后一定要关闭
 * 播放：
 *  公用AudioTrack
 *      接收到TTS_START之后就要启动
 *      接收到TTS_STOP之后就要关闭
 */
class ChatVm(
) : AndroidViewModel(application = MainApplication.getApp()){

    companion object {
        val TAG: String = ChatVm::class.java.name
        val GSON = MainApplication.GSON
        val mainHandler: Handler = Handler(Looper.getMainLooper())
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

            Log.w(AgentEmojiVm.Companion.TAG, "Service binding died - process was killed")
            chatServiceBoundLd.postValue(false)

            // 需要重新绑定
            // rebindService() // 可以在这里自动重试
        }

        override fun onNullBinding(name: ComponentName?) {
            super.onNullBinding(name)
            // Service 的 onBind() 返回了 null
            Log.e(AgentEmojiVm.Companion.TAG, "Service returned null binding - check Service implementation")
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
                    Log.d(AgentEmojiVm.Companion.TAG, "Service disconnected successfully")
                } catch (e: Exception) {
                    Log.e(AgentEmojiVm.Companion.TAG, "Error disconnecting service", e)
                }
            }
        }
        chatServiceBoundLd.postValue(false)
        chatServiceBinder = null
        realtimeChatController = null
    }

    fun initResource(
        activity: FragmentActivity,
        ao : MessageContactItemAo?,
        whereNeedUpdate: RecyclerViewWhereNeedUpdate,
        onReceiveAgentTextCallback: OnReceiveAgentTextCallback,
        onVadChatStateChange: OnVadChatStateChange
    ) {
        // 初始化网络请求
        val initNetworkRunnable = {
            NetworkLoadUtils.showDialog(activity)
            initNetworkRequest(activity, object : SyncRequestCallback {
                override fun onThrowable(throwable: Throwable?) {
                    NetworkLoadUtils.dismissDialogSafety(activity)
                }

                override fun onAllRequestSuccess() {
                    NetworkLoadUtils.dismissDialogSafety(activity)
                }
            })
        }

        messageAo = ao
        Log.i("ChatMessageHandler", "ao1: ${GSON.toJson(ao)}")
        // 初始化ChatMessageHandler
        realtimeChatController?.initResource(
            chatActivity = activity,
            ao = messageAo,
            chatAAo = aao,
            initNetworkRunnable = initNetworkRunnable,
            whereNeedUpdate = whereNeedUpdate,
            onReceiveAgentTextCallback = onReceiveAgentTextCallback,
            onVadChatStateChange = onVadChatStateChange
        )

    }

    //---------------------------AAo Ld---------------------------

    val aao = ChatAAo()
    var messageAo: MessageContactItemAo? = null

    lateinit var adapter : ChatMessageAdapter

    fun initAdapter(onChatMessageClick : OnChatMessageClick){
        adapter = ChatMessageAdapter(
            // 这里是初始化，要是chatManagerPointer == null直接报错吧
            realtimeChatController!!.getChatManagerPointer().getViewChatMessageList(),
            onChatMessageClick
        )
    }

    //---------------------------NetWork---------------------------

    // chat
    private fun initNetworkRequest(context: Context, callback: SyncRequestCallback){
        if (HttpRequestManager.getIsFirstOpen(TAG)){
            // 第一次打开，初始化
            Log.i(TAG, "initNetworkRequest: 第一次打开")
            doGetLastChat(context, callback)
        }
        else {
            Log.i(TAG, "initNetworkRequest: 重启viewModel了")
            // 重启viewModel了，全部更新
            mainHandler.post {
                @SuppressLint("NotifyDataSetChanged")
                adapter.notifyDataSetChanged()
                NetworkLoadUtils.dismissDialogSafety(context)
            }
        }
    }

    // chatHistory First
    fun doGetLastChat(context: Context, callback: SyncRequestCallback){
        if (realtimeChatController?.messageContactItemAo != null) {
            MainApplication.getApiRequestImplInstance().getLastChat(
                realtimeChatController?.messageContactItemAo!!.contactId!!,
                object : OnSuccessCallback<BaseResponse<ChatMessageResponse>>{
                    override fun onResponse(response: BaseResponse<ChatMessageResponse>?) {
                        AppResponseUtil.handleSyncResponseEx(
                            response,
                            context,
                            callback,
                            ::handleGetChatHistory
                        )
                    }

                },
                object : OnThrowableCallback{
                    override fun callback(throwable: Throwable?) {
                        Log.e(TAG, "doGetLastChat: onThrowable", throwable)
                        callback.onThrowable(Throwable("Get ChatHistory Failed"))
                    }
                }
            )
        }
        else {
            Log.w(TAG, "doGetLastChat: messageContactItemAo == null")
            ToastUtils.showToastActivity(context, "获取聊天记录失败")
            val throwable = Throwable("Get ChatHistory Failed")
            callback.onThrowable(throwable)
        }
    }

    // 特定时间段的chat history todo: 上拉上滑获取之前的chat History
    fun doGetTimeLimitChat(context: Context, deadline: String, callback: SyncRequestCallback){
        if (realtimeChatController?.messageContactItemAo != null){
            MainApplication.getApiRequestImplInstance().getTimeLimitChat(
                realtimeChatController?.messageContactItemAo!!.contactId!!,
                deadline,
                BaseConstant.Constant.CHAT_HISTORY_LIMIT_COUNT,
                object : OnSuccessCallback<BaseResponse<ChatMessageResponse>>{
                    override fun onResponse(response: BaseResponse<ChatMessageResponse>?) {
                        AppResponseUtil.handleSyncResponseEx(
                            response,
                            context,
                            callback,
                            ::handleGetChatHistory
                        )
                    }

                },
                object : OnThrowableCallback{
                    override fun callback(throwable: Throwable?) {
                        Log.e(TAG, "doGetTimeLimitChat: onThrowable", throwable)
                        callback.onThrowable(Throwable("doGetTimeLimitChat: onThrowable"))
                    }
                }
            )
        }
        else {
            Log.w(TAG, "doGetTimeLimitChat: messageContactItemAo == null")
            ToastUtils.showToastActivity(context, "获取聊天记录失败")
            callback.onThrowable(Throwable("doGetTimeLimitChat: onThrowable"))
        }
    }

    private fun handleGetChatHistory(response: BaseResponse<ChatMessageResponse>?,
                                     context: Context,
                                     callback: SyncRequestCallback){

        response?.data?.chatMessages?.let {
            realtimeChatController?.getChatManagerPointer()?.setResponsesToViews(it)
            realtimeChatController?.updateMessage()
        }

        callback.onAllRequestSuccess()
    }

    //---------------------------Logic---------------------------

    // text message

    fun getTextWatcher(): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.isNotEmpty() == true) {
                    aao.inputTextLd.value = s.toString()
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
    }

    fun sendMessage(context: Context) {
        val inputText = aao.inputTextLd.value
        val isAllWhitespaceOrSpecialChars = inputText?.all { it.isWhitespace() || !it.isLetterOrDigit() }

        if (inputText == null || inputText.isEmpty() || isAllWhitespaceOrSpecialChars == true){
            // 请输入合法的内容
            ToastUtils.showToastActivity(context, context.getString(R.string.please_input_legal_content))
            return
        }

        val dataMap = mapOf(
            RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.USER_TEXT_MESSAGE.type,
            RealtimeRequestDataTypeEnum.DATA to inputText
        )
        realtimeChatController?.realtimeChatWsClient!!.sendMessage(dataMap, true)
        // 发送的时候不用回显，因为此时还没拿到后端的messageId
    }

    //===========selectImage

    private var selectImageLauncher: ActivityResultLauncher<Intent>? = null

    // 图片消息：3.初始化图片选择器
    fun initPictureSelectorLauncher(activity: FragmentActivity){
        selectImageLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ){
            result ->
            // 图片消息：3.1 获取图片uri
            val imageUri: Uri? = result.data?.data


            if (imageUri != null) {
                val currentTime = System.currentTimeMillis()
                val content = aao.inputTextLd.value

                // todo 发送图片
            }
            else {
                ToastUtils.showToastActivity(
                    activity,
                    activity.getString(R.string.send_image_failed)
                )
            }
        }
    }

    // 选择图片
    fun beginSelectPicture(activity: FragmentActivity){
        val mustPermissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
        )
        val optionalPermissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )

        PermissionUtil.requestPermissionSelectX(
            activity,
            mustPermissions,
            optionalPermissions,
            object : GainPermissionCallback {
                override fun allGranted() {
                    // 必要权限获取成功
                    if (selectImageLauncher != null){
                        SelectPhotoUtil.selectImageFromAlbum(selectImageLauncher!!);
                    }
                }

                override fun notGranted(notGrantedPermissions: Array<String?>?) {
                    // 必要权限被拒绝
                    ToastUtils.showToastActivity(
                        activity,
                        activity.getString(R.string.gain_permission_failed)
                    )
                }

                override fun always() {

                }

            }
        )
    }


    // 语音通话
    fun getCallAo(onMuteClickRunnable: Runnable?, onCallEndClickRunnable: Runnable?): CallAo{
        val callAo = CallAo()
        callAo.agentName = aao.nameLd.value
        callAo.agentAvatar = aao.avatarUrlLd.value
        callAo.agentId = realtimeChatController?.messageContactItemAo?.contactId?:""

        callAo.onMuteClickRunnable = onMuteClickRunnable
        callAo.onCallEndClickRunnable = onCallEndClickRunnable

        return callAo
    }



    //-----------------------Logic-----------------------

    var realtimeChatController: RealtimeChatController? = null

    override fun onCleared() {
        super.onCleared()

//        chatMessageHandler?.destroy()
        disconnectService()
    }
}