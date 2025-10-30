package com.magicvector.activity

import android.os.Bundle
import com.magicvector.databinding.ActivityAgentEmojiBinding
import com.magicvector.utils.BaseAppCompatVmActivity
import com.magicvector.viewModel.activity.AgentEmojiVm

class AgentEmojiActivity : BaseAppCompatVmActivity<ActivityAgentEmojiBinding, AgentEmojiVm>(
    AgentEmojiActivity::class,
    AgentEmojiVm::class
) {
    override fun initBinding(): ActivityAgentEmojiBinding {
        return ActivityAgentEmojiBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun initViewModel() {
        super.initViewModel()
    }

    override fun initView() {
        super.initView()
    }

    override fun setListener() {
        super.setListener()
    }

}