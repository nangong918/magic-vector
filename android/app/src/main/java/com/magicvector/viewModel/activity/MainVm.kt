package com.magicvector.viewModel.activity

import androidx.lifecycle.ViewModel
import com.magicvector.manager.ChatMessageHandler

class MainVm (

) : ViewModel(){

    companion object {
        val TAG: String = MainVm::class.java.name
    }

    var chatMessageHandler : ChatMessageHandler? = null
}