package com.magicvector.manager

import android.Manifest
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import com.core.appcore.api.ApiUrlConfig
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.permissions.PermissionUtil
import com.core.baseutil.ui.ToastUtils
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.ao.mixLLM.McpSwitch
import com.data.domain.ao.mixLLM.MixLLMEvent
import com.data.domain.constant.BaseConstant
import com.data.domain.constant.VadChatState
import com.data.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.data.domain.constant.chat.RealtimeResponseDataTypeEnum
import com.data.domain.constant.chat.RealtimeSystemResponseEventEnum
import com.data.domain.fragmentActivity.aao.ChatAAo
import com.data.domain.vo.test.RealtimeChatState
import com.google.gson.reflect.TypeToken
import com.magicvector.MainApplication
import com.magicvector.callback.OnVadChatStateChange
import com.magicvector.callback.OnReceiveAgentTextCallback
import com.magicvector.manager.audio.AudioController
import com.magicvector.manager.audio.AudioHandleCallback
import com.magicvector.manager.audio.IsAudioRecording
import com.magicvector.manager.mcp.HandleSystemResponse
import com.magicvector.manager.audio.vad.VadDetectionCallback
import com.magicvector.manager.ws.ChatWsTextMessageHandler
import com.magicvector.manager.ws.WsManager
import com.magicvector.utils.chat.RealtimeChatWsClient
import com.view.appview.recycler.RecyclerViewWhereNeedUpdate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Runnable
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt


/**
 * ChatMessageHandler管理：
 * WS长连接
 * ChatMessage处理，SystemMessage处理
 * 音频处理：AudioRecord，AudioTrack
 * VAD语音活动检测
 * UdpVision
 * todo 解耦chatMessageHandler，绘制UML图，多数据源合并问题。（具体需要先实现AppDemo中的Jetpack Compose的多数据源插入LazyColumn）
 */
class RealtimeChatController : IsAudioRecording{

    companion object {
        const val TAG = "RealtimeChatController"
        val GSON = MainApplication.GSON
        val mainHandler: Handler = Handler(Looper.getMainLooper())
    }

    //---------------------------Data---------------------------

    /// 状态管理
    // 当前是否是表情页面
    val currentIsEmoji: AtomicBoolean = AtomicBoolean(false)
    // 当前是否在通话中（仅仅用于chatActivity中的Call；至于是null是因为这个是指针，对象直接从CallDialog中获取）
    var isChatCalling: AtomicBoolean? = null
    var recyclerViewWhereNeedUpdate: RecyclerViewWhereNeedUpdate? = null
    var onReceiveAgentTextCallback: OnReceiveAgentTextCallback? = null
    var onVadChatStateChange: OnVadChatStateChange? = null
    val realtimeChatState: MutableLiveData<RealtimeChatState> = MutableLiveData(RealtimeChatState.NotInitialized)
//    val realtimeChatVolume = MutableLiveData(0f)

    fun setCurrentVADStateChange(callback: OnVadChatStateChange) {
        onVadChatStateChange = callback
    }

    // 设置isChatCalling指针
    fun initIsChatCalling(isCalling: AtomicBoolean) {
        isChatCalling = isCalling
    }
    
    // 数据
    var messageContactItemAo : MessageContactItemAo? = null

    //---------------------------Network / Mapper---------------------------

    var realtimeChatWsClient: RealtimeChatWsClient? = null // 长连接，可为null，允许销毁

    private fun initRealtimeChatWsClient(): RealtimeChatWsClient {
        return realtimeChatWsClient ?: synchronized(this) {
            realtimeChatWsClient ?: RealtimeChatWsClient(
                GSON,
                ApiUrlConfig.getWsMainUrl() + BaseConstant.WSConstantUrl.AGENT_REALTIME_CHAT_URL
            ).also { realtimeChatWsClient = it }
        }
    }

