package com.magicvector.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.core.baseutil.permissions.GainPermissionCallback
import com.core.baseutil.ui.ToastUtils
import com.magicvector.domain.constant.BaseConstant
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.InfoBarView
import com.magicvector.ui.view.activity.AgentDescriptionInput
import com.magicvector.ui.view.activity.AgentNameInput
import com.magicvector.utils.permissions.ComposePermissionUtils
import com.magicvector.utils.activity.BaseComponentActivity
import com.magicvector.viewModel.activity.ComposeAgentInfoEffect
import com.magicvector.viewModel.activity.ComposeAgentInfoIntent
import com.magicvector.viewModel.activity.ComposeAgentInfoState
import com.magicvector.viewModel.activity.ComposeAgentInfoVm

class ComposeAgentInfoActivity : BaseComponentActivity() {
    private val vm: ComposeAgentInfoVm by viewModels()
    private val storagePermissionUtils = ComposePermissionUtils()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        vm.processIntent(ComposeAgentInfoIntent.OnAvatarSelected(result.data?.data))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storagePermissionUtils.registerPermissionLauncher(
            activity = this,
            mustPermissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            optionalPermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        )
        enableEdgeToEdge()
        observeEffects()
        setContent {
            MagicVectorTheme {
                val state by vm.uiState.collectAsState()
                AgentInfoScreen(
                    state = state,
                    onBack = { vm.processIntent(ComposeAgentInfoIntent.ClickBack) },
                    onAvatarClick = { vm.processIntent(ComposeAgentInfoIntent.ClickAvatar) },
                    onNameChange = { vm.processIntent(ComposeAgentInfoIntent.UpdateName(it)) },
                    onDescriptionChange = { vm.processIntent(ComposeAgentInfoIntent.UpdateDescription(it)) },
                    onConfirm = { vm.processIntent(ComposeAgentInfoIntent.ClickConfirm) },
                    onDelete = { vm.processIntent(ComposeAgentInfoIntent.ClickDelete) }
                )
            }
        }
        vm.processIntent(ComposeAgentInfoIntent.Initialize(intent.getStringExtra("agentId")))
    }

    private fun observeEffects() {
        lifecycleScope.launchWhenStarted {
            vm.effect.collect { effect ->
                when (effect) {
                    ComposeAgentInfoEffect.Finish -> finish()
                    ComposeAgentInfoEffect.OpenImagePicker -> openImagePicker()
                    ComposeAgentInfoEffect.RequestStoragePermission -> {
                        storagePermissionUtils.requestPermissions(this@ComposeAgentInfoActivity, object : GainPermissionCallback {
                            override fun allGranted() {
                                vm.processIntent(ComposeAgentInfoIntent.StoragePermissionResult(true))
                            }

                            override fun notGranted(notGrantedPermissions: Array<String?>?) {
                                vm.processIntent(ComposeAgentInfoIntent.StoragePermissionResult(false))
                            }

                            override fun always() {
                            }
                        })
                    }
                    ComposeAgentInfoEffect.ShowLoading -> {
                    }
                    ComposeAgentInfoEffect.HideLoading -> {
                    }
                    is ComposeAgentInfoEffect.ShowToastRes -> {
                        ToastUtils.showToastActivity(this@ComposeAgentInfoActivity, getString(effect.resId))
                    }
                }
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
}

@Composable
private fun AgentInfoScreen(
    state: ComposeAgentInfoState,
    onBack: () -> Unit,
    onAvatarClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDelete: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            InfoBarView(
                title = "设置Agent",
                showBackButton = true,
                onBackClick = onBack,
                backgroundColor = Color.White,
                titleColor = Color.Black
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            AsyncImage(
                model = state.avatarUri ?: state.avatarUrl ?: com.view.appview.R.mipmap.logo,
                contentDescription = "avatar",
                modifier = Modifier
                    .height(120.dp)
                    .background(Color.White)
                    .clickable(onClick = onAvatarClick)
            )
            Spacer(modifier = Modifier.height(20.dp))
            AgentNameInput(
                value = state.name,
                onValueChange = onNameChange,
                isValid = true,
                errorMessage = null,
                maxLength = BaseConstant.Constant.MAX_AGENT_NAME_LENGTH
            )
            Spacer(modifier = Modifier.height(20.dp))
            AgentDescriptionInput(
                value = state.description,
                onValueChange = onDescriptionChange,
                isValid = true,
                errorMessage = null
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onConfirm) { Text("设置Agent") }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onDelete) { Text("删除Agent") }
            if (state.isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun AgentInfoScreenPreview() {
    MagicVectorTheme {
        AgentInfoScreen(
            state = ComposeAgentInfoState(
                name = "测试Agent",
                description = "这是一个用于预览的设定。"
            ),
            onBack = {},
            onAvatarClick = {},
            onNameChange = {},
            onDescriptionChange = {},
            onConfirm = {},
            onDelete = {}
        )
    }
}
