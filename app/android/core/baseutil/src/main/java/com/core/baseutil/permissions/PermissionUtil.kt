package com.core.baseutil.permissions

import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.permissionx.guolindev.PermissionX

object PermissionUtil {

    val TAG: String = PermissionUtil::class.java.simpleName

    /**
     * 申请必要权限并执行
     * @param activity              activity
     * @param mustPermission        必须权限
     * @param optionalPermission    可选权限
     * @param callback              授权后回调
     */
    fun requestPermissionSelectX(
        activity: FragmentActivity,
        mustPermission: Array<String>,
        optionalPermission: Array<String>,
        callback: GainPermissionCallback
    ) {
        val allPermissions = mustPermission + optionalPermission

        PermissionX.init(activity)
            .permissions(*allPermissions)
            .request { allGranted, grantedList, deniedList ->
                val mustGranted = mustPermission.all { it in grantedList }

                if (allGranted) {
                    Log.i(TAG, "全部授权")
                    callback.allGranted()
                }
                else if (mustGranted) {
                    Log.i(TAG, "必要权限全部授权")
                    val notOptionalPermission = deniedList.filterNot { it in mustPermission }
                    Log.w(TAG, "未授权的非必要权限有：$notOptionalPermission")
                    callback.allGranted()
                }
                else {
                    Log.w(TAG, "存在未授权的必须权限：")
                    Log.i(TAG, "已授权: ${grantedList.joinToString()}")
                    Log.w(TAG, "未授权: ${deniedList.joinToString()}")

                    val deniedMustPermissions = mustPermission.filter { it in deniedList }
                    Log.w(TAG, "未授权的必要权限有：$deniedMustPermissions")

                    val deniedOptionalPermissions = optionalPermission.filter { it in deniedList }
                    Log.w(TAG, "未授权的非必要权限有：$deniedOptionalPermissions")

                    val deniedArray = deniedList.toTypedArray()
                    callback.notGranted(deniedArray)
                }
            }
    }
}