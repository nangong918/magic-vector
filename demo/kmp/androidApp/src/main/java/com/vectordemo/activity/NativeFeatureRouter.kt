package com.vectordemo.activity

import android.content.Context
import android.content.Intent
import com.vectordemo.ui.navigation.NativeFeatureActions
import com.vectordemo.ui.navigation.NativeFeatureBridge

fun installNativeFeatureRouter(context: Context) {
    val appCtx = context.applicationContext
    NativeFeatureBridge.setActions(
        NativeFeatureActions(
            openLivePush = { appCtx.startNativeActivity(LivePushDemoActivity::class.java) },
            openLivePull = { appCtx.startNativeActivity(LivePullDemoActivity::class.java) },
            openStlCpp = { appCtx.startNativeActivity(STLActivity::class.java) },
        ),
    )
}

fun clearNativeFeatureRouter() {
    NativeFeatureBridge.setActions(NativeFeatureActions())
}

private fun Context.startNativeActivity(clazz: Class<*>): Boolean {
    startActivity(
        Intent(this, clazz).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
    return true
}
