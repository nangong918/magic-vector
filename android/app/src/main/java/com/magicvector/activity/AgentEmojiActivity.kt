package com.magicvector.activity

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.core.appcore.api.handler.SyncRequestCallback
import com.core.baseutil.network.networkLoad.NetworkLoadUtils
import com.core.baseutil.ui.ToastUtils
import com.data.domain.constant.BaseConstant
import com.data.domain.constant.VadChatState
import com.data.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.data.domain.constant.chat.RealtimeSystemResponseEventEnum
import com.data.domain.constant.chat.VisionUploadTypeEnum
import com.data.domain.dto.ws.request.UploadPhotoRequest
import com.detection.yolov8.BoundingBox
import com.detection.yolov8.Detector
import com.detection.yolov8.targetPoint.YOLOv8TargetPointGenerator
import com.magicvector.MainApplication
import com.magicvector.callback.OnVadChatStateChange
import com.magicvector.databinding.ActivityAgentEmojiBinding
import com.magicvector.manager.mcp.HandleSystemResponse
import com.magicvector.manager.mcp.VisionMcpManager
import com.magicvector.manager.yolo.EyesMoveManager
import com.magicvector.manager.yolo.OnResetCallback
import com.magicvector.manager.yolo.TargetActivityDetectionManager
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.AgentEmojiVm
import com.view.appview.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.LinkedList
import java.util.Queue

