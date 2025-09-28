package com.magicvector.activity


import android.util.Log
import com.data.domain.vo.test.ChatState
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

    override fun initViewModel() {
        super.initViewModel()
    }

    override fun setListener() {
        super.setListener()

        binding.btnSendMessage.setOnClickListener {
            vm.sendQuestion()
        }

        observeData()
    }

    fun observeData(){
        // 观察消息内容
        vm.currentMessage.observe(this) { message ->
            binding.tvAIResponse.text = message
            Log.d(TAG, "更新消息: $message")
        }

        // 观察聊天状态
        vm.chatState.observe(this) { state ->
            when (state) {
                is ChatState.Idle -> {
                    updateUIState("就绪", false)
                    binding.btnSendMessage.isEnabled = true
                }
                is ChatState.Loading -> {
                    updateUIState("连接中...", true)
                    binding.btnSendMessage.isEnabled = false
                }
                is ChatState.Streaming -> {
                    updateUIState("接收中...", false)
                    binding.btnSendMessage.isEnabled = false
                }
                is ChatState.Success -> {
                    updateUIState("对话完成", false)
                    binding.btnSendMessage.isEnabled = true
//                    showToast("对话完成")
                }
                is ChatState.Error -> {
                    updateUIState("错误: ${state.message}", false)
                    binding.btnSendMessage.isEnabled = true
//                    showToast("发生错误: ${state.message}")
                }
            }
        }
    }

    private fun updateUIState(status: String, showProgress: Boolean) {
        binding.tvSseStatus.text = status
    }

}