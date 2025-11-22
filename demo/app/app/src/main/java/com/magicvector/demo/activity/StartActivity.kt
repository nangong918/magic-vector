package com.magicvector.demo.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.magicvector.demo.R
import com.magicvector.demo.domain.constant.BaseConstant
import com.magicvector.demo.ui.theme.AppDemoTheme
import com.magicvector.demo.utils.activity.ActivityLaunchUtils
import java.util.Timer
import java.util.TimerTask

class StartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppDemoTheme {
                StartScreen()
            }
        }

        initTimer()
    }

    override fun onResume() {
        super.onResume()

        // 隐藏标题导航栏
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        // 隐藏状态栏
        val decorView = window.decorView
        decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }

    //-------------------------------定时跳转-------------------------------

    private lateinit var timer: Timer
    private lateinit var timerTask: TimerTask

    private fun initTimer(){
        timer = Timer()

        timerTask = object : TimerTask() {
            override fun run() {
                activityTurn()
            }
        }

        timer.schedule(timerTask, BaseConstant.Constant.START_DELAY_TIME)
    }

    private fun destroyTimer() {
        // 确保在 Activity 销毁时取消 Timer
        timer.cancel()
        timerTask.cancel()
    }

    private fun activityTurn(){
        val intent = Intent(this@StartActivity, MainActivity::class.java)

        ActivityLaunchUtils.launchNewTask(
            this@StartActivity,
            intent,
            null
        )

        finish()
    }


    override fun onDestroy() {
        super.onDestroy()

        destroyTimer()
    }

}


@Composable
fun StartScreen() {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White), // 设置背景为紫色
            contentAlignment = Alignment.Center // 设置内容居中
        ) {
            Logo() // 使用 Logo 组件展示 logo
        }
    }
}

@Composable
fun Logo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.mipmap.logo), // 替换为你的 logo 资源 ID
        contentDescription = "App Logo",
        modifier = modifier.size(220.dp)
            .clip(RoundedCornerShape(20.dp))
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun GreetingPreview() {
    AppDemoTheme {
        StartScreen()
    }
}