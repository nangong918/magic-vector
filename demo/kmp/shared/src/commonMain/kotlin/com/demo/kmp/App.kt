package com.demo.kmp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vectordemo.di.AppContainer
import com.vectordemo.ui.navigation.AppNavigator
import com.vectordemo.ui.navigation.AppRoute
import com.vectordemo.ui.navigation.NativeFeatureBridge
import com.vectordemo.ui.oss.OssMediaBridge
import com.vectordemo.ui.navigation.rememberSyncedAppRoutes
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.ui.view.activity.ComposeLoginScreen
import com.vectordemo.ui.view.activity.ComposeRegisterScreen
import com.vectordemo.ui.view.activity.HelloScreen
import com.vectordemo.ui.view.activity.MainScreen
import com.vectordemo.ui.view.activity.NativeFeatureScreen
import com.vectordemo.ui.view.activity.StartScreen
import com.vectordemo.ui.view.chat.ChatListScreen
import com.vectordemo.ui.view.common.UnsupportedScreen
import com.vectordemo.ui.view.oss.OssDemoScreen
import com.vectordemo.ui.view.voice.VoiceAgentScreen
import com.vectordemo.viewModel.AppViewModelFactory
import com.vectordemo.viewModel.activity.LoginEffect
import com.vectordemo.viewModel.activity.LoginIntent
import com.vectordemo.viewModel.activity.MainEffect
import com.vectordemo.viewModel.activity.MainIntent
import com.vectordemo.viewModel.activity.RegisterEffect
import com.vectordemo.viewModel.activity.StartEffect
import com.vectordemo.viewModel.activity.StartIntent
import com.vectordemo.viewModel.oss.OssDemoEffect
import com.vectordemo.viewModel.oss.OssDemoIntent
import com.vectordemo.viewModel.voice.VoiceAgentEffect

