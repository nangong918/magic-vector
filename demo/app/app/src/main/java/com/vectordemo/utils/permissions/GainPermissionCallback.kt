package com.vectordemo.utils.permissions

interface GainPermissionCallback {
    fun allGranted()
    fun notGranted(notGrantedPermissions: Array<String?>?)
    fun always()
}
