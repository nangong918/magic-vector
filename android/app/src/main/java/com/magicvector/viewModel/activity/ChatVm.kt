package com.magicvector.viewModel.activity

import androidx.lifecycle.ViewModel
import com.data.domain.constant.ao.MessageContactItemAo
import com.data.domain.constant.fragmentActivity.aao.ChatAAo

class ChatVm(

) : ViewModel(){

    companion object {
        val TAG: String = ChatVm::class.java.name
    }

    //---------------------------AAo Ld---------------------------

    val aao = ChatAAo()

    fun initAAo(messageContactItemAo : MessageContactItemAo?){
        aao.messageContactItemAo = messageContactItemAo
    }

    //---------------------------NetWork---------------------------

    //---------------------------Logic---------------------------

}