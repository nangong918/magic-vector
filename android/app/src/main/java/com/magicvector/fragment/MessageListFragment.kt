package com.magicvector.fragment


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.core.appcore.api.handler.SyncRequestCallback
import com.core.baseutil.network.networkLoad.NetworkLoadUtils
import com.magicvector.utils.BaseAppCompatVmFragment
import com.data.domain.OnPositionItemClick
import com.data.domain.fragmentActivity.intentAo.ChatIntentAo
import com.magicvector.MainApplication
import com.magicvector.activity.ChatActivity
import com.magicvector.activity.CreateAgentActivity
import com.magicvector.activity.MainActivity
import com.magicvector.callback.OnCreateAgentCallback
import com.magicvector.databinding.FragmentMessageListBinding
import com.magicvector.viewModel.fragment.MessageListVm
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
                Optional.of(MainApplication.getMessageListManager().messageContactItemAos)
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
            if (isAdded){
                if (requireActivity() is MainActivity){
                    (requireActivity() as MainActivity).turnToCreateAgent()
                }
                else {
                    Log.w("MessageListFragment", "activity is not MainActivity")
                }
            }
            else {
                Log.w("MessageListFragment", "activity is not added")
            }
        }
    }

    var createAgentLauncher: ActivityResultLauncher<Intent>? = null

    fun turnToCreateAgent(activity: FragmentActivity) {
        val intent = Intent(activity, CreateAgentActivity::class.java)
        createAgentLauncher?.launch(intent)
    }

    // activity launcher必须要在LifecycleOwner 的状态为 STARTED 或更早的状态时进行注册
    fun initCreateAgentLuncher(activity: FragmentActivity){
        createAgentLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // 如果intent 返回值中包括ok，则表明创建成功，需要进行刷新list
            val backIntent: Intent? = result.data
            if (backIntent != null) {
                val createResult: Boolean = backIntent.getBooleanExtra(
                    CreateAgentActivity::class.simpleName,
                    false
                )
                // 创建成功
                if (createResult) {
                    vm.doGetLastAgentChatList(activity, object : SyncRequestCallback {
                        override fun onThrowable(throwable: Throwable?) {
                            NetworkLoadUtils.dismissDialogSafety(activity)
                            Log.e(TAG, "initResource: onThrowable", throwable)
                        }

                        override fun onAllRequestSuccess() {
                            NetworkLoadUtils.dismissDialogSafety(activity)
                        }
                    })
                }
            }
        }
    }

    override fun onCreateAgent(createResult: Boolean) {
        if (createResult) {
            vm.doGetLastAgentChatList(requireActivity(), object : SyncRequestCallback {
                override fun onThrowable(throwable: Throwable?) {
                    NetworkLoadUtils.dismissDialogSafety(requireActivity())
                    Log.e(TAG, "initResource: onThrowable", throwable)
                }

                override fun onAllRequestSuccess() {
                    NetworkLoadUtils.dismissDialogSafety(requireActivity())
                }
            })
        }
        else {
            Log.i(TAG, "创建Agent失败/未创建")
        }
    }

}