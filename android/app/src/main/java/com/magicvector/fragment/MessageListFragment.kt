package com.magicvector.fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.czy.smartmedicine.utils.BaseAppCompatVmFragment
import com.magicvector.databinding.FragmentMessageListBinding
import com.magicvector.viewModel.fragment.MessageVm

class MessageListFragment : BaseAppCompatVmFragment<
        FragmentMessageListBinding, MessageVm>(
    MessageListFragment::class,
            MessageVm::class
) {

    override fun initBinding(): FragmentMessageListBinding {
        return FragmentMessageListBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun initViewModel() {
        super.initViewModel()
    }

}