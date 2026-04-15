package com.vectordemo.activity

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import com.vectordemo.ui.theme.VectorDemoTheme
import com.vectordemo.ui.view.activity.ComposeRegisterScreen
import com.vectordemo.utils.activity.BaseComponentActivity
import com.vectordemo.utils.permissions.ComposePermissionUtils
import com.vectordemo.utils.permissions.GainPermissionCallback
import com.vectordemo.viewModel.activity.RegisterEffect
import com.vectordemo.viewModel.activity.RegisterIntent
import com.vectordemo.viewModel.activity.RegisterVm
import kotlinx.coroutines.launch

class RegisterActivity : BaseComponentActivity() {
    private val vm: RegisterVm by viewModels()
    private val permissionUtils = ComposePermissionUtils()
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        vm.processIntent(RegisterIntent.AvatarSelected(result.data?.data))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerPermissionLauncher()
        observeEffect()
        setContent {
            VectorDemoTheme {
                val state by vm.uiState.collectAsState()
                ComposeRegisterScreen(
                    state = state,
                    processIntent = { vm.processIntent(it) }
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
        permissionUtils.registerPermissionLauncher(this, mustPermission, emptyArray())
    }

    private fun observeEffect() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                vm.effect.collect { effect ->
                    when (effect) {
                        RegisterEffect.RequestStoragePermission -> requestStoragePermission()
                        RegisterEffect.NavigateToMain -> {
                            startActivity(Intent(this@RegisterActivity, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            finish()
                        }

                        RegisterEffect.NavigateToLogin -> finish()
                        is RegisterEffect.ShowToast -> Toast.makeText(this@RegisterActivity, effect.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun requestStoragePermission() {
        permissionUtils.requestPermissions(this, object : GainPermissionCallback {
            override fun allGranted() = openImagePicker()
            override fun notGranted(notGrantedPermissions: Array<String?>?) {
                Toast.makeText(this@RegisterActivity, "请给予存储权限", Toast.LENGTH_SHORT).show()
            }
            override fun always() {}
        })
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }
}