class AgentEmojiActivity : BaseAppCompatVmActivity<ActivityAgentEmojiBinding, AgentEmojiVm>(
    AgentEmojiActivity::class,
    AgentEmojiVm::class
), HandleSystemResponse, OnVadChatStateChange {

    companion object {
        val GSON = MainApplication.GSON
    }

    var visionManager = MainApplication.getVisionManager()
    var chatMessageHandler = MainApplication.getChatMessageHandler()

    override fun initBinding(): ActivityAgentEmojiBinding {
        return ActivityAgentEmojiBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        visionManager = MainApplication.getVisionManager()
        chatMessageHandler = MainApplication.getChatMessageHandler()
    }

    override fun initViewModel() {
        super.initViewModel()

        val agentId = intent.getStringExtra("agentId")
        val agentName = intent.getStringExtra("agentName")

        visionManager.initStart(
            context = this,
            previewView = binding.viewFinder,
            listener = getDetectListener(),
            lifecycleOwner = this as LifecycleOwner
        )

        observeData()
    }

    fun observeData(){
/*        chatMessageHandler.realtimeChatState.observe(this) {
            runOnUiThread {
                when (it) {
                    RealtimeChatState.NotInitialized -> {
                        binding.tvCallStatue.text = "未初始化"
                    }
                    RealtimeChatState.Initializing -> {
                        binding.tvCallStatue.text = "正在初始化"
                    }
                    RealtimeChatState.InitializedConnected -> {
                        binding.tvCallStatue.text = "已初始化并连接"
                    }
                    RealtimeChatState.RecordingAndSending -> {
                        binding.tvCallStatue.text = "正在记录消息"
                    }
                    RealtimeChatState.Receiving -> {
                        binding.tvCallStatue.text = "正在接收消息"
                    }
                    RealtimeChatState.Disconnected -> {
                        binding.tvCallStatue.text = "已断开连接"
                    }
                    is RealtimeChatState.Error -> {
                        binding.tvCallStatue.text = "错误"
                    }
                }
            }
        }*/
    }

    override fun initView() {
        super.initView()

        setIsCloseMic(isCloseMic)
    }

    // 是否关闭mic
    private var isCloseMic = false
    @SuppressLint("ResourceType")
    fun setIsCloseMic(isStopRecordAudio: Boolean) {
        // value
        isCloseMic = isStopRecordAudio
        // UI
        binding.ivMic.setImageResource(
            if (isCloseMic) {
                // 停止录音：展示开始录音
                R.xml.mic_24px
            }
            else {
                // 开始录音：展示停止录音
                R.xml.mic_off_24px
            }
        )
        // logic
        if (isCloseMic) {
            setVadChatState(VadChatState.Muted)
        }
        else {
            setVadChatState(VadChatState.Silent)
        }
    }

    fun changeMicState(){
        setIsCloseMic(!isCloseMic)

        // 关闭了Mic
        if (isCloseMic) {
            chatMessageHandler.stopVadCall()
        }
        else {
            chatMessageHandler.startVadCall()
        }
    }

    private var vadChatState = VadChatState()
    fun setVadChatState(state: VadChatState){

        if (state == VadChatState.Replying) {
            Log.i("AgentEmojiActivity", "setVadChatState: Replying")
        }
        else if (state == VadChatState.Speaking) {
            Log.i("AgentEmojiActivity", "setVadChatState: Speaking")
        }


        // 关闭mic之后就不存在Speaking和Silent了
        if (isCloseMic){
            if (vadChatState == VadChatState.Silent || vadChatState == VadChatState.Speaking){
                vadChatState = VadChatState.Muted
            }
        }

        runOnUiThread {
            when (state) {
                is VadChatState.Muted -> {
                    binding.tvCallStatue.text = getString(R.string.muted)
                }
                is VadChatState.Silent -> {
                    binding.tvCallStatue.text = getString(R.string.silent)
                }
                is VadChatState.Speaking -> {
                    binding.tvCallStatue.text = getString(R.string.user_speaking)
                }
                is  VadChatState.Replying -> {
                    binding.tvCallStatue.text = getString(R.string.agent_replying)
                }
                is VadChatState.Error -> {
                    binding.tvCallStatue.text = getString(R.string.error)
                    Log.e(TAG, "setVadChatState: ${state.message}")
                }
            }
        }
    }

    override fun setListener() {
        super.setListener()

        // 切换摄像头的方法
        binding.btnSwitchCamera.setOnClickListener {
            visionManager.switchCamera(
                previewView = binding.viewFinder,
                lifecycleOwner = this as LifecycleOwner
            )
        }

        binding.ivMic.setOnClickListener {
            if (isCloseMic) {
                // 停止录音：展示开始录音
                R.xml.mic_24px
            }
            else {
                // 开始录音：展示停止录音
                R.xml.mic_off_24px
            }
            changeMicState()
        }

        // logic
        if (isCloseMic) {
            setVadChatState(VadChatState.Muted)
        }
        else {
            setVadChatState(VadChatState.Silent)
        }
    }

    // 监听VAD状态
    override fun onChange(state: VadChatState) {
        setVadChatState(state)
    }

    private fun getDetectListener(): Detector.DetectorListener {
        return object : Detector.DetectorListener {
            override fun onEmptyDetect() {
                runOnUiThread {
                    binding.overlay.clear()
                }
            }

            override fun onDetect(
                boundingBoxes: List<BoundingBox>,
                inferenceTime: Long
            ) {
                runOnUiThread {
                    val inferenceTimeStr = "${inferenceTime}ms"
                    binding.inferenceTime.text = inferenceTimeStr
                    binding.overlay.apply {
                        setResults(boundingBoxes)
                        invalidate()
                    }

                    val maxTarget = YOLOv8TargetPointGenerator.generateTargetPoint(
                        boundingBoxes,
                        BaseConstant.YOLO.FILTER_SIZE
                    )

                    // 活动检测
                    val result = TargetActivityDetectionManager.detect(
                        boundingBoxes,
                        targetPoint = maxTarget
                    )

                    result.let {
                        runOnUiThread {
                            val gold = com.view.appview.R.color.gold
                            val red = com.view.appview.R.color.red
                            val light_blue_600 = com.view.appview.R.color.light_blue_600
                            var displayColor = light_blue_600
                            if (it.result) {
                                if (it.detectionType != null) {
                                    displayColor = when (it.detectionType) {
                                        0 -> {
                                            gold
                                        }
                                        1 -> {
                                            red
                                        }
                                        else -> {
                                            light_blue_600
                                        }
                                    }
                                }
                            }

                            binding.vMoveDetect.setBackgroundResource(displayColor)
                        }
                    }

                    // 获取屏幕宽高
                    val screenWidth = resources.displayMetrics.widthPixels
                    val screenHeight = resources.displayMetrics.heightPixels

                    // 移动布局
                    EyesMoveManager.moveLayoutToTargetPoint(
                        targetPoint = maxTarget,
                        screenWidth = screenWidth,
                        screenHeight = screenHeight,
                        layout = binding.lyEmoji,
                        isThisReset = false,
                        onResetCallback = onResetCallback
                    )
                }
            }
        }
    }

    // 处理视觉理解Response
    override fun handleSystemResponse(map: Map<String, String>) {
        val agentId = map["agentId"]
        val userId = MainApplication.getUserId()
        val messageId = map["messageId"]

        if (TextUtils.isEmpty(agentId) || TextUtils.isEmpty(userId) || TextUtils.isEmpty(messageId)){
            ToastUtils.showToastActivity(this@AgentEmojiActivity, "vision视觉理解失败，参数不全")
            return
        }

        // 上传图片的指令
        if (map[RealtimeSystemResponseEventEnum.EVENT_KET] == RealtimeSystemResponseEventEnum.UPLOAD_PHOTO.code) {
            val bitmap = visionManager.getCurrentFrameBitmap()
            if (bitmap == null) {
                // 传递无图片的响应给服务器
                val uploadPhotoRequest = UploadPhotoRequest()
                uploadPhotoRequest.agentId = agentId
                uploadPhotoRequest.userId = userId
                uploadPhotoRequest.messageId = messageId
                uploadPhotoRequest.isHavePhoto = false

                val uploadPhotoRequestJson = GSON.toJson(uploadPhotoRequest)
                val dataMap = mapOf(
                    RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.SYSTEM_MESSAGE.type,
                    RealtimeRequestDataTypeEnum.DATA to uploadPhotoRequestJson
                )

                chatMessageHandler.realtimeChatWsClient?.sendMessage(dataMap)
                ToastUtils.showToastActivity(this, getString(R.string.fetch_photo_fail))
            }
            else {
                // 上传的方式
                val visonUploadType = BaseConstant.VISION.UPLOAD_METHOD

                when (visonUploadType) {
                    VisionUploadTypeEnum.UNKNOWN -> {
                        throw IllegalArgumentException("unknown vison upload type")
                    }
                    VisionUploadTypeEnum.HTTP -> {
                        // http上传
                        httpUploadSingleImageVision(
                            bitmap = bitmap,
                            agentId = agentId!!,
                            userId = userId,
                            messageId = messageId!!
                        )
                    }
                    VisionUploadTypeEnum.WS_FRAGMENT -> {
                        // ws 分片上传
                        wsUploadSingleImageVision(
                            bitmap = bitmap,
                            agentId = agentId!!,
                            userId = userId,
                            messageId = messageId!!
                        )
                    }
                    VisionUploadTypeEnum.RTMP -> {
                        // 暂未实现
                    }
                }
            }
        }
    }

    /// vision 任务
    // http 上传
    private fun httpUploadSingleImageVision(bitmap: Bitmap, agentId: String, userId: String, messageId: String){
        val file = VisionMcpManager.bitmapToFile(
            bitmap = bitmap,
            context = this
        )
        NetworkLoadUtils.showDialog(this)
        vm.doUploadImageVision(
            context = this,
            image = file,
            agentId = agentId,
            userId = userId,
            messageId = messageId,
            callback = object : SyncRequestCallback{
                override fun onThrowable(throwable: Throwable?) {
                    Log.e(TAG, "httpUploadSingleImageVision::error: ", throwable)
                    NetworkLoadUtils.dismissDialogSafety(this@AgentEmojiActivity)
                }

                override fun onAllRequestSuccess() {
                    NetworkLoadUtils.dismissDialogSafety(this@AgentEmojiActivity)
                }
            }
        )
    }

    // ws上传 [额外的kotlin协程异步上传，并设置上传休眠时间，避免出现网络拥塞]
    private fun wsUploadSingleImageVision(bitmap: Bitmap, agentId: String, userId: String, messageId: String) {
        // bitmap -> base64
        val base64Str = VisionMcpManager.bitmapToBase64(
            bitmap = bitmap
        )

        // 获取分片完成的文件队列
        val base64FragmentQueue = VisionMcpManager.fileBase64toQueue(base64Str)

        // 使用协程异步上传
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                while (base64FragmentQueue.isNotEmpty()) {
                    val base64Fragment = base64FragmentQueue.poll()
                    val uploadPhotoRequest = UploadPhotoRequest()
                    uploadPhotoRequest.agentId = agentId
                    uploadPhotoRequest.userId = userId
                    uploadPhotoRequest.messageId = messageId
                    uploadPhotoRequest.isHavePhoto = true
                    uploadPhotoRequest.photoBase64 = base64Fragment

                    // 正确判断最后一个分片：队列为空时就是最后一个
                    uploadPhotoRequest.isLastFragment = base64FragmentQueue.isEmpty()

                    val uploadPhotoRequestJson = GSON.toJson(uploadPhotoRequest)
                    val dataMap = mapOf(
                        RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.SYSTEM_MESSAGE.type,
                        RealtimeRequestDataTypeEnum.DATA to uploadPhotoRequestJson
                    )

                    chatMessageHandler.realtimeChatWsClient?.sendMessage(dataMap)

                    // 添加休眠，避免网络拥塞（不是最后一个分片时休眠）
                    if (base64FragmentQueue.isNotEmpty()) {
                        // 休眠20毫秒
                        delay(
                            timeMillis = BaseConstant.VISION.WS_SHARD_UPLOAD_DELAY
                        )
                    }

                    // 只在最后一个分片发送成功后显示Toast
                    if (base64FragmentQueue.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            ToastUtils.showToastActivity(
                                this@AgentEmojiActivity,
                                getString(R.string.fetch_photo_success)
                            )
                        }
                    }
                }
            }
        }
    }

    // 复位回调
    val onResetCallback = object : OnResetCallback {
        override fun onStartReset() {
            Log.i(TAG, "开始复位")
            runOnUiThread {
                binding.vMoveDetect.setBackgroundResource(com.view.appview.R.color.a1_100)
            }
        }

        override fun onFinishReset() {
            Log.i(TAG, "复位完成")
            runOnUiThread {
                binding.vMoveDetect.setBackgroundResource(com.view.appview.R.color.light_blue_600)
            }
        }
    }

    //----------------生命周期----------------

    // 恢复
    override fun onResume() {
        super.onResume()
        visionManager.onResume(window = window)
        // 启动VAD录音
        chatMessageHandler.initVadCall(this@AgentEmojiActivity)
        chatMessageHandler.currentIsEmoji.set(true)
        chatMessageHandler.setCurrentVADStateChange(this)
        // 系统回调
        chatMessageHandler.setHandleSystemResponse(this)
    }

    // 暂停
    override fun onPause() {
        super.onPause()
        visionManager.onPause()
        // 关闭VAD录音
        chatMessageHandler.stopVadCall()
        chatMessageHandler.currentIsEmoji.set(false)
        // 取消系统回调
        chatMessageHandler.setHandleSystemResponse(null)
    }

    // 销毁
    override fun onDestroy() {
        super.onDestroy()
        visionManager.onDestroy(window = window)
    }

}