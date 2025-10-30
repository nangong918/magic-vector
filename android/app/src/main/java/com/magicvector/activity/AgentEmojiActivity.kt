package com.magicvector.activity

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.data.domain.constant.BaseConstant
import com.data.domain.constant.chat.RealtimeRequestDataTypeEnum
import com.data.domain.constant.chat.RealtimeSystemEventEnum
import com.data.domain.dto.ws.request.UploadPhotoRequest
import com.data.domain.vo.test.RealtimeChatState
import com.detection.yolov8.BoundingBox
import com.detection.yolov8.Detector
import com.detection.yolov8.targetPoint.YOLOv8TargetPointGenerator
import com.magicvector.MainApplication
import com.magicvector.databinding.ActivityAgentEmojiBinding
import com.magicvector.manager.mcp.HandleSystemResponse
import com.magicvector.manager.mcp.VisionMcpManager
import com.magicvector.manager.yolo.EyesMoveManager
import com.magicvector.manager.yolo.OnResetCallback
import com.magicvector.manager.yolo.TargetActivityDetectionManager
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.AgentEmojiVm

class AgentEmojiActivity : BaseAppCompatVmActivity<ActivityAgentEmojiBinding, AgentEmojiVm>(
    AgentEmojiActivity::class,
    AgentEmojiVm::class
), HandleSystemResponse {

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
        chatMessageHandler.realtimeChatState.observe(this) {
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
        }
    }

    override fun initView() {
        super.initView()
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

    override fun handleSystemResponse(map: Map<String, String>) {
        val agentId = map["agentId"]
        val userId = MainApplication.getUserId()
        val messageId = map["messageId"]
        // 上传图片的指令
        if (map["event"] == RealtimeSystemEventEnum.UPLOAD_PHOTO.code) {
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
            }
            else {
                // bitmap -> base64
                val base64Str = VisionMcpManager.bitmapToBase64(
                    bitmap = bitmap
                )

                val uploadPhotoRequest = UploadPhotoRequest()
                uploadPhotoRequest.agentId = agentId
                uploadPhotoRequest.userId = userId
                uploadPhotoRequest.messageId = messageId
                uploadPhotoRequest.isHavePhoto = true
                uploadPhotoRequest.photoBase64 = base64Str

                val uploadPhotoRequestJson = GSON.toJson(uploadPhotoRequest)
                val dataMap = mapOf(
                    RealtimeRequestDataTypeEnum.TYPE to RealtimeRequestDataTypeEnum.SYSTEM_MESSAGE.type,
                    RealtimeRequestDataTypeEnum.DATA to uploadPhotoRequestJson
                )

                chatMessageHandler.realtimeChatWsClient?.sendMessage(dataMap)
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
    }

    // 暂停
    override fun onPause() {
        super.onPause()
        visionManager.onPause()
        // 关闭VAD录音
        chatMessageHandler.stopVadCall()
        chatMessageHandler.currentIsEmoji.set(false)
    }

    // 销毁
    override fun onDestroy() {
        super.onDestroy()
        visionManager.onDestroy(window = window)
    }

}