package com.magicvector.viewModel.activity

import android.content.ComponentName
import android.content.Context
import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import com.core.appcore.api.handler.SyncRequestCallback
import com.core.appcore.utils.AppResponseUtil
import com.core.baseutil.file.FileUtil
import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.data.domain.constant.BaseConstant
import com.data.domain.constant.VadChatState
import com.data.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.data.domain.constant.chat.RealtimeSystemResponseEventEnum
import com.data.domain.constant.chat.VisionUploadTypeEnum
import com.data.domain.dto.ws.request.UploadPhotoRequest
import com.magicvector.MainApplication
import com.magicvector.manager.RealtimeChatController
import com.magicvector.manager.mcp.VisionMcpManager
import com.magicvector.service.ChatService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.lang.ref.WeakReference

class ComposeAgentEmojiVm : AndroidViewModel(application = MainApplication.getApp()) {

    companion object {
        val TAG: String = ComposeAgentEmojiVm::class.java.name
        private val api = MainApplication.getApiRequestImplInstance()
        private const val EYE_RESET_DELAY_MS = 2000L
    }

    // MVI State: 页面可观察状态
    private val _uiState = MutableStateFlow(AgentEmojiState())
    val uiState: StateFlow<AgentEmojiState> = _uiState.asStateFlow()

    // MVI Effect: 一次性事件（权限、Toast、启动视觉等）
    private val _effect = Channel<AgentEmojiEffect>(Channel.BUFFERED)
    val effect: Flow<AgentEmojiEffect> = _effect.receiveAsFlow()

    private var targetResetJob: Job? = null
    private var currentFrameBitmap: Bitmap? = null
    private var agentId: String = ""
    private var agentName: String = ""

    private var chatServiceBinder: ChatService.ChatServiceBinder? = null
    val chatServiceBoundLd = MutableLiveData(false)

    var realtimeChatController: RealtimeChatController? = null
        private set

    private var onBoundChatService: Runnable? = null

