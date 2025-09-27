package com.magicvector.viewModel.fragment

import androidx.lifecycle.ViewModel
import com.data.domain.OnPositionItemClick
import com.data.domain.fragmentActivity.fao.MessageFAo
import com.view.appview.message.MessageContactAdapter


open class MessageVm(
) : ViewModel(){

    companion object {
        val TAG: String = MessageVm::class.java.name
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

}