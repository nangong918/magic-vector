package com.vectordemo.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.ui.view.activity.HelloScreen
import com.vectordemo.utils.activity.BaseComponentActivity

class HelloActivity : BaseComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VectorDemoTheme {
                HelloScreen()
            }
        }
    }
}
