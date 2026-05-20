package com.vectordemo.viewModel

import com.vectordemo.di.AppContainer
import com.vectordemo.viewModel.activity.LoginVm
import com.vectordemo.viewModel.activity.MainVm
import com.vectordemo.viewModel.activity.RegisterVm
import com.vectordemo.viewModel.activity.StartVm
import com.vectordemo.viewModel.chat.ChatListVm
import com.vectordemo.viewModel.oss.OssDemoVm
import com.vectordemo.viewModel.voice.VoiceAgentVm
import com.vectordemo.viewModel.wechat.WeChatDemoVm

object AppViewModelFactory {
    fun startVm(): StartVm = StartVm(AppContainer.userManager, AppContainer.userRemote)
    fun loginVm(): LoginVm = LoginVm(AppContainer.userManager, AppContainer.userRemote)
    fun registerVm(): RegisterVm = RegisterVm(AppContainer.userManager, AppContainer.userRemote)
    fun mainVm(): MainVm = MainVm(AppContainer.userManager)
    fun chatListVm(): ChatListVm = ChatListVm()
    fun weChatDemoVm(): WeChatDemoVm = WeChatDemoVm()
    fun ossDemoVm(): OssDemoVm = OssDemoVm()
    fun voiceAgentVm(): VoiceAgentVm = VoiceAgentVm()
}
