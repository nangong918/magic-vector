package com.magicvector.activity


import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.fragmentActivity.intentAo.ChatIntentAo
import com.data.domain.vo.test.RealtimeChatState
import com.magicvector.databinding.ActivityChatBinding
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.ChatVm
import com.view.appview.chat.OnChatMessageClick

class ChatActivity : BaseAppCompatVmActivity<ActivityChatBinding, ChatVm>(
    ChatActivity::class,
    ChatVm::class
) {
    override fun initBinding(): ActivityChatBinding {
        return ActivityChatBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initViewModel() {
        super.initViewModel()

        val intent = intent

        var ao : MessageContactItemAo? = null
        try {
            val intentAo = intent.getSerializableExtra(ChatIntentAo::class.simpleName) as ChatIntentAo
            ao = intentAo.ao
        } catch (e : Exception){
            Log.e(TAG, "ChatActivity::intentAo转换失败", e)
        }

        try {
            vm.initResource(activity = this@ChatActivity, ao, TODO())
        } catch (e: IllegalArgumentException){
            Log.e(TAG, "ChatActivity::initResource失败", e)
            finish()
        }

        // adapter
        vm.initAdapter(
            object : OnChatMessageClick{
                override fun onMessageClick(position: Int) {
                }

                override fun onAvatarClick(position: Int) {
                }

                override fun onQuoteClick(position: Int) {
                }
            }
        )

        binding.rclvMessage.adapter = vm.adapter

        // 监听设置
        binding.smSendMessage.getEditText().addTextChangedListener(
            vm.getTextWatcher()
        )

        // 图片选择器
        vm.initPictureSelectorLauncher(this)

        observeData()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun observeData(){
        vm.aao.isLoadingLd.observe(this){
            isLoading ->
            binding.progressBar.visibility = if (isLoading){
                View.VISIBLE
            }
            else {
                View.GONE
            }
        }

        // 标题
        vm.aao.nameLd.observe(this, { newName ->
            if (TextUtils.isEmpty(newName)) {
                return@observe
            }
            binding.tvTitle.text = newName
        })

        // 头像
        vm.aao.avatarUrlLd.observe(this, { newAvatarUrl ->
            if (TextUtils.isEmpty(newAvatarUrl)) {
                return@observe
            }
            vm.adapter.setCurrentAvatarUrl(newAvatarUrl)
        })

        // 录制状态
        // 状态
        vm.realtimeChatState.observe(this) { state ->
            when (state){
                is RealtimeChatState.NotInitialized -> {
                }
                is RealtimeChatState.Initializing -> {
                }
                is RealtimeChatState.InitializedConnected -> {
                    binding.vVoiceWave.visibility = View.GONE
                    binding.vVoiceWave.stop()
//                    binding.btnRecordAndSendRealtimeChat2.text = "开始录音 + 流式发送"
                }
                is RealtimeChatState.RecordingAndSending -> {
                    binding.vVoiceWave.visibility = View.VISIBLE
                    binding.vVoiceWave.start()
//                    binding.btnRecordAndSendRealtimeChat2.text = "结束录音 + 接收消息"
                }
                is RealtimeChatState.Receiving -> {
                }
                is RealtimeChatState.Disconnected -> {
                }
                is RealtimeChatState.Error -> {
                    Log.e(TAG, "realtimeChatState: Error: ${state.message}")
                }
            }
        }
    }

    override fun initView() {
        super.initView()

        binding.smSendMessage.setKeyboardOpen(true)
        
        binding.vVoiceWave.init()
    }

    override fun setListener() {
        super.setListener()

        binding.imgvBack.setOnClickListener { v -> finish() }

        // 发送消息
        binding.smSendMessage.setSendClickListener({ v ->
            vm.sendMessage()
            binding.smSendMessage.setEditMessage("")
        })

        // 图片消息：1. 选择图片
        binding.smSendMessage.setImgClickListener {
            v -> vm.beginSelectPicture(this)
        }

        // 按住发送语音与取消录制
        binding.smSendMessage.setTakAudioOnTouchListener(
            {
                // 开始录制
                vm.startRecordRealtimeChatAudio()
            },
            {
                // 结束录制
                vm.stopAndSendRealtimeChatAudio()
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        vm.stopRealtimeChat()
    }

}