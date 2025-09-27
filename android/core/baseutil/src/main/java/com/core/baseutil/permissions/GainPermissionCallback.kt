package com.core.baseutil.permissions

/**
 * 权限获取之后的回调
 */
interface GainPermissionCallback {
    /**
     * 获得了全部权限执行
     */
    fun allGranted()

    /**
     * 存在没有获取的权限执行
     */
    fun notGranted(notGrantedPermissions: Array<String?>?)

    /**
     * 无论如何都执行的回调
     */
    fun always()
}