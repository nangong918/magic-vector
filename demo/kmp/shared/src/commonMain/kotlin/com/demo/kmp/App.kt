package com.demo.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.vectordemo.ui.navigation.AppRoute
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.ui.view.activity.ComposeLoginScreen
import com.vectordemo.ui.view.activity.ComposeRegisterScreen
import com.vectordemo.ui.view.activity.HelloScreen
import com.vectordemo.ui.view.activity.MainScreen
import com.vectordemo.ui.view.activity.StartScreen
import com.vectordemo.ui.view.chat.ChatListScreen
import com.vectordemo.ui.view.common.UnsupportedScreen
import com.vectordemo.ui.view.oss.OssDemoScreen
import com.vectordemo.ui.view.voice.VoiceAgentScreen
import com.vectordemo.viewModel.AppVmStore
import com.vectordemo.viewModel.activity.LoginEffect
import com.vectordemo.viewModel.activity.LoginIntent
import com.vectordemo.viewModel.activity.MainEffect
import com.vectordemo.viewModel.activity.RegisterEffect
import com.vectordemo.viewModel.activity.StartEffect
import com.vectordemo.viewModel.activity.StartIntent
import com.vectordemo.viewModel.oss.OssDemoEffect
import com.vectordemo.viewModel.voice.VoiceAgentEffect

@Composable
fun App() {
    VectorDemoTheme {
        var route by remember { mutableStateOf(AppRoute.START) }
        var toastMessage by remember { mutableStateOf("") }

        val startState by AppVmStore.startVm.uiState.collectAsState()
        val loginState by AppVmStore.loginVm.uiState.collectAsState()
        val loginDataState by AppVmStore.loginVm.dataState.collectAsState()
        val registerState by AppVmStore.registerVm.uiState.collectAsState()
        val mainState by AppVmStore.mainVm.uiState.collectAsState()
        val chatState by AppVmStore.chatListVm.uiState.collectAsState()
        val ossState by AppVmStore.ossDemoVm.uiState.collectAsState()
        val voiceState by AppVmStore.voiceAgentVm.uiState.collectAsState()

        LaunchedEffect(Unit) {
            AppVmStore.startVm.processIntent(StartIntent.Initialize)
            AppVmStore.voiceAgentVm.initialize()
            AppVmStore.chatListVm.initialize()
        }

        LaunchedEffect(Unit) {
            AppVmStore.startVm.effect.collect { effect ->
                route = when (effect) {
                    StartEffect.NavigateToMain -> AppRoute.MAIN
                    StartEffect.NavigateToLogin -> AppRoute.LOGIN
                    is StartEffect.ShowToast -> {
                        toastMessage = effect.message
                        route
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            AppVmStore.loginVm.effect.collect { effect ->
                route = when (effect) {
                    LoginEffect.NavigateToMain -> AppRoute.MAIN
                    LoginEffect.NavigateToRegister -> AppRoute.REGISTER
                    is LoginEffect.ShowToast -> {
                        toastMessage = effect.message
                        route
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            AppVmStore.registerVm.effect.collect { effect ->
                route = when (effect) {
                    RegisterEffect.NavigateToMain -> AppRoute.MAIN
                    RegisterEffect.NavigateToLogin -> AppRoute.LOGIN
                    RegisterEffect.RequestStoragePermission -> {
                        toastMessage = "当前平台尚未接入统一相册选择器"
                        route
                    }
                    is RegisterEffect.ShowToast -> {
                        toastMessage = effect.message
                        route
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            AppVmStore.mainVm.effect.collect { effect ->
                route = when (effect) {
                    MainEffect.NavigateToHello -> AppRoute.HELLO
                    MainEffect.NavigateToOssDemo -> AppRoute.OSS
                    MainEffect.NavigateToChatList -> AppRoute.CHAT
                    MainEffect.NavigateToVoiceAgent -> AppRoute.VOICE
                    MainEffect.NavigateToLivePush,
                    MainEffect.NavigateToLivePull,
                    MainEffect.NavigateToStlCpp -> AppRoute.UNSUPPORTED
                    MainEffect.NavigateToLogin -> AppRoute.LOGIN
                    is MainEffect.ShowToast -> {
                        toastMessage = effect.message
                        route
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            AppVmStore.ossDemoVm.effect.collect { effect ->
                when (effect) {
                    is OssDemoEffect.ShowToast -> toastMessage = effect.message
                    OssDemoEffect.OpenMainImagePicker,
                    OssDemoEffect.OpenReplaceImagePicker -> toastMessage = "当前平台尚未接入统一相册选择器"
                }
            }
        }

        LaunchedEffect(Unit) {
            AppVmStore.voiceAgentVm.effect.collect { effect ->
                when (effect) {
                    VoiceAgentEffect.FinishActivity -> route = AppRoute.MAIN
                }
            }
        }

        when (route) {
            AppRoute.START -> StartScreen(startState)
            AppRoute.LOGIN -> ComposeLoginScreen(
                state = loginState,
                savedAccounts = loginDataState.savedUserSessions,
                processIntent = { AppVmStore.loginVm.processIntent(it) },
            )
            AppRoute.REGISTER -> ComposeRegisterScreen(
                state = registerState,
                processIntent = { AppVmStore.registerVm.processIntent(it) },
            )
            AppRoute.MAIN -> MainScreen(
                state = mainState,
                processIntent = { AppVmStore.mainVm.processIntent(it) },
            )
            AppRoute.HELLO -> HelloScreen()
            AppRoute.CHAT -> ChatListScreen(
                state = chatState,
                onBack = { route = AppRoute.MAIN },
                onSend = { AppVmStore.chatListVm.sendMessage(it) },
            )
            AppRoute.OSS -> OssDemoScreen(
                state = ossState,
                processIntent = { AppVmStore.ossDemoVm.processIntent(it) },
                onBack = { route = AppRoute.MAIN },
            )
            AppRoute.VOICE -> VoiceAgentScreen(
                state = voiceState,
                serviceStatusText = AppVmStore.voiceAgentVm.buildServiceStatusText(voiceState),
                onBack = { route = AppRoute.MAIN },
                onSelectLlm = { AppVmStore.voiceAgentVm.setLlmProvider(it) },
            )
            AppRoute.UNSUPPORTED -> UnsupportedScreen(
                message = "该功能当前仅支持 Android 平台。",
                onBack = { route = AppRoute.MAIN },
            )
        }
    }
}