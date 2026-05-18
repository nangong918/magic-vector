package com.vectordemo.viewModel

import com.vectordemo.viewModel.activity.LoginVm
import com.vectordemo.viewModel.activity.MainVm
import com.vectordemo.viewModel.activity.RegisterVm
import com.vectordemo.viewModel.activity.StartVm
import com.vectordemo.viewModel.chat.ChatListVm
import com.vectordemo.viewModel.oss.OssDemoVm
import com.vectordemo.viewModel.voice.VoiceAgentVm

object AppVmStore {
    val startVm: StartVm = StartVm()
    val loginVm: LoginVm = LoginVm()
    val registerVm: RegisterVm = RegisterVm()
    val mainVm: MainVm = MainVm()
    val chatListVm: ChatListVm = ChatListVm()
    val ossDemoVm: OssDemoVm = OssDemoVm()
    val voiceAgentVm: VoiceAgentVm = VoiceAgentVm()
}
