package com.demo.kmp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.vectordemo.activity.clearNativeFeatureRouter
import com.vectordemo.activity.installNativeFeatureRouter
import com.vectordemo.di.AppContainer
import com.vectordemo.domain.config.initModuleKeyConfigContext
import com.vectordemo.ui.navigation.AppNavigator
import com.vectordemo.ui.navigation.AppRoute
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        initModuleKeyConfigContext(applicationContext)
        runBlocking { AppContainer.initialize() }
        // 避免每次旋转之后重启activity导致每次都要重新start，重新登录
        if (savedInstanceState == null) {
            AppNavigator.resetTo(AppRoute.START)
        }
        installNativeFeatureRouter(this)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (AppNavigator.goBack()) return
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            },
        )

        setContent {
            App()
        }
    }

    override fun onDestroy() {
        clearNativeFeatureRouter()
        super.onDestroy()
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}