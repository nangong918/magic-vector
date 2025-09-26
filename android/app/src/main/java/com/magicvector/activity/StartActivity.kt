package com.magicvector.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.core.baseutil.ActivityLaunchUtils
import com.data.domain.constant.BaseConstant
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.theme.Purple80
import java.util.Timer
import java.util.TimerTask

class StartActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MagicVectorTheme {
                StartScreen()
            }
        }

        initTimer()
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
                .background(Purple80), // 设置背景为灰色
            contentAlignment = Alignment.Center // 设置内容居中
        ) {
            Greeting(name = "world") // 将文本内容改为 "world"
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun GreetingPreview() {
    MagicVectorTheme {
        StartScreen()
    }
}