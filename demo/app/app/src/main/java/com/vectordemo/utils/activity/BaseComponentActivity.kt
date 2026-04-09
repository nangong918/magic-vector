package com.vectordemo.utils.activity

import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity

abstract class BaseComponentActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        // 每次恢复时设置全屏
        setupFullScreen()
    }

    protected fun setupFullScreen() {
        // 隐藏标题导航栏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // 隐藏状态栏和导航栏
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }
}