@Composable
fun App() {
    VectorDemoTheme {
        rememberSyncedAppRoutes()

        var toastMessage by remember { mutableStateOf("") }
        val routes by AppNavigator.routes.collectAsState()
        val route = routes.lastOrNull() ?: AppRoute.START

        val startVm = viewModel { AppViewModelFactory.startVm() }
        val loginVm = viewModel { AppViewModelFactory.loginVm() }
        val registerVm = viewModel { AppViewModelFactory.registerVm() }
        val mainVm = viewModel { AppViewModelFactory.mainVm() }
        val chatListVm = viewModel { AppViewModelFactory.chatListVm() }
        val ossDemoVm = viewModel { AppViewModelFactory.ossDemoVm() }
        val voiceAgentVm = viewModel { AppViewModelFactory.voiceAgentVm() }

        val loginState by loginVm.uiState.collectAsState()
        val loginDataState by loginVm.dataState.collectAsState()
        val registerState by registerVm.uiState.collectAsState()
        val mainState by mainVm.uiState.collectAsState()
        val chatState by chatListVm.uiState.collectAsState()
        val ossState by ossDemoVm.uiState.collectAsState()
        val voiceState by voiceAgentVm.uiState.collectAsState()

        LaunchedEffect(Unit) {
            if (!AppContainer.isInitialized) {
                AppContainer.initialize()
            }
        }

        LaunchedEffect(route) {
            when (route) {
                AppRoute.START -> startVm.processIntent(StartIntent.Initialize)
                AppRoute.MAIN -> mainVm.processIntent(MainIntent.RefreshUserDisplay)
                AppRoute.OSS -> ossDemoVm.processIntent(OssDemoIntent.Initialize)
                AppRoute.CHAT -> chatListVm.initialize()
                AppRoute.VOICE -> voiceAgentVm.initialize()
                else -> Unit
            }
        }

        LaunchedEffect(startVm) {
            startVm.effect.collect { effect ->
                when (effect) {
                    StartEffect.NavigateToMain -> AppNavigator.resetTo(AppRoute.MAIN)
                    StartEffect.NavigateToLogin -> AppNavigator.resetTo(AppRoute.LOGIN)
                    is StartEffect.ShowToast -> toastMessage = effect.message
                }
            }
        }

        LaunchedEffect(loginVm) {
            loginVm.effect.collect { effect ->
                when (effect) {
                    LoginEffect.NavigateToMain -> AppNavigator.resetTo(AppRoute.MAIN)
                    LoginEffect.NavigateToRegister -> AppNavigator.navigate(AppRoute.REGISTER)
                    is LoginEffect.ShowToast -> toastMessage = effect.message
                }
            }
        }

        LaunchedEffect(registerVm) {
            registerVm.effect.collect { effect ->
                when (effect) {
                    RegisterEffect.NavigateToMain -> AppNavigator.resetTo(AppRoute.MAIN)
                    RegisterEffect.NavigateToLogin -> AppNavigator.resetTo(AppRoute.LOGIN)
                    RegisterEffect.RequestStoragePermission -> {
                        toastMessage = "当前平台尚未接入统一相册选择器"
                    }
                    is RegisterEffect.ShowToast -> toastMessage = effect.message
                }
            }
        }

        LaunchedEffect(mainVm) {
            mainVm.effect.collect { effect ->
                when (effect) {
                    MainEffect.NavigateToHello -> AppNavigator.navigate(AppRoute.HELLO)
                    MainEffect.NavigateToOssDemo -> AppNavigator.navigate(AppRoute.OSS)
                    MainEffect.NavigateToChatList -> AppNavigator.navigate(AppRoute.CHAT)
                    MainEffect.NavigateToVoiceAgent -> AppNavigator.navigate(AppRoute.VOICE)
                    MainEffect.NavigateToLivePush -> {
                        if (!NativeFeatureBridge.openLivePush()) {
                            AppNavigator.navigate(AppRoute.LIVE_PUSH)
                        }
                    }
                    MainEffect.NavigateToLivePull -> {
                        if (!NativeFeatureBridge.openLivePull()) {
                            AppNavigator.navigate(AppRoute.LIVE_PULL)
                        }
                    }
                    MainEffect.NavigateToStlCpp -> {
                        if (!NativeFeatureBridge.openStlCpp()) {
                            AppNavigator.navigate(AppRoute.STL_CPP)
                        }
                    }
                    MainEffect.NavigateToLogin -> AppNavigator.resetTo(AppRoute.LOGIN)
                    is MainEffect.ShowToast -> toastMessage = effect.message
                }
            }
        }

        LaunchedEffect(ossDemoVm) {
            ossDemoVm.effect.collect { effect ->
                when (effect) {
                    is OssDemoEffect.ShowToast -> toastMessage = effect.message
                    OssDemoEffect.OpenMainImagePicker -> {
                        OssMediaBridge.pickMainImage { picked ->
                            ossDemoVm.processIntent(OssDemoIntent.MainImagePicked(picked))
                        }
                    }
                    OssDemoEffect.OpenReplaceImagePicker -> {
                        OssMediaBridge.pickReplaceImage { picked ->
                            ossDemoVm.processIntent(OssDemoIntent.ReplaceImagePicked(picked))
                        }
                    }
                }
            }
        }

        LaunchedEffect(voiceAgentVm) {
            voiceAgentVm.effect.collect { effect ->
                when (effect) {
                    VoiceAgentEffect.FinishActivity -> AppNavigator.resetTo(AppRoute.MAIN)
                }
            }
        }

        when (route) {
            AppRoute.START -> StartScreen()
            AppRoute.LOGIN -> ComposeLoginScreen(
                state = loginState,
                savedAccounts = loginDataState.savedUserSessions,
                processIntent = { loginVm.processIntent(it) },
            )
            AppRoute.REGISTER -> ComposeRegisterScreen(
                state = registerState,
                processIntent = { registerVm.processIntent(it) },
            )
            AppRoute.MAIN -> MainScreen(
                state = mainState,
                processIntent = { mainVm.processIntent(it) },
            )
            AppRoute.HELLO -> HelloScreen()
            AppRoute.CHAT -> ChatListScreen(
                state = chatState,
                onBack = { AppNavigator.goBack() },
                onSend = { chatListVm.sendMessage(it) },
            )
            AppRoute.OSS -> OssDemoScreen(
                state = ossState,
                processIntent = { ossDemoVm.processIntent(it) },
                onBack = { AppNavigator.goBack() },
            )
            AppRoute.VOICE -> VoiceAgentScreen(
                state = voiceState,
                serviceStatusText = voiceAgentVm.buildServiceStatusText(voiceState),
                onBack = { AppNavigator.goBack() },
                onSelectLlm = { voiceAgentVm.setLlmProvider(it) },
            )
            AppRoute.LIVE_PUSH -> NativeFeatureScreen(
                title = "Live Push Demo",
                description = "该功能为 Android 原生 AAR 能力，当前由 Android Host 承载。",
                onBack = { AppNavigator.goBack() },
            )
            AppRoute.LIVE_PULL -> NativeFeatureScreen(
                title = "Live Pull Demo",
                description = "该功能为 Android 原生 AAR 能力，当前由 Android Host 承载。",
                onBack = { AppNavigator.goBack() },
            )
            AppRoute.STL_CPP -> NativeFeatureScreen(
                title = "STL / JNI Demo",
                description = "该功能为 Android JNI + C++ 能力，当前由 Android Host 承载。",
                onBack = { AppNavigator.goBack() },
            )
            AppRoute.UNSUPPORTED -> UnsupportedScreen(
                message = "该功能当前仅支持 Android 平台。",
                onBack = { AppNavigator.goBack() },
            )
        }
    }
}
