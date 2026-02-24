package com.demo.flutternew

import com.demo.flutternew.manager.BatteryBridgeManager
import com.demo.flutternew.manager.OfflineVoiceWakeUpManager
import com.demo.flutternew.manager.VadBridgeManager
import com.demo.flutternew.manager.WifiBridgeManager
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {

    private lateinit var wifiBridgeManager: WifiBridgeManager
    private lateinit var batteryBridgeManager: BatteryBridgeManager
    private lateinit var offlineVoiceWakeUpManager: OfflineVoiceWakeUpManager
    private lateinit var vadBridgeManager: VadBridgeManager

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        wifiBridgeManager = WifiBridgeManager(this)
        batteryBridgeManager = BatteryBridgeManager(this)
        offlineVoiceWakeUpManager = OfflineVoiceWakeUpManager(this)
        vadBridgeManager = VadBridgeManager(this)

        wifiBridgeManager.registerWith(flutterEngine)
        batteryBridgeManager.registerWith(flutterEngine)
        offlineVoiceWakeUpManager.registerWith(flutterEngine)
        vadBridgeManager.registerWith(flutterEngine)
    }

    override fun onStart() {
        super.onStart()
        if (::offlineVoiceWakeUpManager.isInitialized) {
            offlineVoiceWakeUpManager.onStart()
        }
        if (::vadBridgeManager.isInitialized) {
            vadBridgeManager.onStart()
        }
    }

    override fun onStop() {
        if (::offlineVoiceWakeUpManager.isInitialized) {
            offlineVoiceWakeUpManager.onStop()
        }
        if (::vadBridgeManager.isInitialized) {
            vadBridgeManager.onStop()
        }
        super.onStop()
    }

    override fun onDestroy() {
        if (::offlineVoiceWakeUpManager.isInitialized) {
            offlineVoiceWakeUpManager.onDestroy()
        }
        if (::vadBridgeManager.isInitialized) {
            vadBridgeManager.onDestroy()
        }
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (::offlineVoiceWakeUpManager.isInitialized) {
            offlineVoiceWakeUpManager.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
            )
        }
        if (::vadBridgeManager.isInitialized) {
            vadBridgeManager.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
            )
        }
    }
}