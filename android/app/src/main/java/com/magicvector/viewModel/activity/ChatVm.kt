package com.magicvector.viewModel.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.core.appcore.api.ApiUrlConfig
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
import com.data.domain.constant.VadChatState
import com.data.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.data.domain.constant.chat.RealtimeResponseDataTypeEnum
import com.data.domain.dto.response.ChatMessageResponse
import com.data.domain.dto.ws.RealtimeChatConnectRequest
import com.data.domain.fragmentActivity.aao.ChatAAo
import com.data.domain.vo.test.RealtimeChatState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.magicvector.MainApplication
import com.magicvector.callback.OnVadChatStateChange
import com.magicvector.callback.VADCallTextCallback
import com.magicvector.manager.ChatManager
import com.magicvector.manager.ChatWsTextMessageHandler
import com.magicvector.manager.vad.VadDetectionCallback
import com.magicvector.manager.vad.VadSileroManager
import com.magicvector.utils.chat.RealtimeChatWsClient
import com.view.appview.R
import com.view.appview.call.CallAo
import com.view.appview.recycler.RecyclerViewWhereNeedUpdate
import com.view.appview.chat.ChatMessageAdapter
import com.view.appview.chat.OnChatMessageClick
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt


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

) : ViewModel(){

    companion object {
        val TAG: String = ChatVm::class.java.name
        val GSON = MainApplication.GSON
        val mainHandler: Handler = Handler(Looper.getMainLooper())
    }

    lateinit var recyclerViewWhereNeedUpdate: RecyclerViewWhereNeedUpdate
    lateinit var vadCallTextCallback: VADCallTextCallback
    lateinit var onVadChatStateChange: OnVadChatStateChange
    // 是否正在通话
    var isCalling: AtomicBoolean? = null
    fun initIsCalling(isCalling: AtomicBoolean) {
        // 直接从CallDialog中拿到boolean的地址
        this.isCalling = isCalling
    }

    fun initResource(
        activity: FragmentActivity,
        ao : MessageContactItemAo?,
        whereNeedUpdate: RecyclerViewWhereNeedUpdate,
        vadCallTextCallback: VADCallTextCallback,
        onVadChatStateChange: OnVadChatStateChange
    ) {

        realtimeChatState.postValue(RealtimeChatState.NotInitialized)

        this.recyclerViewWhereNeedUpdate = whereNeedUpdate
        this.vadCallTextCallback = vadCallTextCallback
        this.onVadChatStateChange = onVadChatStateChange

        aao.messageContactItemAo = ao
        if (aao.messageContactItemAo?.contactId != null){
            realtimeChatState.postValue(RealtimeChatState.Initializing)
            initRealtimeChatWsClient(activity)
        }
        else {
            realtimeChatState.postValue(RealtimeChatState.Error("Agent Id is Null"))
            throw IllegalArgumentException("Agent Id is Null")
        }
        aao.nameLd.postValue(ao?.vo?.name?: "")
        aao.avatarUrlLd.postValue(ao?.vo?.avatarUrl?: "")

        // 获取chatManager
        chatManagerPointer = MainApplication.getChatMapManager().getChatManager(
            aao.messageContactItemAo!!.contactId!!
        )

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

    //---------------------------AAo Ld---------------------------

    val aao = ChatAAo()

    lateinit var adapter : ChatMessageAdapter

    fun initAdapter(onChatMessageClick : OnChatMessageClick){
        adapter = ChatMessageAdapter(
            chatManagerPointer.getViewChatMessageList(),
            onChatMessageClick
        )
    }

    val realtimeChatState: MutableLiveData<RealtimeChatState> = MutableLiveData(RealtimeChatState.NotInitialized)
//    val realtimeChatVolume = MutableLiveData(0f)

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

    // audio

    private var realtimeChatWsClient: RealtimeChatWsClient? = null
    private var realtimeChatAudioRecord: AudioRecord? = null
    private var realtimeChatAudioTrack: AudioTrack? = null
    private var vadSileroManager: VadSileroManager? = null

    // 语音通话
    fun initVadCall(context: Context){
        vadSileroManager = VadSileroManager()

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

                    onVadChatStateChange.onChange(VadChatState.Speaking)
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

                    onVadChatStateChange.onChange(VadChatState.Speaking)
                }
            }

            override fun onStopSpeech() {
                val dataMap = mapOf(
                    RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.type,
                    RealtimeRequestDataTypeEnum.DATA to RealtimeRequestDataTypeEnum.STOP_AUDIO_RECORD.name
                )
                realtimeChatWsClient!!.sendMessage(dataMap)

                onVadChatStateChange.onChange(VadChatState.Silent)
            }
        })

        startVadCall()
    }

    fun startVadCall(){
        vadSileroManager?.let {
            it.startRecording()
            onVadChatStateChange.onChange(VadChatState.Silent)
            Log.i(TAG, "startVadCall: ")
        }
    }

    fun stopVadCall(){
        vadSileroManager?.let {
            it.stopRecording()
            onVadChatStateChange.onChange(VadChatState.Muted)
            Log.i(TAG, "stopVadCall: ")
        }
    }

    fun destroyVadCall(){
        vadSileroManager?.onDestroy()
        onVadChatStateChange.onChange(VadChatState.Muted)
    }

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

    // chatHistory First
    fun doGetLastChat(context: Context, callback: SyncRequestCallback){
        MainApplication.getApiRequestImplInstance().getLastChat(
            aao.messageContactItemAo!!.contactId!!,
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

    // 特定时间段的chat history todo: 上拉上滑获取之前的chat History
    fun doGetTimeLimitChat(context: Context, deadline: String, callback: SyncRequestCallback){
        MainApplication.getApiRequestImplInstance().getTimeLimitChat(
            aao.messageContactItemAo!!.contactId!!,
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

    private fun handleGetChatHistory(response: BaseResponse<ChatMessageResponse>?,
                                     context: Context,
                                     callback: SyncRequestCallback){

        response?.data?.chatMessages?.let {
            chatManagerPointer.setResponsesToViews(it)
            updateMessage()
        }

        callback.onAllRequestSuccess()
    }

    //---------------------------Logic---------------------------

    //===========chatHistory

    lateinit var chatManagerPointer: ChatManager

    val realTimeChatSampleRate = 24000
    // realtime chat

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun initRealtimeChatRecorderAndPlayer(){
        // 配置音频参数
        val inChannelConfig = AudioFormat.CHANNEL_IN_MONO
        val outChannelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val audioRecordBufferSize = AudioRecord.getMinBufferSize(realTimeChatSampleRate, inChannelConfig, audioFormat)
        val audioTrackBufferSize = AudioTrack.getMinBufferSize(realTimeChatSampleRate, outChannelConfig, audioFormat)

        // 创建AudioRecord
        realtimeChatAudioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            realTimeChatSampleRate,
            inChannelConfig,
            audioFormat,
            audioRecordBufferSize
        )

        // 创建AudioTrack
        realtimeChatAudioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            realTimeChatSampleRate,
            outChannelConfig,
            audioFormat,
            audioTrackBufferSize,
            AudioTrack.MODE_STREAM
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
                    request.agentId = aao.messageContactItemAo!!.contactId!!
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

    private fun handleTextMessage(text: String){
        // text --GSON--> Map<String, String>
        val map: Map<String, String> = GSON.fromJson(text, object : TypeToken<Map<String, String>>() {}.type)

        val typeStr = map[RealtimeResponseDataTypeEnum.TYPE]
        val type = RealtimeResponseDataTypeEnum.getByType(typeStr)

        when(type){
            RealtimeResponseDataTypeEnum.START_TTS -> {
                Log.i(TAG, "handleTextMessage::START_TTS播放")
                // 开始接收数据
                realtimeChatState.postValue(RealtimeChatState.Receiving)
                // 清空播放缓存
                realtimeChatAudioTrack?.flush()
                // 开始播放
                realtimeChatAudioTrack?.play()
                onVadChatStateChange.onChange(VadChatState.Replying)

                // VAD:设置AI回复的时候不能说话
                isCalling?.let {
                    // 正在通话
                    if (it.get()){
                        vadSileroManager?.stopRecording()
                        Log.d(TAG, "handleTextMessage:: 正在VAD通话:AI正在回复, 停止录音")
                    }
                    else {
                        Log.d(TAG, "handleTextMessage:: 停止VAD通话: 不管理VAD录音")
                    }
                }
            }
            RealtimeResponseDataTypeEnum.STOP_TTS -> {
                Log.i(TAG, "handleTextMessage::STOP_TTS播放")
                // 结束接收数据
                realtimeChatState.postValue(RealtimeChatState.InitializedConnected)
                // 停止播放
                realtimeChatAudioTrack?.stop()
                // 清空播放缓存
                realtimeChatAudioTrack?.flush()
                onVadChatStateChange.onChange(VadChatState.Silent)

                // VAD:设置AI回复结束的时候可以说话
                isCalling?.let {
                    // 正在通话
                    if (it.get()){
                        vadSileroManager?.startRecording()
                        Log.d(TAG, "handleTextMessage:: 正在VAD通话:AI回复结束, 继续录音")
                    }
                    else {
                        Log.d(TAG, "handleTextMessage:: 停止VAD通话: 不管理VAD录音")
                    }
                }
            }
            RealtimeResponseDataTypeEnum.AUDIO_CHUNK -> {
                // 音频数据
                realtimeChatState.postValue(RealtimeChatState.Receiving)
                val data = map[RealtimeResponseDataTypeEnum.DATA]
                data?.let {
                    playBase64Audio(data)
                }
                onVadChatStateChange.onChange(VadChatState.Replying)
            }
            RealtimeResponseDataTypeEnum.TEXT_CHAT_RESPONSE -> {
                // 文本数据
                realtimeChatState.postValue(RealtimeChatState.Receiving)
                val data = map[RealtimeResponseDataTypeEnum.DATA]
                if (data != null){
                    ChatWsTextMessageHandler.handleTextMessage(
                        message = data,
                        GSON = GSON,
                        chatManagerPointer = chatManagerPointer,
                        vadCallTextCallback = this.vadCallTextCallback
                    )
                    updateMessage()
                }
                else {
                    Log.e(TAG, "handleTextMessage: data is null")
                }
            }
            RealtimeResponseDataTypeEnum.WHOLE_CHAT_RESPONSE -> {}
            RealtimeResponseDataTypeEnum.TEXT_SYSTEM_RESPONSE -> {}
        }
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

    fun startRecordRealtimeChatAudio() {
        // 录制音频 -> 音频流bytes实时转为Base64的PCM格式 -> 调用websocket的sendAudioMessage
        val bufferSize = AudioRecord.getMinBufferSize(
            realTimeChatSampleRate,
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
        realtimeChatWsClient!!.sendMessage(dataMap, true)
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

    // update Message
    fun updateMessage(){
        mainHandler.post {
            val updateList = chatManagerPointer.getNeedUpdateList()
            if (!updateList.isEmpty()){
                recyclerViewWhereNeedUpdate.whereNeedUpdate(updateList)
                Log.d(TAG, "handleGetChatHistory::待更新数据：${updateList.size} 条")
            }
            else {
                Log.d(TAG, "handleGetChatHistory::没有待更新数据")
            }
        }
    }

    // 语音通话
    fun getCallAo(onMuteClickRunnable: Runnable?, onCallEndClickRunnable: Runnable?): CallAo{
        val callAo = CallAo()
        callAo.agentName = aao.nameLd.value
        callAo.agentAvatar = aao.avatarUrlLd.value
        callAo.agentId = aao.messageContactItemAo?.contactId

        callAo.onMuteClickRunnable = onMuteClickRunnable
        callAo.onCallEndClickRunnable = onCallEndClickRunnable

        return callAo
    }

    // 销毁
    fun destroy(){
        stopRealtimeChat()
        vadSileroManager?.onDestroy()
    }
}