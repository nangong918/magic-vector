package com.vectordemo.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.ui.view.chat.ChatListScreen
import com.vectordemo.utils.activity.BaseComponentActivity
import com.vectordemo.viewModel.chat.ChatListVm

class ChatListActivity : BaseComponentActivity() {
    private val vm: ChatListVm by viewModels { ChatListVm.factory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        vm.initialize()
        setContent {
            VectorDemoTheme {
                val state by vm.uiState.collectAsState()
                ChatListScreen(
                    state = state,
                    onBack = { finish() },
                    onSend = { vm.sendMessage(it) }
                )
            }
        }
    }
}

