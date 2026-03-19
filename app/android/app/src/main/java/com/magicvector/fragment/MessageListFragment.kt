package com.magicvector.fragment


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.core.baseutil.network.networkLoad.NetworkLoadUtils
import com.data.domain.OnPositionItemClick
import com.data.domain.fragmentActivity.intentAo.ChatIntentAo
import com.magicvector.MainApplication
import com.magicvector.activity.ComposeAgentChatActivity
import com.magicvector.callback.OnCreateAgentCallback
import com.magicvector.databinding.FragmentMessageListBinding
import com.magicvector.utils.BaseAppCompatVmFragment
import com.magicvector.viewModel.fragment.MessageListVm
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Optional

class MessageListFragment : BaseAppCompatVmFragment<
        FragmentMessageListBinding, MessageListVm>(
    MessageListFragment::class,
            MessageListVm::class
),OnCreateAgentCallback {

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
                Optional.of(MainApplication.getMessageListManager().messageContactItemModels)
                    .filter { it -> it.size > position }
                    .ifPresent {
                        it ->
                        val intentAo = ChatIntentAo()
                        intentAo.ao = it[position]

                        vm.startChatActivity(requireActivity()) {
                            val intent = Intent(activity, ComposeAgentChatActivity::class.java)
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

//        binding.fbtnCreateAgent.setOnClickListener {
//            if (isAdded){
//                if (requireActivity() is MainActivity){
//                    (requireActivity() as MainActivity).turnToCreateAgent()
//                }
//                else {
//                    Log.w("MessageListFragment", "activity is not MainActivity")
//                }
//            }
//            else {
//                Log.w("MessageListFragment", "activity is not added")
//            }
//        }

        binding.layoutMain.setOnRefreshListener{
            if (isAdded) {
                lifecycleScope.launch {
                    runCatching { vm.fetchLastAgentChatList() }
                        .onFailure { Log.e(TAG, "initResource: onThrowable", it) }
                    requireActivity().let {
                        NetworkLoadUtils.dismissDialogSafety(it)
                        it.runOnUiThread {
                            binding.layoutMain.isRefreshing = false
                        }
                    }
                }
            }
            else {
                Log.w("MessageListFragment", "activity is not added")
            }
        }
    }

    override fun onCreateAgent(createResult: Boolean) {
        if (createResult) {
            lifecycleScope.launch {
                runCatching { vm.fetchLastAgentChatList() }
                    .onFailure { Log.e(TAG, "initResource: onThrowable", it) }
                NetworkLoadUtils.dismissDialogSafety(requireActivity())
            }
        }
        else {
            Log.i(TAG, "创建Agent失败/未创建")
        }
    }

}