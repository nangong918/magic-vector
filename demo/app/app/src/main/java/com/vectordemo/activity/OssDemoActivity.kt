package com.vectordemo.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.ui.view.oss.OssDemoScreen
import com.vectordemo.utils.activity.BaseComponentActivity
import com.vectordemo.viewModel.oss.OssDemoViewModel

class OssDemoActivity : BaseComponentActivity() {

    private val vm: OssDemoViewModel by viewModels { OssDemoViewModel.factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VectorDemoTheme {
                OssDemoScreen(vm = vm, onBack = { finish() })
            }
        }
    }
}
