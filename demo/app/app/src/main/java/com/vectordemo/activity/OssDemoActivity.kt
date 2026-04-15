package com.vectordemo.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.ui.view.oss.OssDemoScreen
import com.vectordemo.utils.activity.BaseComponentActivity
import com.vectordemo.viewModel.oss.OssDemoEffect
import com.vectordemo.viewModel.oss.OssDemoIntent
import com.vectordemo.viewModel.oss.OssDemoVm
import kotlinx.coroutines.launch

class OssDemoActivity : BaseComponentActivity() {

    private val vm: OssDemoVm by viewModels { OssDemoVm.factory() }

    private val pickMainLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        vm.processIntent(OssDemoIntent.MainImagePicked(uri))
    }

    private val pickReplaceLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        vm.processIntent(OssDemoIntent.ReplaceImagePicked(uri))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeEffect()
        setContent {
            VectorDemoTheme {
                val state by vm.uiState.collectAsState()
                OssDemoScreen(
                    state = state,
                    processIntent = { vm.processIntent(it) },
                    onBack = { finish() }
                )
            }
        }
    }

    private fun observeEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        is OssDemoEffect.ShowToast -> {
                            Toast.makeText(this@OssDemoActivity, effect.message, Toast.LENGTH_LONG).show()
                        }

                        OssDemoEffect.OpenMainImagePicker -> {
                            pickMainLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }

                        OssDemoEffect.OpenReplaceImagePicker -> {
                            pickReplaceLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    }
                }
            }
        }
    }
}
