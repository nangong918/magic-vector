package com.magicvector.manager

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
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
import com.data.domain.dto.ws.request.RealtimeChatConnectRequest
import com.data.domain.fragmentActivity.aao.ChatAAo
import com.data.domain.vo.test.RealtimeChatState
import com.google.gson.reflect.TypeToken
import com.magicvector.MainApplication
import com.magicvector.callback.OnVadChatStateChange
import com.magicvector.callback.VADCallTextCallback
import com.magicvector.manager.mcp.HandleSystemResponse
import com.magicvector.manager.vad.VadDetectionCallback
import com.magicvector.manager.vad.VadSileroManager
import com.magicvector.utils.chat.RealtimeChatWsClient
import com.view.appview.recycler.RecyclerViewWhereNeedUpdate
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt


/**
 * ChatMessageHandler管理：
 * WS长连接
 * ChatMessage处理
 * SystemMessage处理
 * 音频处理：AudioRecord，AudioTrack
 * VAD语音活动检测
 */
class ChatMessageHandler {

    companion object {
        val TAG = ChatMessageHandler::class.simpleName
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
    var vadCallTextCallback: VADCallTextCallback? = null
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

    // ws
    fun initRealtimeChatWsClient(activity: FragmentActivity) {
        realtimeChatWsClient = RealtimeChatWsClient(
            GSON,
            ApiUrlConfig.getWsMainUrl() + BaseConstant.WSConstantUrl.AGENT_REALTIME_CHAT_URL
        )

        PermissionUtil.requestPermissionSelectX(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            object : GainPermissionCallback{
                @RequiresPermission(Manifest.permission.RECORD_AUDIO)
                override fun allGranted() {
                    Log.i(TAG, "获取录音权限成功")

                    initRealtimeChatRecorderAndPlayer()

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
        realtimeChatWsClient!!.start(
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
                    // 发送连接成功的消息
                    val request = RealtimeChatConnectRequest()
                    request.agentId = messageContactItemAo!!.contactId!!
                    request.userId = MainApplication.getUserId()
                    request.timestamp = System.currentTimeMillis()

                    val dataMap = mapOf(
                        RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.CONNECT.type,
                        RealtimeRequestDataTypeEnum.DATA to GSON.toJson(request)
                    )

                    // 发送连接数据
                    realtimeChatWsClient!!.sendMessage(dataMap, true)
                }
            }
        )
    }

    //---------------------------Logic---------------------------

    //===========音频
    var realtimeChatWsClient: RealtimeChatWsClient? = null
    private var realtimeChatAudioRecord: AudioRecord? = null
    private var realtimeChatAudioTrack: AudioTrack? = null
    private var vadSileroManager: VadSileroManager? = null

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initRealtimeChatRecorderAndPlayer(){
        // 配置音频参数
        val inChannelConfig = AudioFormat.CHANNEL_IN_MONO
        val outChannelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val audioRecordBufferSize = AudioRecord.getMinBufferSize(BaseConstant.AUDIO.REALTIME_CHAT_SAMPLE_RATE, inChannelConfig, audioFormat)
        val audioTrackBufferSize = AudioTrack.getMinBufferSize(BaseConstant.AUDIO.REALTIME_CHAT_SAMPLE_RATE, outChannelConfig, audioFormat)

        // 创建AudioRecord
        realtimeChatAudioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            BaseConstant.AUDIO.REALTIME_CHAT_SAMPLE_RATE,
            inChannelConfig,
            audioFormat,
            audioRecordBufferSize
        )

        // 创建AudioTrack
        realtimeChatAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            BaseConstant.AUDIO.REALTIME_CHAT_SAMPLE_RATE,
            outChannelConfig,
            audioFormat,
            audioTrackBufferSize,
            AudioTrack.MODE_STREAM
        )
    }

    fun playBase64Audio(base64Audio: String) {

        val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)

        // 写入音频数据
        realtimeChatAudioTrack?.write(
            audioBytes,
            0,
            audioBytes.size
        )