    private val chatServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: android.os.IBinder?) {
            val binder = service as? ChatService.ChatServiceBinder ?: return
            chatServiceBinder = binder
            chatServiceBoundLd.postValue(true)
            realtimeChatController = binder.getChatMessageHandler()
            _uiState.update { it.copy(isServiceBound = true) }
            onBoundChatService?.run()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            chatServiceBoundLd.postValue(false)
            _uiState.update { it.copy(isServiceBound = false) }
            chatServiceBinder = null
            realtimeChatController = null
        }
    }

    fun processIntent(intent: AgentEmojiIntent) {
        when (intent) {
            is AgentEmojiIntent.Initialize -> initialize(intent.agentId, intent.agentName)
            is AgentEmojiIntent.OnServiceBound -> onServiceBound(intent.afterBound)
            AgentEmojiIntent.OnResume -> sendEffect(AgentEmojiEffect.RequestRecordPermission)
            is AgentEmojiIntent.OnRecordPermissionGranted -> onRecordPermissionGranted(intent.context)
            AgentEmojiIntent.OnRecordPermissionDenied -> sendEffect(AgentEmojiEffect.ShowToast("录音权限被拒绝"))
            AgentEmojiIntent.OnPause -> onPause()
            AgentEmojiIntent.OnDestroy -> onDestroy()
            AgentEmojiIntent.ToggleMic -> toggleMic()
            AgentEmojiIntent.SwitchCamera -> sendEffect(AgentEmojiEffect.SwitchCamera)
            AgentEmojiIntent.ToggleVideoVisible -> _uiState.update { it.copy(isVideoVisible = !it.isVideoVisible) }
            AgentEmojiIntent.VisionTest -> visionTest()
            is AgentEmojiIntent.OnVadStateChanged -> onVadStateChanged(intent.state)
            is AgentEmojiIntent.OnInferenceChanged -> _uiState.update { it.copy(inferenceTimeMs = intent.inferenceMs) }
            is AgentEmojiIntent.OnDetectionChanged -> onDetectionChanged(intent.detectionType)
            is AgentEmojiIntent.OnTargetChanged -> onTargetChanged(intent.xFraction, intent.yFraction)
            is AgentEmojiIntent.OnCurrentFrame -> currentFrameBitmap = intent.bitmap
            is AgentEmojiIntent.HandleSystemResponse -> handleSystemResponse(intent.map, intent.context)
            AgentEmojiIntent.StartVision -> sendEffect(AgentEmojiEffect.StartVision)
            is AgentEmojiIntent.SetEmojiCallbacksBound -> {
                _uiState.update { it.copy(emojiCallbacksBound = intent.bound) }
            }
        }
    }

    /**
     * 初始化页面基础信息（来自 Intent）
     */
    private fun initialize(agentId: String?, agentName: String?) {
        this.agentId = agentId ?: ""
        this.agentName = agentName ?: ""
        _uiState.update {
            it.copy(
                agentId = this.agentId,
                agentName = this.agentName
            )
        }
    }

    /**
     * 绑定 ChatService，拿到 RealtimeChatController。
     */
    private fun onServiceBound(afterBound: Runnable) {
        this.onBoundChatService = afterBound
        val serviceIntent = Intent(application, ChatService::class.java)
        application.bindService(serviceIntent, chatServiceConnection, BIND_AUTO_CREATE)
    }

    /**
     * 录音权限通过后，启动 Emoji 模式下的 VAD 通话。
     */
    private fun onRecordPermissionGranted(context: Context) {
        realtimeChatController?.let { controller ->
            controller.initVadCall(WeakReference(context))
            controller.currentIsEmoji.set(true)
            _uiState.update { it.copy(isMicClosed = false, vadChatState = VadChatState.Silent) }
        }
    }

    /**
     * 页面进入后台：关闭 VAD、取消系统回调，避免后台继续占用音频。
     */
    private fun onPause() {
        realtimeChatController?.let { controller ->
            controller.stopVadCall()
            controller.currentIsEmoji.set(false)
            controller.setHandleSystemResponse(null)
        }
        _uiState.update { it.copy(emojiCallbacksBound = false) }
    }

    /**
     * 页面销毁：释放 UDP 视觉和 Service 连接。
     */
    private fun onDestroy() {
        realtimeChatController?.udpVisionManager?.destroy()
        disconnectService()
    }

    /**
     * mic 开关：与 VAD 状态保持一致。
     */
    private fun toggleMic() {
        val current = _uiState.value
        if (current.isMicClosed) {
            realtimeChatController?.startVadCall()
            _uiState.update { it.copy(isMicClosed = false, vadChatState = VadChatState.Silent) }
        } else {
            realtimeChatController?.stopVadCall()
            _uiState.update { it.copy(isMicClosed = true, vadChatState = VadChatState.Muted) }
        }
    }

    /**
     * 更新通话状态文案；当 mic 关闭时强制状态为 Muted。
     */
    private fun onVadStateChanged(state: VadChatState) {
        val actual = if (_uiState.value.isMicClosed &&
            (state is VadChatState.Silent || state is VadChatState.Speaking)
        ) {
            VadChatState.Muted
        } else {
            state
        }
        _uiState.update { it.copy(vadChatState = actual) }
    }

    /**
     * 将检测类型映射为 UI 颜色状态。
     */
    private fun onDetectionChanged(detectionType: Int?) {
        val colorType = when (detectionType) {
            0 -> DetectionColorType.GOLD
            1 -> DetectionColorType.RED
            else -> DetectionColorType.DEFAULT
        }
        _uiState.update { it.copy(detectionColorType = colorType) }
    }

    /**
     * 眼睛目标点更新：
     * - 立即看向新目标
     * - 2 秒后自动复位到中心
     */
    private fun onTargetChanged(xFraction: Float, yFraction: Float) {
        _uiState.update {
            it.copy(
                eyeTargetX = xFraction.coerceIn(0f, 1f),
                eyeTargetY = yFraction.coerceIn(0f, 1f)
            )
        }

        targetResetJob?.cancel()
        targetResetJob = viewModelScope.launch {
            delay(EYE_RESET_DELAY_MS)
            _uiState.update { it.copy(detectionColorType = DetectionColorType.RESETTING) }
            _uiState.update {
                it.copy(
                    eyeTargetX = 0.5f,
                    eyeTargetY = 0.5f,
                    detectionColorType = DetectionColorType.DEFAULT
                )
            }
        }
    }

    /**
     * 触发一次视觉问答测试消息。
     */
    private fun visionTest() {
        val userQuestion = "你表述一下现在看到的场景。"
        val dataMap = mapOf(
            RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.USER_TEXT_MESSAGE.type,
            RealtimeRequestDataTypeEnum.DATA to userQuestion
        )
        realtimeChatController?.realtimeChatWsClient?.sendMessage(dataMap, true)
    }

    /**
     * 处理 MCP 系统消息（当前主要是上传图片事件）。
     */
    private fun handleSystemResponse(map: Map<String, String>, context: Context) {
        val event = map[RealtimeSystemResponseEventEnum.EVENT_KET] ?: return
        if (event != RealtimeSystemResponseEventEnum.UPLOAD_PHOTO.code) return

        val localAgentId = map["agentId"] ?: ""
        val userId = MainApplication.getUserId()
        val messageId = map["messageId"] ?: ""
        if (localAgentId.isEmpty() || userId.isEmpty() || messageId.isEmpty()) {
            sendEffect(AgentEmojiEffect.ShowToast("vision视觉理解失败，参数不全"))
            return
        }

        val bitmap = currentFrameBitmap
        if (bitmap == null) {
            val request = UploadPhotoRequest().apply {
                this.agentId = localAgentId
                this.userId = userId
                this.messageId = messageId
                this.isHavePhoto = false
            }
            val dataMap = mapOf(
                RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.SYSTEM_MESSAGE.type,
                RealtimeRequestDataTypeEnum.DATA to MainApplication.GSON.toJson(request)
            )
            realtimeChatController?.realtimeChatWsClient?.sendMessage(dataMap)
            sendEffect(AgentEmojiEffect.ShowToastRes(com.view.appview.R.string.fetch_photo_fail))
            return
        }

        when (BaseConstant.VISION.UPLOAD_METHOD) {
            VisionUploadTypeEnum.HTTP -> {
                httpUploadSingleImageVision(
                    context = context,
                    bitmaps = listOf(bitmap),
                    agentId = localAgentId,
                    userId = userId,
                    messageId = messageId
                )
            }
            VisionUploadTypeEnum.WS_FRAGMENT -> {
                wsUploadSingleImageVision(
                    bitmap = bitmap,
                    agentId = localAgentId,
                    userId = userId,
                    messageId = messageId
                )
            }
            VisionUploadTypeEnum.RTMP -> {
            }
            VisionUploadTypeEnum.UNKNOWN -> throw IllegalArgumentException("unknown vison upload type")
        }
    }

    /**
     * HTTP 上传视觉图片。
     */
    private fun httpUploadSingleImageVision(
        context: Context,
        bitmaps: List<Bitmap>,
        agentId: String,
        userId: String,
        messageId: String
    ) {
        val files = VisionMcpManager.bitmapsToFlies(bitmaps, context)
        sendEffect(AgentEmojiEffect.ShowLoading)
        doUploadImageVision(
            context = context,
            images = files,
            agentId = agentId,
            userId = userId,
            messageId = messageId,
            callback = object : SyncRequestCallback {
                override fun onThrowable(throwable: Throwable?) {
                    Log.e(TAG, "httpUploadSingleImageVision error", throwable)
                    sendEffect(AgentEmojiEffect.HideLoading)
                }

                override fun onAllRequestSuccess() {
                    sendEffect(AgentEmojiEffect.HideLoading)
                }
            }
        )
    }

    /**
     * WS 分片上传视觉图片，最后一片发送后提示成功。
     */
    private fun wsUploadSingleImageVision(
        bitmap: Bitmap,
        agentId: String,
        userId: String,
        messageId: String
    ) {
        val base64Str = VisionMcpManager.bitmapToBase64(bitmap)
        val queue = VisionMcpManager.fileBase64toQueue(base64Str)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                while (queue.isNotEmpty()) {
                    val fragment = queue.poll()
                    val request = UploadPhotoRequest().apply {
                        this.agentId = agentId
                        this.userId = userId
                        this.messageId = messageId
                        this.isHavePhoto = true
                        this.photoBase64 = fragment
                        this.isLastFragment = queue.isEmpty()
                    }

                    val dataMap = mapOf(
                        RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.SYSTEM_MESSAGE.type,
                        RealtimeRequestDataTypeEnum.DATA to MainApplication.GSON.toJson(request)
                    )
                    realtimeChatController?.realtimeChatWsClient?.sendMessage(dataMap)

                    if (queue.isNotEmpty()) {
                        delay(BaseConstant.VISION.WS_SHARD_UPLOAD_DELAY)
                    } else {
                        sendEffect(AgentEmojiEffect.ShowToastRes(com.view.appview.R.string.fetch_photo_success))
                    }
                }
            }
        }
    }

    /**
     * 复用旧仓储逻辑：multipart 上传图片并处理统一响应。
     */
    private fun doUploadImageVision(
        context: Context,
        images: List<File>,
        agentId: String,
        userId: String,
        messageId: String,
        callback: SyncRequestCallback
    ) {
        val imageParam: List<MultipartBody.Part> = FileUtil.createImageMultipartBodyParts(images, "images")
            ?: run {
                callback.onThrowable(Throwable("image is null"))
                return
            }

        val agentIdParam = RequestBody.create("text/plain".toMediaTypeOrNull(), agentId)
        val userIdParam = RequestBody.create("text/plain".toMediaTypeOrNull(), userId)
        val messageIdParam = RequestBody.create("text/plain".toMediaTypeOrNull(), messageId)

        api.uploadImageVision(
            images = imageParam,
            agentId = agentIdParam,
            userId = userIdParam,
            messageId = messageIdParam,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<String>> {
                override fun onResponse(response: BaseResponse<String>?) {
                    AppResponseUtil.handleSyncResponseEx(
                        response = response,
                        context = context,
                        callback = callback
                    ) { _, _ ->
                        callback.onAllRequestSuccess()
                    }
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    callback.onThrowable(throwable)
                }
            }
        )
    }

    /**
     * Service 解绑，避免 ViewModel 持有无效连接。
     */
    private fun disconnectService() {
        application.let { context ->
            if (chatServiceBoundLd.value == true) {
                try {
                    context.unbindService(chatServiceConnection)
                } catch (e: Exception) {
                    Log.e(TAG, "disconnectService failed", e)
                }
            }
        }
        chatServiceBoundLd.postValue(false)
        chatServiceBinder = null
        realtimeChatController = null
    }

    private fun sendEffect(effect: AgentEmojiEffect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    override fun onCleared() {
        super.onCleared()
        targetResetJob?.cancel()
        disconnectService()
    }
}

