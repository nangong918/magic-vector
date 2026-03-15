package com.magicvector.activity

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.core.baseutil.fragmentActivity.ActivityLaunchUtils
import com.core.baseutil.permissions.GainPermissionCallback
import com.magicvector.ui.theme.MagicVectorTheme
import com.magicvector.ui.view.activity.ComposeRegisterScreen
import com.magicvector.utils.activity.BaseComponentActivity
import com.magicvector.utils.permissions.ComposePermissionUtils
import com.magicvector.viewModel.activity.ComposeRegisterVm
import com.magicvector.viewModel.activity.RegisterEffect
import com.magicvector.viewModel.activity.RegisterIntent
import kotlinx.coroutines.launch

class ComposeRegisterActivity : BaseComponentActivity() {
    private val vm: ComposeRegisterVm by viewModels()
    private val permissionUtils = ComposePermissionUtils()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        vm.processIntent(RegisterIntent.AvatarSelected(result.data?.data))
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerPermissionLauncher()
        observeEffect()

        setContent {
            MagicVectorTheme {
                val state by vm.uiState.collectAsState()
                ComposeRegisterScreen(
                    state = state,
                    onAccountChange = { vm.processIntent(RegisterIntent.UpdateAccount(it)) },
                    onPasswordChange = { vm.processIntent(RegisterIntent.UpdatePassword(it)) },
                    onConfirmPasswordChange = { vm.processIntent(RegisterIntent.UpdateConfirmPassword(it)) },
                    onSelectAvatar = { vm.processIntent(RegisterIntent.SelectAvatar) },
                    onSubmit = { vm.processIntent(RegisterIntent.SubmitRegister) },
                    onGoLogin = { vm.processIntent(RegisterIntent.NavigateToLogin) }
                )
            }
        }
    }

    private fun registerPermissionLauncher() {
        val mustPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionUtils.registerPermissionLauncher(
            activity = this,
            mustPermissions = mustPermission,
            optionalPermissions = emptyArray()
        )
    }

    private fun observeEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        RegisterEffect.RequestStoragePermission -> requestStoragePermission()
                        RegisterEffect.NavigateToMain -> navigateToMain()
                        RegisterEffect.NavigateToLogin -> finish()
                        is RegisterEffect.ShowToast -> {
                            Toast.makeText(this@ComposeRegisterActivity, effect.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun requestStoragePermission() {
        permissionUtils.requestPermissions(
            this,
            object : GainPermissionCallback {
                override fun allGranted() {
                    openImagePicker()
                }

                override fun notGranted(notGrantedPermissions: Array<String?>?) {
                    Toast.makeText(this@ComposeRegisterActivity, "请给予存储权限", Toast.LENGTH_SHORT).show()
                }

                override fun always() {}
            }
        )
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun navigateToMain() {
        ActivityLaunchUtils.launchNewTask(
            this,
            Intent(this, MainActivity::class.java),
            null
        )
        finish()
    }
}
