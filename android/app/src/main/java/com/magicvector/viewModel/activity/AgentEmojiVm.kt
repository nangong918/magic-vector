package com.magicvector.viewModel.activity

import androidx.lifecycle.ViewModel
import com.magicvector.MainApplication

class AgentEmojiVm(

) : ViewModel(){

    companion object {
        val TAG: String = AgentEmojiVm::class.java.name
        val GSON = MainApplication.GSON
    }

}