@Stable
data class AgentEmojiState(
    // 基础信息
    val agentId: String = "",
    val agentName: String = "",
    // Service 状态
    val isServiceBound: Boolean = false,
    // 通话状态
    val isMicClosed: Boolean = false,
    val vadChatState: VadChatState = VadChatState.Silent,
    // 视觉面板状态
    val isVideoVisible: Boolean = true,
    val inferenceTimeMs: Long = 0L,
    // 眼睛移动目标点（归一化坐标）
    val eyeTargetX: Float = 0.5f,
    val eyeTargetY: Float = 0.5f,
    // 活动检测色块状态
    val detectionColorType: DetectionColorType = DetectionColorType.DEFAULT,
    // 防止重复绑定 callback
    val emojiCallbacksBound: Boolean = false,
)

enum class DetectionColorType {
    DEFAULT,
    GOLD,
    RED,
    RESETTING
}

sealed class AgentEmojiIntent {
    // 初始化 / 生命周期
    data class Initialize(val agentId: String?, val agentName: String?) : AgentEmojiIntent()
    data class OnServiceBound(val afterBound: Runnable) : AgentEmojiIntent()

    data object OnResume : AgentEmojiIntent()
    data class OnRecordPermissionGranted(val context: Context) : AgentEmojiIntent()
    data object OnRecordPermissionDenied : AgentEmojiIntent()
    data object OnPause : AgentEmojiIntent()
    data object OnDestroy : AgentEmojiIntent()

