package com.magicvector.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.core.appcore.api.handler.SyncRequestCallback
import com.core.baseutil.network.networkLoad.NetworkLoadUtils
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.permissions.PermissionUtil
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
import com.magicvector.manager.yolo.VisionCallback
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.AgentEmojiVm
import com.view.appview.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class AgentEmojiActivity : BaseAppCompatVmActivity<ActivityAgentEmojiBinding, AgentEmojiVm>(
    AgentEmojiActivity::class,
    AgentEmojiVm::class
), HandleSystemResponse, OnVadChatStateChange, VisionCallback {

    companion object {
        val GSON = MainApplication.GSON
        val udpVisionManager = MainApplication.getUdpVisionManager()
    }

    var visionManager = MainApplication.getVisionManager()

    override fun initBinding(): ActivityAgentEmojiBinding {
        return ActivityAgentEmojiBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        visionManager = MainApplication.getVisionManager()
        visionManager.setVisionCallback(this)
    }

    override fun initViewModel() {
        super.initViewModel()

        val agentId = intent.getStringExtra("agentId")
        val agentName = intent.getStringExtra("agentName")

        vm.initService()

        visionManager.initStart(
            context = this,
            previewView = binding.viewFinder,
            listener = getDetectListener(),
            lifecycleOwner = this as LifecycleOwner
        )

        agentId?.let {
            udpVisionManager.initialize(
                userId = MainApplication.getUserId(),
                agentId = agentId,
            )
            Log.i(TAG, "udpVisionManager initialize: userId = ${MainApplication.getUserId()}, agentId = $agentId")
        }

        observeData()
    }

    fun observeData(){
        vm.chatServiceBoundLd.observe(this@AgentEmojiActivity) {

        }
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
            vm.realtimeChatController?.stopVadCall()
        }
        else {
            vm.realtimeChatController?.startVadCall()
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
                    binding.tvCallState.text = getString(R.string.muted)
                }
                is VadChatState.Silent -> {
                    binding.tvCallState.text = getString(R.string.silent)
                }
                is VadChatState.Speaking -> {
                    binding.tvCallState.text = getString(R.string.user_speaking)
                }
                is  VadChatState.Replying -> {
                    binding.tvCallState.text = getString(R.string.agent_replying)
                }
                is VadChatState.Error -> {
                    binding.tvCallState.text = getString(R.string.error)
                    Log.e(TAG, "setVadChatState: ${state.message}")
                }
            }
            vadChatState = state
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

        // vision测试
        binding.btnVisionTest.setOnClickListener {
            vm.realtimeChatController?.let {
                vm.visionTest(it)
            }
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

    // 添加一个变量来保存当前帧的Bitmap
    private var currentFrameBitmap: Bitmap? = null
    override fun onReceiveCurrentFrameBitmap(bitmap: Bitmap) {
        currentFrameBitmap = bitmap
        handleFrameBitmapToUdpSend(bitmap)
    }
    // 将帧通过UDP发送给后端
    private fun handleFrameBitmapToUdpSend(bitmap: Bitmap){
        if (vadChatState == VadChatState.Speaking) {
            udpVisionManager.sendVideoFrame(bitmap)
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
            val bitmap = currentFrameBitmap
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

                vm.realtimeChatController?.realtimeChatWsClient?.sendMessage(dataMap)
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
                            bitmaps = listOf(bitmap),
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
    private fun httpUploadSingleImageVision(bitmaps: List<Bitmap>, agentId: String, userId: String, messageId: String){
        val files = VisionMcpManager.bitmapsToFlies(
            bitmaps = bitmaps,
            context = this
        )
        NetworkLoadUtils.showDialog(this)
        vm.doUploadImageVision(
            context = this,
            images = files,
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

                    vm.realtimeChatController?.realtimeChatWsClient?.sendMessage(dataMap)

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

    override fun initWindow() {
        // 隐藏标题导航栏
        supportActionBar?.hide()

        // 隐藏状态栏
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // 隐藏导航栏
        val decorView = window.decorView
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }

    // 恢复
    override fun onResume() {
        super.onResume()
        visionManager.onResume(window = window)
        vm.realtimeChatController?.let { chatMessageHandler ->
            PermissionUtil.requestPermissionSelectX(
                this@AgentEmojiActivity,
                mustPermission = arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                ),
                optionalPermission = arrayOf(
                ),
                object : GainPermissionCallback {
                    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
                    override fun allGranted() {
                        // 启动VAD录音
                        val weakContext = WeakReference<Context>(this@AgentEmojiActivity)
                        chatMessageHandler.initVadCall(weakContext)
                        chatMessageHandler.currentIsEmoji.set(true)
                        // vad状态回调
                        chatMessageHandler.setCurrentVADStateChange(this@AgentEmojiActivity)
                        // 系统回调
                        chatMessageHandler.setHandleSystemResponse(this@AgentEmojiActivity)
                    }

                    override fun notGranted(notGrantedPermissions: Array<String?>?) {
                    }

                    override fun always() {
                    }
                }
            )

        }
    }

    // 暂停
    override fun onPause() {
        super.onPause()
        visionManager.onPause()
        vm.realtimeChatController?.let { chatMessageHandler ->
            // 关闭VAD录音
            chatMessageHandler.stopVadCall()
            chatMessageHandler.currentIsEmoji.set(false)
            // 取消系统回调
            chatMessageHandler.setHandleSystemResponse(null)
        }
    }

    // 销毁
    override fun onDestroy() {
        super.onDestroy()
        visionManager.onDestroy(window = window)
        udpVisionManager.destroy()
    }

}