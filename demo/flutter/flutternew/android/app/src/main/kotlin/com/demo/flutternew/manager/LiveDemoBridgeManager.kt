package com.demo.flutternew.manager

import android.content.Intent
import com.demo.flutternew.live.activity.LivePullDemoActivity
import com.demo.flutternew.live.activity.LivePushDemoActivity
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class LiveDemoBridgeManager(private val activity: FlutterActivity) {
    private val channelName = "com.demo.flutternew/live_demo_android"

    fun registerWith(flutterEngine: FlutterEngine) {
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName).setMethodCallHandler { call, result ->
            when (call.method) {
                "openLivePushDemo" -> {
                    activity.startActivity(Intent(activity, LivePushDemoActivity::class.java))
                    result.success(null)
                }

                "openLivePullDemo" -> {
                    activity.startActivity(Intent(activity, LivePullDemoActivity::class.java))
                    result.success(null)
                }

                else -> result.notImplemented()
            }
        }
    }
}