    // ws
    fun initRealtimeChatWsClient(activity: FragmentActivity) {
        // 初始化
        realtimeChatWsClient = initRealtimeChatWsClient()

        PermissionUtil.requestPermissionSelectX(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            object : GainPermissionCallback{
                @RequiresPermission(Manifest.permission.RECORD_AUDIO)
                override fun allGranted() {
                    Log.i(TAG, "获取录音权限成功")

                    // 初始化AudioController
                    initAudioController()

                    audioController?.initAudioRecorderAndPlayer() ?: run {
                        Log.w(TAG, "初始化AudioRecorderAndPlayer失败")
                    }

                    startRealtimeWs()
                }

                override fun notGranted(notGrantedPermissions: Array<String?>?) {
                    Log.w(TAG, "没有获取录音权限: ${notGrantedPermissions?.contentToString()}")
                    ToastUtils.showToastActivity(activity, "没有获取录音权限")
                    realtimeChatState.postValue(RealtimeChatState.Error("没有获取录音权限"))
                }

                override fun always() {
                }
            }
        )
    }

    private fun startRealtimeWs() {
        realtimeChatWsClient?.let { client ->
            client.start(
                object : WebSocketListener() {
                    override fun onClosed(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String
                    ) {
                        super.onClosed(webSocket, code, reason)
                        realtimeChatState.postValue(RealtimeChatState.Disconnected)
                        Log.i(TAG, "realtimeChatWsClient::onClosed")
                    }

                    override fun onClosing(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String
                    ) {
                        super.onClosing(webSocket, code, reason)
                        Log.i(TAG, "realtimeChatWsClient::onClosing")
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?
                    ) {
                        super.onFailure(webSocket, t, response)
                        Log.e(TAG, "realtimeChatWsClient::onFailure: ${t.message}")
                        realtimeChatState.postValue(RealtimeChatState.Error(t.message ?: "-"))
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        super.onMessage(webSocket, text)
                        // 处理text
                        realtimeChatState.postValue(RealtimeChatState.Receiving)
                        handleTextMessage(text)
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        super.onMessage(webSocket, bytes)
                        realtimeChatState.postValue(RealtimeChatState.Receiving)
                        // 处理字节信息
                        Log.i(TAG, "收到字节信息::长度: ${bytes.size}")
                    }

                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        super.onOpen(webSocket, response)
                        // 到了此处说明: 授权 && 连接成功
                        realtimeChatState.postValue(RealtimeChatState.InitializedConnected)
                        Log.i(TAG, "realtimeChatWsClient::onOpen; response: $response")

                        val agentId = messageContactItemAo?.contactId

                        if (agentId == null || agentId.isEmpty()){
                            Log.e(TAG, "onOpen::agentId为空")
                            return
                        }

                        // 发送连接成功的消息
                        WsManager.sendOnOpenInfo(
                            agentId = agentId,
                            userId = MainApplication.getUserId(),
                            wsClient = client
                        )
                    }
                }
            )
        } ?: run {
            Log.e(TAG, "startRealtimeWs::realtimeChatWsClient is null")
        }
    }

    //---------------------------Logic---------------------------

    // 音频处理回调
    fun getAudioHandleCallback(): AudioHandleCallback{
        return object : AudioHandleCallback{
            override fun onPlayBase64Audio(base64Audio: String) {
                // 内部播放了，此粗只需要改变状态
                onVadChatStateChange?.onChange(VadChatState.Replying)
            }

            override fun onStartRecording() {
                // 发送启动录音
                val dataMap = mapOf(
                    RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.START_AUDIO_RECORD.type,
                    RealtimeRequestDataTypeEnum.DATA to RealtimeRequestDataTypeEnum.START_AUDIO_RECORD.name
                )
                realtimeChatWsClient?.sendMessage(dataMap)

                // 录制状态
                realtimeChatState.value = RealtimeChatState.RecordingAndSending
            }

            override fun onObtainAudio(base64Audio: String) {
                val dataMap = mapOf(
                    RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.AUDIO_CHUNK.type,
                    RealtimeRequestDataTypeEnum.DATA to base64Audio
                )
                realtimeChatWsClient!!.sendMessage(dataMap)
            }

            override fun onStopRecording() {
                // 发送结束录音
                val dataMap = mapOf(
                    RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.type,
                    RealtimeRequestDataTypeEnum.DATA to RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.name
                )
                realtimeChatWsClient!!.sendMessage(dataMap)
            }
        }
    }

    // vad识别结果回调
    fun getVadDetectionCallback(): VadDetectionCallback{
        return object : VadDetectionCallback{
            override fun onStartSpeech(audioBuffer: ByteArray) {
                // 发送启动录音
                val dataMap = mapOf(
                    RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.START_AUDIO_RECORD.type,
                    RealtimeRequestDataTypeEnum.DATA to RealtimeRequestDataTypeEnum.START_AUDIO_RECORD.name
                )
                realtimeChatWsClient?.sendMessage(dataMap)

                // 发送初次启动的数据
                if (audioBuffer.isNotEmpty()) {
                    val base64Audio = Base64.encodeToString(audioBuffer, 0, audioBuffer.size, Base64.NO_WRAP)
                    val dataMap = mapOf(
                        RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.AUDIO_CHUNK.type,
                        RealtimeRequestDataTypeEnum.DATA to base64Audio
                    )
                    realtimeChatWsClient!!.sendMessage(dataMap)

                    onVadChatStateChange?.onChange(VadChatState.Speaking)
                }
            }

            override fun speeching(audioBuffer: ByteArray) {
                // 直接发送数据
                if (audioBuffer.isNotEmpty()) {
                    val base64Audio = Base64.encodeToString(audioBuffer, 0, audioBuffer.size, Base64.NO_WRAP)
                    val dataMap = mapOf(
                        RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.AUDIO_CHUNK.type,
                        RealtimeRequestDataTypeEnum.DATA to base64Audio
                    )
                    realtimeChatWsClient!!.sendMessage(dataMap)

                    onVadChatStateChange?.onChange(VadChatState.Speaking)
                }
            }

            override fun onStopSpeech() {
                val dataMap = mapOf(
                    RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.type,
                    RealtimeRequestDataTypeEnum.DATA to RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.name
                )
                realtimeChatWsClient!!.sendMessage(dataMap)

                onVadChatStateChange?.onChange(VadChatState.Silent)
            }
        }
    }

    var audioController : AudioController? = null

    private fun initAudioController(){
        if (audioController == null) {

            val audioHandleCallback = getAudioHandleCallback()
            val vadDetectionCallback = getVadDetectionCallback()

            audioController = AudioController(
                audioHandleCallback = audioHandleCallback,
                vadDetectionCallback = vadDetectionCallback
            )
        }
    }

    override fun isAudioRecording(): Boolean {
        return realtimeChatState.value == RealtimeChatState.RecordingAndSending
    }

    //===========音频

    // 按下录制音频
    fun startRecordRealtimeChatAudio(weakScope: WeakReference<CoroutineScope>) {
        val scope = weakScope.get() ?: return

        audioController?.startRecordingAudio(isAudioRecording = this, scope)
    }

    // 松手发送音频
    fun stopAndSendRealtimeChatAudio(){
        realtimeChatState.postValue(RealtimeChatState.InitializedConnected)
    }

    // 计算音量（RMS - 均方根值，更准确）
    // RMS 计算（最准确）
    fun calculateRMSAmplitude(buffer: ByteArray, bytesRead: Int): Float {
        if (bytesRead < 2) return 0f

        var sumSquares = 0.0
        var sampleCount = 0

        for (i in 0 until bytesRead - 1 step 2) {
            // 假设小端序（Android 通常使用）
            val low = buffer[i].toInt() and 0xFF
            val high = buffer[i + 1].toInt() and 0xFF
            val sample = (high shl 8) or low
            val signedSample = if (sample > 32767) sample - 65536 else sample

            sumSquares += signedSample * signedSample
            sampleCount++
        }

        if (sampleCount == 0) return 0f

        val rms = sqrt(sumSquares / sampleCount)
        return minOf(1.0f, (rms / 32767.0).toFloat())
    }

    // 语音通话
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun initVadCall(weakContext: WeakReference<Context>){

        // 初始化VAD
        audioController?.initVadController(weakContext)

        startVadCall()
    }

    fun startVadCall(){
        audioController?.let { controller ->
            controller.startVAD(onStart = {
                onVadChatStateChange?.onChange(VadChatState.Silent)
                Log.i(TAG, "startVadCall")
            })
        } ?: run {
            Log.e(TAG, "startVadCall: audioController == null")
        }
    }

    fun stopVadCall(){
        audioController?.let { controller ->
            controller.stopVAD(onStop = {
                onVadChatStateChange?.onChange(VadChatState.Muted)
                Log.i(TAG, "stopVadCall")
            })
        } ?: run {
            Log.e(TAG, "stopVadCall: audioController == null")
        }
    }

    fun destroyVadCall(){
        audioController?.let { controller ->
            controller.releaseVADController()
            onVadChatStateChange?.onChange(VadChatState.Muted)
        }
    }

    //===========chatHistory

    private var chatControllerPointer: ChatController? = null
    fun getChatManagerPointer(): ChatController {
        return chatControllerPointer!!
    }

    fun initResource(
        chatActivity: FragmentActivity,
        ao : MessageContactItemAo?,
        chatAAo: ChatAAo,
        initNetworkRunnable: Runnable,
        whereNeedUpdate: RecyclerViewWhereNeedUpdate,
        onReceiveAgentTextCallback: OnReceiveAgentTextCallback,
        onVadChatStateChange: OnVadChatStateChange
    ) {
        // 在初始化之前先清理全部资源
//        try {
//            releaseAllResource()
//        } catch (e: Exception){
//            Log.e(TAG, "initResource: ${e.message}")
//        }

        realtimeChatState.postValue(RealtimeChatState.NotInitialized)

        this.recyclerViewWhereNeedUpdate = whereNeedUpdate
        this.onReceiveAgentTextCallback = onReceiveAgentTextCallback
        this.onVadChatStateChange = onVadChatStateChange

        messageContactItemAo = ao
        if (messageContactItemAo?.contactId != null){
            realtimeChatState.postValue(RealtimeChatState.Initializing)
            initRealtimeChatWsClient(chatActivity)
        }
        else {
            realtimeChatState.postValue(RealtimeChatState.Error("Agent Id is Null"))
            throw IllegalArgumentException("Agent Id is Null")
        }
        chatAAo.nameLd.postValue(ao?.vo?.name?: "")
        chatAAo.avatarUrlLd.postValue(ao?.vo?.avatarUrl?: "")

        // 获取chatManager
        chatControllerPointer = MainApplication.getChatMapManager().getChatManager(
            messageContactItemAo!!.contactId!!
        )
        // 初始化网络请求
        initNetworkRunnable.run()
    }

    // update Message
    fun updateMessage(){
        if (chatControllerPointer != null && recyclerViewWhereNeedUpdate != null){
            mainHandler.post {
                val updateList = chatControllerPointer!!.getNeedUpdateList()
                if (!updateList.isEmpty()){
                    recyclerViewWhereNeedUpdate!!.whereNeedUpdate(updateList)
                    Log.d(TAG, "handleGetChatHistory::待更新数据：${updateList.size} 条")
                }
                else {
                    Log.d(TAG, "handleGetChatHistory::没有待更新数据")
                }
            }
        }
        else {
            Log.w(TAG, "更新失败，chatManagerPointer：$chatControllerPointer, " +
                    "recyclerViewWhereNeedUpdate: $recyclerViewWhereNeedUpdate")
        }
    }

    //===========realtime chat

    private fun handleTextMessage(text: String){
        val chatWsTextMessageParseResult = WsManager.getTextMessageDataType(text = text)
        if (chatWsTextMessageParseResult == null) {
            return
        }

        val type = chatWsTextMessageParseResult.responseType
        val map = chatWsTextMessageParseResult.map

        when(type){
            // 开始TTS
            RealtimeResponseDataTypeEnum.START_TTS -> {
                Log.i(TAG, "handleTextMessage::START_TTS播放")
                // 开始接收数据
                realtimeChatState.postValue(RealtimeChatState.Receiving)
                onVadChatStateChange?.onChange(VadChatState.Replying)

                // 清空播放缓存 + 开始播放
                audioController?.startAudioTrackPlay()

                // VAD:设置AI回复的时候不能说话
                // 首先检查是不是Emoji状态
                if (currentIsEmoji.get()){
                    audioController?.stopVAD {
                        Log.d(TAG, "handleTextMessage:: 正在Emoji VAD通话:AI正在回复, 停止录音")
                    }
                }
                else if (isChatCalling?.get() == true) {
                    audioController?.stopVAD {
                        Log.d(TAG, "handleTextMessage:: 正在VAD通话:AI正在回复, 停止录音")
                    }
                }
                else {
                    Log.d(TAG, "START_TTS::handleTextMessage:: 停止VAD通话: 不管理VAD录音, isChatCalling: $isChatCalling")
                }
            }
            // 结束TTS + 结束会话
            RealtimeResponseDataTypeEnum.STOP_TTS -> {
                Log.i(TAG, "handleTextMessage::STOP_TTS播放")
                // 结束接收数据
                realtimeChatState.postValue(RealtimeChatState.InitializedConnected)
                onVadChatStateChange?.onChange(VadChatState.Silent)

                // 停止播放
                audioController?.stopAudioTrackPlay()

                // VAD:设置AI回复结束的时候可以说话
                if (currentIsEmoji.get()) {
                    audioController?.startVAD {
                        Log.d(TAG, "handleTextMessage:: 正在Emoji VAD通话:AI回复结束, 继续录音")
                    }
                }
                else if (isChatCalling?.get() == true){
                    audioController?.startVAD {
                        Log.d(TAG, "handleTextMessage:: 正在VAD通话:AI回复结束, 继续录音")
                    }
                }
                else {
                    Log.d(TAG, "STOP_TTS::handleTextMessage:: 停止VAD通话: 不管理VAD录音, isChatCalling: $isChatCalling")
                }
            }
            // 音频流
            RealtimeResponseDataTypeEnum.AUDIO_CHUNK -> {
                // 音频数据
                realtimeChatState.postValue(RealtimeChatState.Receiving)
                val data = map[RealtimeResponseDataTypeEnum.DATA]
                data?.let { it ->
                    audioController?.playBase64Audio(
                        base64Audio = it,
                        isShowLog = true
                    ) ?: run {
                        Log.w(TAG, "handleTextMessage::playBase64Audio: 播放音频失败")
                    }
                }
            }
            // 文本流
            RealtimeResponseDataTypeEnum.TEXT_CHAT_RESPONSE -> {
                // 文本数据
                realtimeChatState.postValue(RealtimeChatState.Receiving)
                val data = map[RealtimeResponseDataTypeEnum.DATA]
                if (data != null){
                    if (chatControllerPointer != null){
                        ChatWsTextMessageHandler.handleTextMessage(
                            message = data,
                            gson = GSON,
                            chatControllerPointer = chatControllerPointer!!,
                            onReceiveAgentTextCallback = this.onReceiveAgentTextCallback
                        )
                        updateMessage()
                    }
                    else {
                        Log.w(TAG, "handleTextMessage:更新文本数据异常 chatManagerPointer is null")
                    }
                }
                else {
                    Log.e(TAG, "handleTextMessage: data is null")
                }
            }
            // 文本整句
            RealtimeResponseDataTypeEnum.WHOLE_CHAT_RESPONSE -> {}
            // system响应
            RealtimeResponseDataTypeEnum.TEXT_SYSTEM_RESPONSE -> {
                val data = map[RealtimeResponseDataTypeEnum.DATA]
                data?.let {
                    handleSystemMessage(it)
                }
                /*
                    agentEmoji注册system事件回调。
                    接收system消息，handle system消息，分类处理mcp消息：MCP handler：VisionHandler，EmojiHandler。
                    找到要求拍照的消息，调用回调。
                    回调调用拍摄照片，转为base64，组成json加入message消息，传递给后端 -> 等待响应。
                 */
            }
            // 事件列表
            RealtimeResponseDataTypeEnum.EVENT_LIST -> {
                val data = map[RealtimeResponseDataTypeEnum.DATA]
                data?.let {
                    handleEventList(it)
                }
            }
        }
    }

    private var handleSystemResponse: HandleSystemResponse? = null
    fun setHandleSystemResponse(handleSystemResponse: HandleSystemResponse?) {
        this.handleSystemResponse = handleSystemResponse
    }

    private fun handleSystemMessage(data: String){
        if (data.isEmpty()){
            Log.w(TAG, "handleSystemMessage: data is empty")
            return
        }
        try {
            val map: Map<String, String> = GSON.fromJson(data, object : TypeToken<Map<String, String>>() {}.type)
            if (map[RealtimeSystemResponseEventEnum.EVENT_KET] == null) {
                Log.w(TAG, "handleSystemMessage: event is null")
                return
            }
            if (handleSystemResponse != null){
                Log.d(TAG, "handleSystemMessage: 正在处理系统消息:: system message: $data")
                handleSystemResponse!!.handleSystemResponse(map)
            }
            else {
                Log.w(TAG, "handleSystemMessage: 无法处理系统消息，因为：handleSystemResponse is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleSystemMessage: parse error", e)
        }
    }

    private fun handleEventList(eventListStr: String) {
        try {
            val eventListType = object : TypeToken<List<MixLLMEvent>>() {}.type
            val eventList: List<MixLLMEvent> = GSON.fromJson(eventListStr, eventListType)

            // 处理解析后的事件列表
            eventList.forEach { event ->
                // 根据需要处理每个 event 对象
                println("$TAG, Event Type: ${event.eventType}, Event Data: ${event.event}")
            }
        } catch (e: Exception){
            Log.e(TAG, "handleEventList: parse error", e)
        }
    }

    //===========sendMessage

    fun sendMcpSwitch(mcpSwitch: McpSwitch = MainApplication.getMcpSwitch()){
        val mcpSwitchJson = GSON.toJson(mcpSwitch)
        val dataMap = mapOf(
            RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.SYSTEM_MESSAGE.type,
            RealtimeRequestDataTypeEnum.DATA to mcpSwitchJson
        )
        realtimeChatWsClient!!.sendMessage(dataMap)
    }

    //--------------------------LifeCycle---------------------------

    // 释放全部资源
    fun releaseAllResource(){
        // 1. 状态值重置
        currentIsEmoji.set(false)
        isChatCalling = null

        // 2. 清空数据
        messageContactItemAo = null

        // 3. 清空回调
        recyclerViewWhereNeedUpdate = null
        onReceiveAgentTextCallback = null
//        onVadChatStateChange = null
        realtimeChatState.postValue(RealtimeChatState.NotInitialized)
//        realtimeChatVolume.postValue(0f)
        chatControllerPointer = null

        // 4. 清理资源
        realtimeChatWsClient?.let {
            // 考虑到已经关闭的情况
            try {
                it.close()
                realtimeChatWsClient = null
            } catch (e: Exception){
                Log.e(TAG, "releaseAllResource::realtimeChatWsClient error", e)
            }
        }

        // 5. 清理音频资源
        audioController?.let { controller ->
            try {
                controller.releaseAll()
                audioController = null
            } catch (e: Exception){
                Log.e(TAG, "releaseAllResource::audioController error", e)
            }
        }

        Log.i(TAG, "releaseAllResource::releaseAllResource")
    }

    // 销毁
    fun destroy(){
        releaseAllResource()
    }

}