package com.magicvector.activity


import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.core.baseutil.ui.ToastUtils
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.fragmentActivity.intentAo.ChatIntentAo
import com.data.domain.vo.test.RealtimeChatState
import com.magicvector.databinding.ActivityChatBinding
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.ChatVm
import com.view.appview.chat.OnChatMessageClick
import com.view.appview.recycler.RecyclerViewWhereNeedUpdate
import com.view.appview.recycler.UpdateRecyclerViewItem
import com.view.appview.recycler.UpdateRecyclerViewTypeEnum

class ChatActivity : BaseAppCompatVmActivity<ActivityChatBinding, ChatVm>(
    ChatActivity::class,
    ChatVm::class
) {

    private val tag = ChatActivity::class.simpleName

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
            vm.initResource(activity = this@ChatActivity, ao, getWhereNeedUpdate())
        } catch (e: IllegalArgumentException){
            Log.e(TAG, "ChatActivity::initResource失败", e)
            ToastUtils.showToastActivity(this@ChatActivity,
                getString(com.view.appview.R.string.init_agent_failed))
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
            vm.sendMessage(this@ChatActivity)
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

    fun getWhereNeedUpdate(): RecyclerViewWhereNeedUpdate {
        return object : RecyclerViewWhereNeedUpdate {
            override fun whereNeedUpdate(updateInfos: List<UpdateRecyclerViewItem>) {
                if (updateInfos.isEmpty()){
                    Log.d(tag, "whereNeedUpdate: updateInfos is empty")
                    return
                }
                Log.d(tag, "当前的chatList长度为：${vm.chatManagerPointer.getViewChatMessageList().size}")
                for (updateInfo in updateInfos) {
                    when (updateInfo.type){
                        // 单个覆盖更新
                        UpdateRecyclerViewTypeEnum.SINGLE_ID_UPDATE -> {
                            if (updateInfo.singleUpdateId == null){
                                Log.w(TAG, "whereNeedUpdate: singleUpdateId is null")
                                return
                            }
                            // 单个更新
                            // 找到id当前的position然后更新
                            var viewIndex = -1
                            for (chatItemAo in vm.chatManagerPointer.getViewChatMessageList()){
                                if (chatItemAo.messageId == updateInfo.singleUpdateId){
                                    viewIndex = vm.chatManagerPointer.getViewChatMessageList().indexOf(chatItemAo)
                                }
                            }
                            if (viewIndex != -1){
                                vm.adapter.notifyItemChanged(viewIndex)
                                Log.d(tag, "whereNeedUpdate: SINGLE_ID_UPDATE: $viewIndex")
                            }
                            else {
                                Log.w(tag, "whereNeedUpdate: SINGLE_ID_UPDATE: not found")
                            }
                        }
                        UpdateRecyclerViewTypeEnum.ID_TO_END_UPDATE -> {
                            if (updateInfo.idToEndUpdateId == null){
                                Log.w(tag, "whereNeedUpdate: idToEndUpdateId is null")
                                return
                            }
                            // 找到id当前的position然后更新
                            var viewIndex = -1
                            for (chatItemAo in vm.chatManagerPointer.getViewChatMessageList()){
                                if (chatItemAo.messageId == updateInfo.idToEndUpdateId){
                                    viewIndex = vm.chatManagerPointer.getViewChatMessageList().indexOf(chatItemAo)
                                }
                            }
                            if (viewIndex >= 0){
                                val endPosition = vm.chatManagerPointer.getViewChatMessageList().size - 1
                                vm.adapter.notifyItemRangeChanged(
                                    viewIndex,
                                    endPosition
                                )
                                Log.d(tag, "whereNeedUpdate: ID_TO_END_UPDATE 范围: [$viewIndex, $endPosition]")
                            }
                            else {
                                Log.w(tag, "whereNeedUpdate: ID_TO_END_UPDATE: not found")
                            }
                        }
                        // 单个插入
                        UpdateRecyclerViewTypeEnum.SINGLE_ID_INSERT -> {
                            if (updateInfo.singleInsertId == null){
                                Log.w(tag, "whereNeedUpdate: singleInsertId is null")
                                return
                            }
                            // 找到id当前的position然后更新
                            var viewIndex = -1
                            for (chatItemAo in vm.chatManagerPointer.getViewChatMessageList()){
                                if (chatItemAo.messageId == updateInfo.idToEndUpdateId){
                                    viewIndex = vm.chatManagerPointer.getViewChatMessageList().indexOf(chatItemAo)
                                }
                            }
                            if (viewIndex >= 0) {
                                vm.adapter.notifyItemInserted(viewIndex)
                                Log.d(tag, "whereNeedUpdate: SINGLE_ID_INSERT: $viewIndex")
                            }
                            else {
                                Log.w(tag, "whereNeedUpdate: SINGLE_ID_INSERT: not found")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        vm.stopRealtimeChat()
    }

}