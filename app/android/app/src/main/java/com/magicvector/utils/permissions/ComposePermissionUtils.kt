package com.magicvector.utils.permissions

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Compose/AndroidX 页面专用权限工具。
 *
 * 设计对齐 PermissionUtils：
 * 1) 启动阶段先 registerPermissionLauncher（在 onCreate）
 * 2) 业务时机再 requestPermissions
 */
class ComposePermissionUtils {

    private lateinit var launcher: ActivityResultLauncher<Array<String>>
    private var mustPermissions: Array<String> = emptyArray()
    private var optionalPermissions: Array<String> = emptyArray()
    private var callback: GainPermissionCallback? = null

    /**
     * 在 Activity 启动阶段注册权限回调（推荐 onCreate 中调用）。
     */
    fun registerPermissionLauncher(
        activity: ComponentActivity,
        mustPermissions: Array<String>,
        optionalPermissions: Array<String> = emptyArray()
    ) {
        this.mustPermissions = mustPermissions
        this.optionalPermissions = optionalPermissions

        launcher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val denied = result.filterValues { granted -> !granted }.keys.toList()
            val mustGranted = mustPermissions.all { permission -> result[permission] != false }

            if (mustGranted) {
                callback?.allGranted()
            } else {
                callback?.notGranted(denied.map { it as String? }.toTypedArray())
            }
            callback?.always()
        }
    }

    /**
     * 在业务触发点请求权限。
     */
    fun requestPermissions(activity: ComponentActivity, callback: GainPermissionCallback) {
        this.callback = callback
        val all = mustPermissions + optionalPermissions
        val needRequest = all.filter { permission ->
            ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (needRequest.isEmpty()) {
            callback.allGranted()
            callback.always()
            return
        }
        launcher.launch(needRequest)
    }
}
