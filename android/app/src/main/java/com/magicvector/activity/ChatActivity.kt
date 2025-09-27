package com.magicvector.activity


import android.os.Bundle
import android.util.Log
import android.view.View
import com.data.domain.ao.message.MessageContactItemAo
import com.data.domain.fragmentActivity.intentAo.ChatIntentAo
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

        vm.initAAo(ao)

        // adapter

        // 监听设置
        binding.smSendMessage.getEditText().addTextChangedListener(
            vm.getTextWatcher()
        )

        observeData()
    }

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
    }

    override fun initView() {
        super.initView()
    }

    override fun setListener() {
        super.setListener()
    }

}