        Log.i(TAG, "播放音频数据::: ${audioBytes.take(50)}")
    }

    // 按下录制音频
    fun startRecordRealtimeChatAudio() {
        // 录制音频 -> 音频流bytes实时转为Base64的PCM格式 -> 调用websocket的sendAudioMessage
        val bufferSize = AudioRecord.getMinBufferSize(
            BaseConstant.AUDIO.REALTIME_CHAT_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        // 发送启动录音
        val dataMap = mapOf(
            RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.START_AUDIO_RECORD.type,
            RealtimeRequestDataTypeEnum.DATA to RealtimeRequestDataTypeEnum.START_AUDIO_RECORD.name
        )
        realtimeChatWsClient?.sendMessage(dataMap)

        // 录制状态
        realtimeChatState.value = RealtimeChatState.RecordingAndSending

        val audioBuffer = ByteArray(bufferSize)
        realtimeChatAudioRecord?.startRecording()

        // 录制音频并转换为 Base64
        Thread {
            val handler = Handler(Looper.getMainLooper())
            val updateInterval = 100L // 100ms


            // 定时更新音量的 Runnable
            val volumeUpdateRunnable = object : Runnable {
                override fun run() {
                    if (realtimeChatState.value == RealtimeChatState.RecordingAndSending) {
                        // 使用最近读取的数据计算音量
                        /*                        val amplitude = calculateRMSAmplitude(audioBuffer, audioBuffer.size)
                                                Log.i(TAG, "realtimeChat音量: $amplitude")
                                                realtimeChatVolume.postValue(amplitude)*/
                        handler.postDelayed(this, updateInterval)
                    }
                }
            }

            // 启动定时更新
            handler.post(volumeUpdateRunnable)

            while (realtimeChatState.value == RealtimeChatState.RecordingAndSending) {
                val readSize = realtimeChatAudioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
                if (readSize > 0) {
                    val base64Audio = Base64.encodeToString(audioBuffer, 0, readSize, Base64.NO_WRAP)
                    val dataMap = mapOf(
                        RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.AUDIO_CHUNK.type,
                        RealtimeRequestDataTypeEnum.DATA to base64Audio
                    )
                    realtimeChatWsClient!!.sendMessage(dataMap)
//                    Log.i(TAG, "发送数据:: 类型: ${dataMap[RealtimeDataTypeEnum.TYPE]}; 长度: ${base64Audio.length}; 数据: ${base64Audio.take(100)}")
                }
            }

            try {
                realtimeChatAudioRecord?.stop()
            } catch (e : Exception){
                Log.e(TAG, "stopAndSendRealtimeChatAudio: ${e.message}")
            }

            // 发送结束录音
            val dataMap = mapOf(
                RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.type,
                RealtimeRequestDataTypeEnum.DATA to RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.name
            )
            realtimeChatWsClient!!.sendMessage(dataMap)
//            Log.i(TAG, "发送数据:: 类型: ${dataMap[RealtimeDataTypeEnum.TYPE]}")

        }.start()
    }

    // 松手发送音频
    fun stopAndSendRealtimeChatAudio(){
        realtimeChatState.postValue(RealtimeChatState.InitializedConnected)
    }

    fun stopRealtimeChat() {
        try {
            realtimeChatAudioRecord?.stop()
            realtimeChatAudioRecord?.release()
        } catch (e : Exception){
            Log.e(TAG, "释放录音失败", e)
        }
        try {
            realtimeChatAudioTrack?.stop()
            realtimeChatAudioTrack?.release()
        } catch (e : Exception){
            Log.e(TAG, "释放播放失败", e)
        }
        realtimeChatWsClient?.close()
        realtimeChatState.postValue(RealtimeChatState.Disconnected)
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
    fun initVadCall(context: Context){
        // 改为单例
        if (vadSileroManager == null){
            vadSileroManager = VadSileroManager()
        }

        vadSileroManager!!.init(context, object : VadDetectionCallback{
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
        })

        startVadCall()
    }

    fun startVadCall(){
        if (vadSileroManager != null) {
            vadSileroManager!!.startRecording()
            onVadChatStateChange?.onChange(VadChatState.Silent)
            Log.i(TAG, "startVadCall: ")
        }
        else {
            Log.e(TAG, "startVadCall: vadSileroManager == null")
        }
    }

    fun stopVadCall(){
        if (vadSileroManager != null) {
            vadSileroManager!!.stopRecording()
            onVadChatStateChange?.onChange(VadChatState.Muted)
            Log.i(TAG, "stopVadCall: ")
        }
        else {
            Log.e(TAG, "stopVadCall: vadSileroManager == null")
        }
    }

    fun destroyVadCall(){
        vadSileroManager?.onDestroy()
        onVadChatStateChange?.onChange(VadChatState.Muted)
    }

    //===========chatHistory

    private var chatManagerPointer: ChatManager? = null
    fun getChatManagerPointer(): ChatManager {
        return chatManagerPointer!!
    }

    fun initResource(
        chatActivity: FragmentActivity,
        ao : MessageContactItemAo?,
        chatAAo: ChatAAo,
        initNetworkRunnable: Runnable,
        whereNeedUpdate: RecyclerViewWhereNeedUpdate,
        vadCallTextCallback: VADCallTextCallback,
        onVadChatStateChange: OnVadChatStateChange
    ) {
        // 在初始化之前先清理全部资源
        try {
            releaseAllResource()
        } catch (e: Exception){
            Log.e(TAG, "initResource: ${e.message}")
        }

        realtimeChatState.postValue(RealtimeChatState.NotInitialized)

        this.recyclerViewWhereNeedUpdate = whereNeedUpdate
        this.vadCallTextCallback = vadCallTextCallback
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
        chatManagerPointer = MainApplication.getChatMapManager().getChatManager(
            messageContactItemAo!!.contactId!!
        )
        // 初始化网络请求
        initNetworkRunnable.run()
    }

    // update Message
    fun updateMessage(){
        if (chatManagerPointer != null && recyclerViewWhereNeedUpdate != null){
            mainHandler.post {
                val updateList = chatManagerPointer!!.getNeedUpdateList()
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
            Log.w(TAG, "更新失败，chatManagerPointer：$chatManagerPointer, " +
                    "recyclerViewWhereNeedUpdate: $recyclerViewWhereNeedUpdate")
        }
    }

    //===========realtime chat

    private fun handleTextMessage(text: String){
        // text --GSON--> Map<String, String>
        val map: Map<String, String> = GSON.fromJson(text, object : TypeToken<Map<String, String>>() {}.type)

        val typeStr = map[RealtimeResponseDataTypeEnum.TYPE]
        val type = RealtimeResponseDataTypeEnum.getByType(typeStr)

        when(type){
            // 开始TTS
            RealtimeResponseDataTypeEnum.START_TTS -> {
                Log.i(TAG, "handleTextMessage::START_TTS播放")
                // 开始接收数据
                realtimeChatState.postValue(RealtimeChatState.Receiving)
                // 清空播放缓存
                realtimeChatAudioTrack?.flush()
                // 开始播放
                realtimeChatAudioTrack?.play()
                onVadChatStateChange?.onChange(VadChatState.Replying)

                // VAD:设置AI回复的时候不能说话
                // 首先检查是不是Emoji状态
                if (currentIsEmoji.get()){
                    vadSileroManager?.stopRecording()
                    Log.d(TAG, "handleTextMessage:: 正在Emoji VAD通话:AI正在回复, 停止录音")
                }
                else if (isChatCalling?.get() == true) {
                    vadSileroManager?.startRecording()
                    Log.d(TAG, "handleTextMessage:: 正在VAD通话:AI正在回复, 停止录音")
                }
                else {
                    Log.d(TAG, "handleTextMessage:: 停止VAD通话: 不管理VAD录音")
                }
            }
            // 结束TTS + 结束会话
            RealtimeResponseDataTypeEnum.STOP_TTS -> {
                Log.i(TAG, "handleTextMessage::STOP_TTS播放")
                // 结束接收数据
                realtimeChatState.postValue(RealtimeChatState.InitializedConnected)
                // 停止播放
                realtimeChatAudioTrack?.stop()
                // 清空播放缓存
                realtimeChatAudioTrack?.flush()
                onVadChatStateChange?.onChange(VadChatState.Silent)

                // VAD:设置AI回复结束的时候可以说话
                if (currentIsEmoji.get()) {
                    vadSileroManager?.startRecording()
                    Log.d(TAG, "handleTextMessage:: 正在Emoji VAD通话:AI回复结束, 继续录音")
                }
                else if (isChatCalling?.get() == true){
                    vadSileroManager?.startRecording()
                    Log.d(TAG, "handleTextMessage:: 正在VAD通话:AI回复结束, 继续录音")
                }
                else {
                    Log.d(TAG, "handleTextMessage:: 停止VAD通话: 不管理VAD录音")
                }
            }
            // 音频流
            RealtimeResponseDataTypeEnum.AUDIO_CHUNK -> {
                // 音频数据
                realtimeChatState.postValue(RealtimeChatState.Receiving)
                val data = map[RealtimeResponseDataTypeEnum.DATA]
                data?.let {
                    playBase64Audio(data)
                }
                onVadChatStateChange?.onChange(VadChatState.Replying)
            }
            // 文本流
            RealtimeResponseDataTypeEnum.TEXT_CHAT_RESPONSE -> {
                // 文本数据
                realtimeChatState.postValue(RealtimeChatState.Receiving)
                val data = map[RealtimeResponseDataTypeEnum.DATA]
                if (data != null){
                    if (chatManagerPointer != null){
                        ChatWsTextMessageHandler.handleTextMessage(
                            message = data,
                            GSON = GSON,
                            chatManagerPointer = chatManagerPointer!!,
                            vadCallTextCallback = this.vadCallTextCallback
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
        vadCallTextCallback = null
//        onVadChatStateChange = null
        realtimeChatState.postValue(RealtimeChatState.NotInitialized)
//        realtimeChatVolume.postValue(0f)
        chatManagerPointer = null

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
        realtimeChatAudioRecord?.let {
            // 考虑到已经关闭的情况或者释放资源的情况
            try {
                it.stop()
                it.release()
                realtimeChatAudioRecord = null
            } catch (e: Exception){
                Log.e(TAG, "releaseAllResource::realtimeChatAudioRecord error", e)
            }
        }
        realtimeChatAudioTrack?.let {
            // 考虑到已经关闭的情况或者释放资源的情况
            try {
                it.stop()
                it.release()
                realtimeChatAudioTrack = null
            } catch (e: Exception){
                Log.e(TAG, "releaseAllResource::realtimeChatAudioTrack error", e)
            }
        }
        vadSileroManager?.let {
            try {
                it.onDestroy()
                vadSileroManager = null
            } catch (e: Exception){
                Log.e(TAG, "releaseAllResource::vadSileroManager error", e)
            }
        }

        Log.i(TAG, "releaseAllResource::releaseAllResource")
    }

    // 销毁
    fun destroy(){
        releaseAllResource()
    }

}