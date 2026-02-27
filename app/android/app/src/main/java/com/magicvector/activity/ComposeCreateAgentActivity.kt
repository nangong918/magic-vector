package com.magicvector.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.activity.InfoBarView
import com.magicvector.viewModel.activity.ComposeCreateAgentVm
import com.magicvector.viewModel.activity.CreateAgentEffect
import com.magicvector.viewModel.activity.CreateAgentIntent
import com.magicvector.viewModel.activity.CreateAgentState
import kotlinx.coroutines.launch

class ComposeCreateAgentActivity : ComponentActivity() {

    private val vm: ComposeCreateAgentVm by viewModels()

    // 图片选择器 Launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data: Intent? = result.data
        val imageUri = data?.data
        vm.processIntent(CreateAgentIntent.AvatarSelected(imageUri))
    }

    // 权限请求 Launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        vm.onPermissionResult(isGranted, this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 发送初始化 Intent
        vm.processIntent(CreateAgentIntent.Initialize)

        // 观察 Effect
        observeEffects()

        setContent {
            MagicVectorTheme {

                val state by vm.uiState.collectAsState()

                CreateAgentScreen(
                    state = state,
                    onNameChange = { vm.processIntent(CreateAgentIntent.UpdateAgentName(it)) },
                    onDescriptionChange = { vm.processIntent(CreateAgentIntent.UpdateAgentDescription(it)) },
                    onSelectAvatar = { vm.processIntent(CreateAgentIntent.SelectAvatar) },
                    onSubmit = { vm.processIntent(CreateAgentIntent.SubmitCreate) },
                    onBack = { vm.processIntent(CreateAgentIntent.CancelCreate) }
                )
            }
        }
    }

    private fun observeEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        is CreateAgentEffect.NavigateBack -> {
                            finish()
                        }
                        is CreateAgentEffect.NavigateToImagePicker -> {
                            openImagePicker()
                        }
                        is CreateAgentEffect.RequestStoragePermission -> {
                            requestStoragePermission()
                        }
                        is CreateAgentEffect.ShowToast -> {
                            Toast.makeText(this@ComposeCreateAgentActivity, effect.message, Toast.LENGTH_SHORT).show()
                        }
                        is CreateAgentEffect.ShowError -> {
                            Toast.makeText(this@ComposeCreateAgentActivity, effect.message, Toast.LENGTH_SHORT).show()
                        }
                        is CreateAgentEffect.AgentCreated -> {
                            // Agent 创建成功，可以返回结果
                            val resultIntent = Intent().apply {
                                putExtra("agent_id", effect.agentId)
                            }
                            setResult(RESULT_OK, resultIntent)
                        }
                    }
                }
            }
        }
    }


    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun requestStoragePermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // 已有权限，直接打开图片选择器
            openImagePicker()
        } else {
            // 请求权限
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}


@Composable
private fun CreateAgentScreen(
    state: CreateAgentState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onSelectAvatar: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            InfoBarView(
                title = "创建Agent",
                showBackButton = true,
                onBackClick = onBack,
                backgroundColor = Color.Gray,
                titleColor = Color.Black,
            )
        }
    ) { innerPadding ->
    }
}


@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun CreateAgentPreview() {
    MagicVectorTheme {
        CreateAgentScreen(
            state = CreateAgentState(),
            onNameChange = {},
            onDescriptionChange = {},
            onSelectAvatar = {},
            onSubmit = {},
            onBack = {}
        )
    }
}