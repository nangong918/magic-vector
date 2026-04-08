package com.magicvector.activity

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.activity.ComposeMineVideoActivityScreen
import com.magicvector.utils.activity.BaseComponentActivity
import com.magicvector.viewModel.activity.MineVideoVm
import com.magicvector.viewModel.activity.MineVideoEffect
import com.magicvector.viewModel.activity.MineVideoIntent
import kotlinx.coroutines.launch

class MineVideoActivity : BaseComponentActivity() {

    private val vm: MineVideoVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeEffect()
        setContent {
            MagicVectorTheme {
                val state by vm.uiState.collectAsState()
                val pickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    vm.processIntent(MineVideoIntent.OnLocalVideoSelected(uri?.toString()))
                }
                val uploadPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    if (uri != null) {
                        vm.processIntent(
                            MineVideoIntent.StartUpload(
                                contentResolver = contentResolver,
                                uri = uri,
                                fileName = "local-upload.mp4"
                            )
                        )
                    }
                }
                ComposeMineVideoActivityScreen(
                    state = state,
                    onSwitchVideoTab = { vm.processIntent(MineVideoIntent.SwitchVideoTab(it)) },
                    onPickLocalVideo = { pickerLauncher.launch("video/*") },
                    onTogglePlay = { vm.processIntent(MineVideoIntent.ToggleVideoPlay) },
                    onLoadCloudVideos = { vm.processIntent(MineVideoIntent.LoadCloudVideos) },
                    onOpenCloudVideo = { vm.processIntent(MineVideoIntent.OpenCloudVideo(it)) },
                    onUploadPick = { uploadPickerLauncher.launch("video/*") },
                    onPauseUpload = { vm.processIntent(MineVideoIntent.PauseUpload) }
                )
            }
        }
        vm.processIntent(MineVideoIntent.Initialize)
    }

    private fun observeEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        MineVideoEffect.OpenLocalVideoPicker -> {
                            // 由 Activity Screen 直接触发 picker，此处不重复处理。
                        }
                        is MineVideoEffect.ShowToast -> {
                            Toast.makeText(this@MineVideoActivity, effect.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
