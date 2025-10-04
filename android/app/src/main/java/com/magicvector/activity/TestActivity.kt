package com.magicvector.activity


import android.annotation.SuppressLint
import android.util.Log
import com.data.domain.vo.test.ChatState
import com.data.domain.vo.test.TtsChatState
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
    }

    override fun setListener() {
        super.setListener()

        binding.btnSendMessage.setOnClickListener {
            vm.sendQuestion()
        }

        binding.btnSendTTSMessage.setOnClickListener {
            vm.sendTTSQuestion()
        }

        binding.btnInitTtsAudio.setOnClickListener {
            vm.initializeAudioTrack()
        }

        observeData()
    }

    @SuppressLint("SetTextI18n")
    fun observeData(){
        // 观察tts sse消息内容
        vm.ttsSseChatMessage.observe(this) { message ->
            binding.tvTtsAIResponse.text = message
            Log.d(TAG, "更新消息: $message")
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


}