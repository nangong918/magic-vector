package com.magicvector.activity


import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.fragmentActivity.intentAo.ChatIntentAo
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

        vm.initResource(activity = this@ChatActivity, ao)

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

        vm.aao.chatMessageCountLd.observe(this){
            count ->
            if (count <= 0){
                binding.rclvMessage.visibility = View.GONE
            }
            else {
                binding.rclvMessage.visibility = View.VISIBLE
                vm.adapter.notifyDataSetChanged()
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
    }

    override fun initView() {
        super.initView()
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
    }

    override fun onDestroy() {
        super.onDestroy()

        vm.stopRealtimeChat()
    }

}