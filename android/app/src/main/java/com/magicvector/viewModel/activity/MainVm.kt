package com.magicvector.viewModel.activity

import androidx.lifecycle.ViewModel
import com.magicvector.manager.RealtimeChatController

class MainVm (

) : ViewModel(){

    companion object {
        val TAG: String = MainVm::class.java.name
    }

    var realtimeChatController : RealtimeChatController? = null
}