package com.core.baseutil.permissions

import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class PermissionUtils {

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var mustPermissions: Array<String> = emptyArray()
    private var optionalPermissions: Array<String> = emptyArray()
    private var callback: GainPermissionCallback? = null

    /**
     * 注册权限请求结果监听
     * @param activity              FragmentActivity
     * @param mustPermissions       必须请求的权限
     * @param optionalPermissions   可选请求的权限
     * @param callback              权限请求结果回调
     */
    fun registerPermissionLauncher(
        activity: FragmentActivity,
        mustPermissions: Array<String>,
        optionalPermissions: Array<String>,
    ) {
        this.mustPermissions = mustPermissions
        this.optionalPermissions = optionalPermissions

        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val grantedPermissions = permissions.filter { it.value }.map { it.key }
            val deniedPermissions = permissions.filter { !it.value }.map { it.key }

            // 只需要必须请求的权限通过就行
            val mustGranted = mustPermissions.all { it in grantedPermissions }

            if (mustGranted) {
                // 所有必要权限都被授予
                callback?.allGranted()
            }

            if (deniedPermissions.isNotEmpty()) {
                // 存在未授权的必要权限
                callback?.notGranted(deniedPermissions.toTypedArray())
            }

            // 无论如何都会执行的回调
            callback?.always()
        }
    }

    /**
     * 请求权限
     * @param activity              FragmentActivity
     */
    fun requestPermissions(
        activity: FragmentActivity,
        callback: GainPermissionCallback
    ) {
        this.callback = callback

        // 合并出全部权限
        val allPermissions = mustPermissions + optionalPermissions

        // 检查权限并请求
        // 过滤出没有请求的权限
        val permissionsToRequest = allPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest)
        } else {
            // 如果所有必要权限已被授予
            callback.allGranted()
        }
    }
}