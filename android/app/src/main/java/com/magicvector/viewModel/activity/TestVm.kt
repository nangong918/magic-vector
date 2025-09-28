package com.magicvector.viewModel.activity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.data.domain.constant.BaseConstant
import com.data.domain.vo.test.ChatState
import com.google.gson.Gson
import com.magicvector.utils.test.SSEClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class TestVm(

) : ViewModel(){

    companion object {
        val TAG: String = TestVm::class.java.name
        val GSON = Gson()
        const val baseUrl = BaseConstant.ConstantUrl.LOCAL_URL + "/chat/stream-sse"
    }

    //---------------------------AAo Ld---------------------------

    private val customQuestion = "你好啊，你是谁？"

    //---------------------------UI State---------------------------
    private val _currentMessage = MutableStateFlow("")
    val currentMessage: StateFlow<String> = _currentMessage.asStateFlow()

    private val _chatState = MutableStateFlow<ChatState>(ChatState.Idle)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    //---------------------------NetWork---------------------------

    private val sseClient = SSEClient(
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS) // 设置读取超时时间为0，表示无限读取
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
        ,
        GSON,
        baseUrl
    )

    fun sendQuestion() {
        viewModelScope.launch {
            _chatState.value = ChatState.Loading
            _currentMessage.value = ""

            sseClient.streamChat(customQuestion)
                .collect { data ->
                    _currentMessage.value += data
                    _chatState.value = ChatState.Streaming
                    Log.i(TAG, "收到消息: $data")
                }

            _chatState.value = ChatState.Success
        }
    }

    //---------------------------Logic---------------------------

}