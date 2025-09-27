package com.magicvector.activity


import com.magicvector.databinding.ActivityChatBinding
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.ChatVm

class ChatActivity : BaseAppCompatVmActivity<ActivityChatBinding, ChatVm>(
    ChatActivity::class,
    ChatVm::class
) {
    override fun initBinding(): ActivityChatBinding {
        return ActivityChatBinding.inflate(layoutInflater)
    }

}