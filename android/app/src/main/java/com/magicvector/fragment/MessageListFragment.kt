package com.magicvector.fragment


import com.core.baseutil.BaseAppCompatFragment
import com.magicvector.databinding.FragmentMessageListBinding

class MessageListFragment : BaseAppCompatFragment<FragmentMessageListBinding>(
    MessageListFragment::class
) {

    override fun initBinding(): FragmentMessageListBinding {
        return FragmentMessageListBinding.inflate(layoutInflater)
    }

}