    // 用户交互
    data object ToggleMic : AgentEmojiIntent()
    data object SwitchCamera : AgentEmojiIntent()
    data object ToggleVideoVisible : AgentEmojiIntent()
    data object VisionTest : AgentEmojiIntent()
    data object StartVision : AgentEmojiIntent()

    // 视觉/通话数据回流
    data class OnVadStateChanged(val state: VadChatState) : AgentEmojiIntent()
    data class OnInferenceChanged(val inferenceMs: Long) : AgentEmojiIntent()
    data class OnDetectionChanged(val detectionType: Int?) : AgentEmojiIntent()
    data class OnTargetChanged(val xFraction: Float, val yFraction: Float) : AgentEmojiIntent()
    data class OnCurrentFrame(val bitmap: Bitmap) : AgentEmojiIntent()
    data class HandleSystemResponse(val map: Map<String, String>, val context: Context) : AgentEmojiIntent()
    data class SetEmojiCallbacksBound(val bound: Boolean) : AgentEmojiIntent()
}

sealed class AgentEmojiEffect {
    // 权限/设备
    data object RequestRecordPermission : AgentEmojiEffect()
    data object SwitchCamera : AgentEmojiEffect()
    data object StartVision : AgentEmojiEffect()
    // 反馈
    data object ShowLoading : AgentEmojiEffect()
    data object HideLoading : AgentEmojiEffect()
    data class ShowToast(val message: String) : AgentEmojiEffect()
    data class ShowToastRes(val messageRes: Int) : AgentEmojiEffect()
}
