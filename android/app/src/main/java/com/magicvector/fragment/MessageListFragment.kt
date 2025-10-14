package com.magicvector.fragment


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.czy.smartmedicine.utils.BaseAppCompatVmFragment
import com.data.domain.OnPositionItemClick
import com.data.domain.fragmentActivity.intentAo.ChatIntentAo
import com.magicvector.activity.ChatActivity
import com.magicvector.databinding.FragmentMessageListBinding
import com.magicvector.viewModel.fragment.MessageListVm
import java.util.Optional

class MessageListFragment : BaseAppCompatVmFragment<
        FragmentMessageListBinding, MessageListVm>(
    MessageListFragment::class,
            MessageListVm::class
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

        vm.initResource(requireActivity())
        vm.initFAo()

        vm.initAdapter(object : OnPositionItemClick {
            override fun onPositionItemClick(position: Int) {
                Optional.of(vm.fao.messageContactList)
                    .filter { it -> it.size > position }
                    .ifPresent {
                        it ->
                        val intentAo = ChatIntentAo()
                        intentAo.ao = it[position]

                        vm.startChatActivity(requireActivity()) {
                            val intent = Intent(activity, ChatActivity::class.java)
                            intent.putExtra(ChatIntentAo::class.simpleName, intentAo)
                            startActivity(intent)
                        }
                    }
            }
        })

        binding.rclvMessage.adapter = vm.adapter

        observeData()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun observeData(){
        vm.fao.messageContactCountLd.observe(viewLifecycleOwner){
            countLd ->
            if (countLd <= 0){
                binding.rclvMessage .visibility = View.GONE
                binding.lyHaveNoMessage.visibility = View.VISIBLE
            }
            else {
                binding.rclvMessage.visibility = View.VISIBLE
                binding.lyHaveNoMessage.visibility = View.GONE
                vm.adapter.notifyDataSetChanged()
            }
        }
    }

    override fun setListener() {
        super.setListener()

        binding.fbtnCreateAgent.setOnClickListener {
            vm.turnToCreateAgent(requireActivity())
        }
    }

}