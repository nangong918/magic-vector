package com.magicvector.activity.test

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.permissions.PermissionUtil
import com.core.baseutil.ui.ToastUtils
import com.data.domain.vo.test.AudioRecordPlayState
import com.data.domain.vo.test.ChatState
import com.data.domain.vo.test.RealtimeChatState
import com.data.domain.vo.test.TtsChatState
import com.data.domain.vo.test.WebsocketState
import com.magicvector.databinding.ActivityTestBinding
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.TestVm

class TestActivity : BaseAppCompatVmActivity<ActivityTestBinding, TestVm>(
    TestActivity::class,
    TestVm::class
) {
    override fun initBinding(): ActivityTestBinding {
        return ActivityTestBinding.inflate(layoutInflater)
    }

    override fun initWindow() {
        binding.statusBar.layoutParams.height = getStatusBarHeight()
    }

    override fun initViewModel() {
        super.initViewModel()

        observeData()

        initVoiceWaveView()
    }

    override fun setListener() {
        super.setListener()
        // YOLOv8
        binding.btnStartToYolov8.setOnClickListener {
            val startYOLOv8Intent = Intent(this, YOLOv8Activity::class.java)

            PermissionUtil.requestPermissionSelectX(this,
                arrayOf(Manifest.permission.CAMERA),
                arrayOf(),
                object : GainPermissionCallback {
                    override fun allGranted() {
                        startActivity(startYOLOv8Intent)
                    }

                    override fun notGranted(notGrantedPermissions: Array<String?>?) {
                        ToastUtils.showToastActivity(this@TestActivity, "请允许相机权限")
                    }

                    override fun always() {
                    }
                })

        }

        // vad
        binding.btnStartToVad.setOnClickListener {
            val intent = Intent(this, VADMainActivity::class.java)
            startActivity(intent)
        }

        // realtime chat2
        binding.btnInitRealtimeChat2.setOnClickListener {
            vm.initRealtimeChat2WsClient(this)
        }

        binding.btnRecordAndSendRealtimeChat2.setOnClickListener {
            when (vm.realtimeChat2State.value) {
                is RealtimeChatState.InitializedConnected -> {
                    vm.startRecordRealtimeChatAudio2()
                }
                is RealtimeChatState.RecordingAndSending -> {
                    vm.stopAndSendRealtimeChatAudio2()
                }
                else -> {
                    Log.w(TAG, "当前状态异常: ${vm.realtimeChat2State}")
                    ToastUtils.showToastActivity(this, "当前状态异常")
                }
            }
        }

        binding.btnStopRealtimeChat2.setOnClickListener {
            vm.stopRealtimeChat2()
        }

        binding.btnRealtimeChat2SendQuestion.setOnClickListener {
            val question = binding.editRealtimeChat2.text.toString()
            if (question.isEmpty()){
                ToastUtils.showToastActivity(this, "请输入问题")
                Log.i(TAG, "请输入问题")
                return@setOnClickListener
            }
            Log.i(TAG, "question: $question")
            vm.sendQuestion(question)
            binding.editRealtimeChat2.text.clear()
        }

        // realtime chat
        binding.btnInitRealtimeChat.setOnClickListener {
            vm.initRealtimeChatWsClient(this)
        }

        binding.btnRecordAndSendRealtimeChat.setOnClickListener {
            when (vm.realtimeChatState.value) {
                is RealtimeChatState.InitializedConnected -> {
                    vm.startRecordRealtimeChatAudio()
                }
                is RealtimeChatState.RecordingAndSending -> {
                    vm.stopAndSendRealtimeChatAudio()
                }
                else -> {
                    Log.w(TAG, "当前状态异常: ${vm.realtimeChatState}")
                    ToastUtils.showToastActivity(this, "当前状态异常")
                }
            }
        }

        binding.btnStopRealtimeChat.setOnClickListener {
            vm.stopRealtimeChat()
        }

        // audioRecordPlay
        binding.btnInitRecordAudio.setOnClickListener {
            vm.initRecordAudio(this)
        }

        binding.btnBeginRecord.setOnClickListener {
            vm.beginRecordAudio(this)
        }

        binding.btnStopRecord.setOnClickListener {
            vm.stopRecordAudio()
        }

        binding.btnPlayRecord.setOnClickListener {
            vm.playRecordAudio(this)
        }

        // websocket
        binding.btnInitWebsocket.setOnClickListener {
            vm.connectWebsocket()
        }

        binding.btnSendWebsocketMessage.setOnClickListener {
            vm.sendWebsocketMessage("你好啊" + System.currentTimeMillis())
        }

        binding.btnDisconnectWebsocket.setOnClickListener {
            vm.disconnectWebsocket()
        }

        // tts sse
        binding.btnSendTTSMessage.setOnClickListener {
            vm.sendTTSQuestion()
        }

        binding.btnInitTtsAudio.setOnClickListener {
            vm.initializeAudioTrack()
        }

        // sse
        binding.btnSendMessage.setOnClickListener {
            vm.sendQuestion()
        }
    }

    @SuppressLint("SetTextI18n")
    fun observeData(){
        /// realtime chat2
        // 文本数据
        vm.realtimeChat2Message.observe(this) { chatMessage ->
            // 文本数据
            binding.tvRealTimeChatMessage2.text = chatMessage
        }
        // 状态
        vm.realtimeChat2State.observe(this) { state ->
            when (state){
                is RealtimeChatState.NotInitialized -> {
                    binding.tvRealTimeChatStatus2.text = "未初始化"
                    binding.btnInitRealtimeChat2.isEnabled = true
                    binding.btnRecordAndSendRealtimeChat2.isEnabled = false
                    binding.btnStopRealtimeChat2.isEnabled = false
                    binding.btnRealtimeChat2SendQuestion.isEnabled = false
                }
                is RealtimeChatState.Initializing -> {
                    binding.tvRealTimeChatStatus2.text = "正在初始化..."
                    binding.btnInitRealtimeChat2.isEnabled = false
                    binding.btnRecordAndSendRealtimeChat2.isEnabled = false
                    binding.btnStopRealtimeChat2.isEnabled = false
                    binding.btnRealtimeChat2SendQuestion.isEnabled = false
                }
                is RealtimeChatState.InitializedConnected -> {
                    binding.tvRealTimeChatStatus2.text = "已初始化并且已经连接"
                    binding.btnInitRealtimeChat2.isEnabled = false
                    binding.btnRecordAndSendRealtimeChat2.isEnabled = true
                    binding.btnStopRealtimeChat2.isEnabled = true
                    binding.btnRealtimeChat2SendQuestion.isEnabled = true

                    // btn change
                    binding.btnRecordAndSendRealtimeChat2.text = "开始录音 + 流式发送"
                }
                is RealtimeChatState.RecordingAndSending -> {
                    binding.tvRealTimeChatStatus2.text = "正在录音..."
                    binding.btnInitRealtimeChat2.isEnabled = false
                    binding.btnRecordAndSendRealtimeChat2.isEnabled = true
                    binding.btnStopRealtimeChat2.isEnabled = true
                    binding.btnRealtimeChat2SendQuestion.isEnabled = false

                    // btn change
                    binding.btnRecordAndSendRealtimeChat2.text = "结束录音 + 接收消息"
                }
                is RealtimeChatState.Receiving -> {
                    binding.tvRealTimeChatStatus2.text = "正在接收..."
                    binding.btnInitRealtimeChat2.isEnabled = false
                    binding.btnRecordAndSendRealtimeChat2.isEnabled = false
                    binding.btnStopRealtimeChat2.isEnabled = true
                    binding.btnRealtimeChat2SendQuestion.isEnabled = false
                }
                is RealtimeChatState.Disconnected -> {
                    binding.tvRealTimeChatStatus2.text = "已断开"
                    binding.btnInitRealtimeChat2.isEnabled = true
                    binding.btnRecordAndSendRealtimeChat2.isEnabled = false
                    binding.btnStopRealtimeChat2.isEnabled = false
                    binding.btnRealtimeChat2SendQuestion.isEnabled = false
                }
                is RealtimeChatState.Error -> {
                    binding.tvRealTimeChatStatus2.text = "错误: ${state.message}"
                    binding.btnInitRealtimeChat2.isEnabled = true
                    binding.btnRecordAndSendRealtimeChat2.isEnabled = false
                    binding.btnStopRealtimeChat2.isEnabled = false
                    binding.btnRealtimeChat2SendQuestion.isEnabled = false
                }
            }
        }
        // 音量大小
        vm.realtimeChat2Volume.observe(this) {
            Log.i(TAG, "realtimeChatVolume更新: $it")
            binding.vRealTimeChatVoiceWave2.setVolume(it)
        }


        /// realtime chat
        // 文本数据
        vm.realtimeChatMessage.observe(this) { chatMessage ->
            // 文本数据
            binding.tvRealTimeChatMessage.text = chatMessage
        }
        // 状态
        vm.realtimeChatState.observe(this) { state ->
            when (state){
                is RealtimeChatState.NotInitialized -> {
                    binding.tvRealTimeChatStatus.text = "未初始化"
                    binding.btnInitRealtimeChat.isEnabled = true
                    binding.btnRecordAndSendRealtimeChat.isEnabled = false
                    binding.btnStopRealtimeChat.isEnabled = false
                }
                is RealtimeChatState.Initializing -> {
                    binding.tvRealTimeChatStatus.text = "正在初始化..."
                    binding.btnInitRealtimeChat.isEnabled = false
                    binding.btnRecordAndSendRealtimeChat.isEnabled = false
                    binding.btnStopRealtimeChat.isEnabled = false
                }
                is RealtimeChatState.InitializedConnected -> {
                    binding.tvRealTimeChatStatus.text = "已初始化并且已经连接"
                    binding.btnInitRealtimeChat.isEnabled = false
                    binding.btnRecordAndSendRealtimeChat.isEnabled = true
                    binding.btnStopRealtimeChat.isEnabled = true

                    // btn change
                    binding.btnRecordAndSendRealtimeChat.text = "开始录音 + 流式发送"
                }
                is RealtimeChatState.RecordingAndSending -> {
                    binding.tvRealTimeChatStatus.text = "正在录音..."
                    binding.btnInitRealtimeChat.isEnabled = false
                    binding.btnRecordAndSendRealtimeChat.isEnabled = true
                    binding.btnStopRealtimeChat.isEnabled = true

                    // btn change
                    binding.btnRecordAndSendRealtimeChat.text = "结束录音 + 接收消息"
                }
                is RealtimeChatState.Receiving -> {
                    binding.tvRealTimeChatStatus.text = "正在接收..."
                    binding.btnInitRealtimeChat.isEnabled = false
                    binding.btnRecordAndSendRealtimeChat.isEnabled = false
                    binding.btnStopRealtimeChat.isEnabled = true
                }
                is RealtimeChatState.Disconnected -> {
                    binding.tvRealTimeChatStatus.text = "已断开"
                    binding.btnInitRealtimeChat.isEnabled = true
                    binding.btnRecordAndSendRealtimeChat.isEnabled = false
                    binding.btnStopRealtimeChat.isEnabled = false
                }
                is RealtimeChatState.Error -> {
                    binding.tvRealTimeChatStatus.text = "错误: ${state.message}"
                    binding.btnInitRealtimeChat.isEnabled = true
                    binding.btnRecordAndSendRealtimeChat.isEnabled = false
                    binding.btnStopRealtimeChat.isEnabled = false
                }
            }
        }
        // 音量大小
        vm.realtimeChatVolume.observe(this) {
            Log.i(TAG, "realtimeChatVolume更新: $it")
            binding.vRealTimeChatVoiceWave.setVolume(it)
        }


        /// 录音播放
        // 状态
        vm.audioRecordPlayState.observe(this){ state ->
            Log.d(TAG, "audioRecordPlayState更新状态: $state")

            when (state) {
                // 未初始化
                is AudioRecordPlayState.NotInitialized -> {
                    binding.tvRecordAudioStatus.text = "未初始化"
                    binding.btnInitRecordAudio.isEnabled = true
                    binding.btnBeginRecord.isEnabled = false
                    binding.btnStopRecord.isEnabled = false
                    binding.btnPlayRecord.isEnabled = false
                }
                // 正在初始化
                is AudioRecordPlayState.Initializing -> {
                    binding.tvRecordAudioStatus.text = "正在初始化..."
                    binding.btnInitRecordAudio.isEnabled = false
                    binding.btnBeginRecord.isEnabled = false
                    binding.btnStopRecord.isEnabled = false
                    binding.btnPlayRecord.isEnabled = false
                }
                // 就绪
                is AudioRecordPlayState.Ready -> {
                    binding.tvRecordAudioStatus.text = "就绪"
                    binding.btnInitRecordAudio.isEnabled = false
                    binding.btnBeginRecord.isEnabled = true
                    binding.btnStopRecord.isEnabled = false
                    binding.btnPlayRecord.isEnabled = false
                }
                // 正在录音
                is AudioRecordPlayState.Recording -> {
                    binding.tvRecordAudioStatus.text = "正在录音..."
                    binding.btnInitRecordAudio.isEnabled = false
                    binding.btnBeginRecord.isEnabled = false
                    binding.btnStopRecord.isEnabled = true
                    binding.btnPlayRecord.isEnabled = false

                }
                // 录音结束，可播放
                is AudioRecordPlayState.RecordedAndPlayable -> {
                    binding.tvRecordAudioStatus.text = "录音结束，可播放:\n ${state.recordMessage}"
                    binding.btnInitRecordAudio.isEnabled = false
                    binding.btnBeginRecord.isEnabled = false
                    binding.btnStopRecord.isEnabled = false
                    binding.btnPlayRecord.isEnabled = true
                }
                // 正在播放
                is AudioRecordPlayState.Playing -> {
                    binding.tvRecordAudioStatus.text = "正在播放..."
                    binding.btnInitRecordAudio.isEnabled = false
                    binding.btnBeginRecord.isEnabled = false
                    binding.btnStopRecord.isEnabled = false
                    binding.btnPlayRecord.isEnabled = false
                }
                // 播放结束
                is AudioRecordPlayState.PlayedEnd -> {
                    binding.tvRecordAudioStatus.text = "播放结束"
                    binding.btnInitRecordAudio.isEnabled = true
                    binding.btnBeginRecord.isEnabled = false
                    binding.btnStopRecord.isEnabled = false
                    binding.btnPlayRecord.isEnabled = false
                }
                // 错误
                is AudioRecordPlayState.Error -> {
                    binding.tvRecordAudioStatus.text = "错误: ${state.message}"
                    binding.btnInitRecordAudio.isEnabled = true
                    binding.btnBeginRecord.isEnabled = false
                    binding.btnStopRecord.isEnabled = false
                    binding.btnPlayRecord.isEnabled = false
                }
            }
        }

        // 音量
        vm.audioRecordVolume.observe(this){ volume ->
            Log.i(TAG, "音量: $volume")

            binding.vVoiceWave.setVolume(volume)
        }

        /// websocket
        // 观察websocket聊天记录
        vm.websocketAllMessage.observe(this){ message ->
            Log.d(TAG, "websocketAllMessage更新消息: $message")
            binding.tvWebsocketMessageHistory.text = message
        }
        // 观察websocket聊天状态
        binding.tvWebsocketStatus.text = "未初始化"
        binding.btnInitWebsocket.isEnabled = true
        binding.btnSendWebsocketMessage.isEnabled = false
        binding.btnDisconnectWebsocket.isEnabled = false
        vm.websocketState.observe(this) { state ->
            Log.i(TAG, "websocketState更新状态: $state")
            when (state) {
                is WebsocketState.NotInitialized -> {
                    binding.tvWebsocketStatus.text = "未初始化"
                    binding.btnInitWebsocket.isEnabled = true
                    binding.btnSendWebsocketMessage.isEnabled = false
                    binding.btnDisconnectWebsocket.isEnabled = false
                }
                is WebsocketState.Initializing -> {
                    binding.tvWebsocketStatus.text = "初始化中..."
                    binding.btnInitWebsocket.isEnabled = false
                    binding.btnSendWebsocketMessage.isEnabled = false
                    binding.btnDisconnectWebsocket.isEnabled = false
                }
                is WebsocketState.InitializedNotConnected -> {
                    binding.tvWebsocketStatus.text = "未连接"
                    binding.btnInitWebsocket.isEnabled = false
                    binding.btnSendWebsocketMessage.isEnabled = true
                    binding.btnDisconnectWebsocket.isEnabled = false
                }
                is WebsocketState.Connected -> {
                    binding.tvWebsocketStatus.text = "已连接"
                    binding.btnInitWebsocket.isEnabled = false
                    binding.btnSendWebsocketMessage.isEnabled = true
                    binding.btnDisconnectWebsocket.isEnabled = true
                }
                is WebsocketState.Sending -> {
                    binding.tvWebsocketStatus.text = "正在发送消息..."
                    binding.btnInitWebsocket.isEnabled = false
                    binding.btnSendWebsocketMessage.isEnabled = false
                    binding.btnDisconnectWebsocket.isEnabled = true
                }
                is WebsocketState.Receiving -> {
                    binding.tvWebsocketStatus.text = "正在接收消息..."
                    binding.btnInitWebsocket.isEnabled = false
                    binding.btnSendWebsocketMessage.isEnabled = true
                    binding.btnDisconnectWebsocket.isEnabled = true
                }
                is WebsocketState.Disconnected -> {
                    binding.tvWebsocketStatus.text = "已断开连接"
                    binding.btnInitWebsocket.isEnabled = true
                    binding.btnSendWebsocketMessage.isEnabled = false
                    binding.btnDisconnectWebsocket.isEnabled = false
                }
                is WebsocketState.Error -> {
                    binding.tvWebsocketStatus.text = "错误: ${state.message}"
                    binding.btnInitWebsocket.isEnabled = true
                    binding.btnSendWebsocketMessage.isEnabled = false
                    binding.btnDisconnectWebsocket.isEnabled = false
                }
            }
        }


        /// sse
        // 观察tts sse消息内容
        vm.ttsSseChatMessage.observe(this) { message ->
            binding.tvTtsAIResponse.text = message
            Log.d(TAG, "ttsSseChatMessage更新消息: $message")
        }

        // 观察tts sse聊天状态
        vm.ttsSseChatState.observe(this) { state ->
            when (state) {
                is TtsChatState.NotInitialized -> {
                    binding.tvTtsSseStatus.text = "未初始化"
                    binding.btnSendTTSMessage.isEnabled = false
                    binding.btnInitTtsAudio.isEnabled = true
                }
                is TtsChatState.Initializing -> {
                    binding.tvTtsSseStatus.text = "初始化中..."
                    binding.btnSendTTSMessage.isEnabled = false
                    binding.btnInitTtsAudio.isEnabled = false
                }
                is TtsChatState.InitializationFailed -> {
                    binding.tvTtsSseStatus.text = "初始化失败: ${state.message}"
                    binding.btnSendTTSMessage.isEnabled = false
                    binding.btnInitTtsAudio.isEnabled = true
                }
                is TtsChatState.Idle -> {
                    binding.tvTtsSseStatus.text = "Android端就绪"
                    binding.btnSendTTSMessage.isEnabled = true
                    binding.btnInitTtsAudio.isEnabled = false
                }
                is TtsChatState.Loading -> {
                    binding.tvTtsSseStatus.text = "连接中..."
                    binding.btnSendTTSMessage.isEnabled = false
                    binding.btnInitTtsAudio.isEnabled = false
                }
                is TtsChatState.Streaming -> {
                    binding.tvTtsSseStatus.text = "接收中..."
                    binding.btnSendTTSMessage.isEnabled = false
                    binding.btnInitTtsAudio.isEnabled = false
                }
                is TtsChatState.Success -> {
                    binding.tvTtsSseStatus.text = "对话完成"
                    binding.btnSendTTSMessage.isEnabled = true
                    binding.btnInitTtsAudio.isEnabled = true
//                    showToast("对话完成")
                }
                is TtsChatState.Error -> {
                    binding.tvTtsSseStatus.text = "错误: ${state.message}"
                    binding.btnSendTTSMessage.isEnabled = true
                    binding.btnInitTtsAudio.isEnabled = true
//                    showToast("发生错误: ${state.message}")
                }
            }
        }


        /// tts sse
        // 观察sse消息内容
        vm.sseChatMessage.observe(this) { message ->
            binding.tvAIResponse.text = message
            Log.d(TAG, "更新消息: $message")
        }

        // 观察sse聊天状态
        vm.sseChatState.observe(this) { state ->
            when (state) {
                is ChatState.Idle -> {
                    binding.tvSseStatus.text = "Android端就绪"
                    binding.btnSendMessage.isEnabled = true
                }
                is ChatState.Loading -> {
                    binding.tvSseStatus.text = "连接中..."
                    binding.btnSendMessage.isEnabled = false
                }
                is ChatState.Streaming -> {
                    binding.tvSseStatus.text = "接收中..."
                    binding.btnSendMessage.isEnabled = false
                }
                is ChatState.Success -> {
                    binding.tvSseStatus.text = "对话完成"
                    binding.btnSendMessage.isEnabled = true
//                    showToast("对话完成")
                }
                is ChatState.Error -> {
                    binding.tvSseStatus.text = "错误: ${state.message}"
                    binding.btnSendMessage.isEnabled = true
//                    showToast("发生错误: ${state.message}")
                }
            }
        }


    }

    private fun initVoiceWaveView() {
        binding.vVoiceWave.apply {
            init()
            setVolume(0f)
        }

        binding.vRealTimeChatVoiceWave.apply {
            init()
            setVolume(0f)
        }

        binding.vRealTimeChatVoiceWave2.apply {
            init()
            setVolume(0f)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.vVoiceWave.stop()

    }
}