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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.AgentAvatar
import com.magicvector.ui.view.InfoBarView
import com.magicvector.ui.view.activity.AgentDescriptionInput
import com.magicvector.ui.view.activity.AgentNameInput
import com.magicvector.ui.view.activity.SubmitButton
import com.magicvector.viewModel.activity.ComposeCreateAgentVm
import com.magicvector.utils.activity.BaseComponentActivity
import com.magicvector.viewModel.activity.CreateAgentEffect
import com.magicvector.viewModel.activity.CreateAgentIntent
import com.magicvector.viewModel.activity.CreateAgentState
import kotlinx.coroutines.launch

class ComposeCreateAgentActivity : BaseComponentActivity() {

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
                        is CreateAgentEffect.CreateAgent -> {
                            vm.createAgent(context = this@ComposeCreateAgentActivity)
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
    // 添加滚动状态
    val scrollState = rememberScrollState()

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,  // 水平居中
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Agent Avatar
            AgentAvatar(
//            imageFile = state.avatarFile,
                defaultResId = state.defaultAvatarResId,
                onClick = onSelectAvatar,
                modifier = Modifier.align(Alignment.CenterHorizontally) // 剧中
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Agent Name Input - 对应 GeneralEditText
            AgentNameInput(
                value = state.agentName,
                onValueChange = onNameChange,
                isValid = state.isNameValid,
                errorMessage = state.nameError,
                maxLength = 20
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Agent Description Input - 对应 GeneralEditText (第二行)
            AgentDescriptionInput(
                value = state.agentDescription,
                onValueChange = onDescriptionChange,
                isValid = state.isDescriptionValid,
                errorMessage = state.descriptionError
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button - 对应 AppCompatButton
            SubmitButton(
                enabled = state.isFormValid,
                isLoading = state.isSubmitting,
                onClick = onSubmit
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
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