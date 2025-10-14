package com.magicvector.viewModel.fragment

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import com.data.domain.OnPositionItemClick
import com.data.domain.fragmentActivity.fao.MessageFAo
import com.magicvector.activity.CreateAgentActivity
import com.view.appview.message.MessageContactAdapter


open class MessageListVm(
) : ViewModel(){

    companion object {
        val TAG: String = MessageListVm::class.java.name
    }

    fun initResource(activity: FragmentActivity){
        initCreateAgentLuncher(activity)
    }

    //---------------------------FAo Ld---------------------------

    lateinit var adapter : MessageContactAdapter

    val fao = MessageFAo()

    fun initFAo(){
        // 后续缓存的数据会加载到此处
    }

    fun initAdapter(onPositionItemClick : OnPositionItemClick){
        adapter = MessageContactAdapter(
            fao.messageContactList,
            onPositionItemClick
        )
    }

    //---------------------------NetWork---------------------------

    //---------------------------Logic---------------------------

    var createAgentLauncher: ActivityResultLauncher<Intent>? = null

    fun turnToCreateAgent(activity: FragmentActivity) {
        val intent = Intent(activity, CreateAgentActivity::class.java)
        createAgentLauncher?.launch(intent)
    }

    fun initCreateAgentLuncher(activity: FragmentActivity){
        createAgentLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            // 如果intent 返回值中包括ok，则表明创建成功，需要进行刷新list
            val backIntent: Intent? = result.data
            if (backIntent != null){
                val createResult: Boolean = backIntent.getBooleanExtra(
                    CreateAgentActivity::class.simpleName,
                    false
                )
                // 创建成功
                if (createResult) {
                    // todo 网络请求刷新列表
                }
            }
        }
    }

}