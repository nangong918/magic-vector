package com.vectordemo.utils.permissions

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class ComposePermissionUtils {
    private lateinit var launcher: ActivityResultLauncher<Array<String>>
    private var mustPermissions: Array<String> = emptyArray()
    private var optionalPermissions: Array<String> = emptyArray()
    private var callback: GainPermissionCallback? = null

    fun registerPermissionLauncher(activity: ComponentActivity, mustPermissions: Array<String>, optionalPermissions: Array<String> = emptyArray()) {
        this.mustPermissions = mustPermissions
        this.optionalPermissions = optionalPermissions
        launcher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val denied = result.filterValues { !it }.keys.toTypedArray()
            val mustGranted = mustPermissions.all { result[it] != false }
            if (mustGranted) callback?.allGranted() else callback?.notGranted(denied.map { it as String? }.toTypedArray())
            callback?.always()
        }
    }

    fun requestPermissions(activity: ComponentActivity, callback: GainPermissionCallback) {
        this.callback = callback
        val all = mustPermissions + optionalPermissions
        val needRequest = all.filter { ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED }.toTypedArray()
        if (needRequest.isEmpty()) {
            callback.allGranted()
            callback.always()
            return
        }
        launcher.launch(needRequest)
    }
}
