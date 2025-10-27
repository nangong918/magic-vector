package com.magicvector.activity.test

import com.core.baseutil.fragmentActivity.BaseAppCompatActivity
import com.magicvector.databinding.ActivityAgentEmojiTestBinding

class AgentEmojiTestActivity : BaseAppCompatActivity<ActivityAgentEmojiTestBinding>(
    AgentEmojiTestActivity::class
) {
    override fun initBinding(): ActivityAgentEmojiTestBinding {
        return ActivityAgentEmojiTestBinding.inflate(layoutInflater)
    }

}