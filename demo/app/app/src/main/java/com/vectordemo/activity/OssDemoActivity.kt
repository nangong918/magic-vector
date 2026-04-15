package com.vectordemo.activity

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.ui.view.oss.OssDemoScreen
import com.vectordemo.utils.activity.BaseComponentActivity
import com.vectordemo.viewModel.oss.OssDemoEffect
import com.vectordemo.viewModel.oss.OssDemoIntent
import com.vectordemo.viewModel.oss.OssDemoViewModel

/**
 * OSS Demo：MVI 中 Activity 负责一次性副作用（Toast、系统相册选择器），通过 [OssDemoViewModel.effect] 与 [OssDemoViewModel.processIntent] 通信。
 */
class OssDemoActivity : BaseComponentActivity() {

    private val vm: OssDemoViewModel by viewModels { OssDemoViewModel.factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by vm.uiState.collectAsState()
            val pickMain = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                vm.processIntent(OssDemoIntent.MainImagePicked(uri))
            }
            val pickReplace = rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                vm.processIntent(OssDemoIntent.ReplaceImagePicked(uri))
            }

            LaunchedEffect(Unit) {
                vm.processIntent(OssDemoIntent.Initialize)
            }
            LaunchedEffect(Unit) {
                vm.effect.collect { effect ->
                    when (effect) {
                        is OssDemoEffect.ShowToast -> {
                            Toast.makeText(this@OssDemoActivity, effect.message, Toast.LENGTH_LONG).show()
                        }
                        OssDemoEffect.OpenMainImagePicker -> {
                            pickMain.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        OssDemoEffect.OpenReplaceImagePicker -> {
                            pickReplace.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    }
                }
            }

            VectorDemoTheme {
                OssDemoScreen(
                    state = state,
                    processIntent = { vm.processIntent(it) },
                    onBack = { finish() }
                )
            }
        }
    }
}
