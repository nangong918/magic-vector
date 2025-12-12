**Flutter学习笔记**
====




### 组件相关

StatefulWidget: 有状态组件

StatelessWidget: 无状态组件

ValueNotifier: 类似Android的LiveData


### Flutter调用原生

Flutter调用Android，IOS，ohos原生方法
使用MethodChannel

#### 调用Android原生

Android的代码中一般有`MainActivity`，其中就是放置原生调用接口的地方
这个`MainActivity`不是继承`AndroidX`或者`Compose`的`Activity`
一般会先导入`import io.flutter.embedding.android.FlutterActivity`

Flutter调用的本质其实就是反射，所以需要创建`MethodChannel`对外暴露方法，然后注册频道。调用的时候就会根据注册的频道检查内部暴露的方法。

创建频道方法的时候需要`FlutterEngine`和`MethodChannel`
需要重写`FlutterActivity`中的`configureFlutterEngine`方法，然后通过`call.method`反射的携带值进行方法分类调用。最后用result将结果返回给flutter，如果是异步的就需要在Android端等待到结果，然后在flutter端执行`await`

参考代码，demo是一个用原生获取wifi信号强度的方法：
```kotlin
class MainActivity: FlutterActivity() {
    private val WIFI_CHANNEL = "com.demo.flutter3_app/wifi"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        // ========== WiFi信号强度调用逻辑 ==========
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, WIFI_CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                // Flutter端调用的方法名：getWifiSignalStrength
                "getWifiSignalStrength" -> {
                    val signalStrength = getWifiSignalStrength()
                    if (signalStrength != -999) { // 自定义兜底值
                        result.success(signalStrength)
                    } else {
                        result.error("UNAVAILABLE", "WiFi信号强度获取失败", null)
                    }
                }
                // 可选：新增获取WiFi是否连接的方法
                "isWifiConnected" -> {
                    val isConnected = isWifiConnected()
                    result.success(isConnected)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    // ========== 获取WiFi信号强度（返回dBm值，负数，如-65） ==========
    private fun getWifiSignalStrength(): Int {
        // 1. 获取WifiManager实例
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        // 2. 检查WiFi是否开启
        if (!wifiManager.isWifiEnabled) {
            return -999 // 未开启WiFi，返回兜底值
        }

        // 3. 获取当前WiFi连接信息
        val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 推荐用法
            wifiManager.connectionInfo
        } else {
            // 低版本兼容
            wifiManager.connectionInfo
        }

        // 4. 获取信号强度（RSSI：Received Signal Strength Indicator，单位dBm）
        return wifiInfo?.rssi ?: -999 // 未连接WiFi返回-999
    }

    // ========== 判断WiFi是否已连接 ==========
    private fun isWifiConnected(): Boolean {
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wifiManager.isWifiEnabled) return false

        val wifiInfo = wifiManager.connectionInfo
        // 判断是否已分配IP（已连接的标志）
        return wifiInfo.ipAddress != 0
    }
}
```

在flutter端进行调用：
需要先创建与原生一致的频道注册，
然后定义调用方法，如果原生端异步的话，需要使用await进行等待
```dart

  // ========== WiFi相关 MethodChannel（与Android原生端保持一致） ==========
  static const wifiPlatform = MethodChannel('com.demo.flutter3_app/wifi');

  // ========== 调用Android原生获取WiFi信号强度 ==========
  Future<void> _callAndroidWifiNative() async {
    try {
      // 调用Android原生WiFi信号强度方法
      final int result = await wifiPlatform.invokeMethod('getWifiSignalStrength');
      // 转换信号强度为更易读的格式（dBm值 + 信号等级）
      String signalDesc = _getWifiSignalDesc(result);
      setState(() {
        _wifiSignalStrength = "Android原生返回：$result dBm ($signalDesc)";
      });
    } on PlatformException catch (e) {
      // 处理调用失败的情况
      setState(() {
        _wifiSignalStrength = "WiFi调用失败: '${e.message}'";
      });
    }
  }
```

当然如果涉及到权限需要在AndroidManifest.xml中进行权限申明
```xml
<!-- 基础权限：获取 WiFi 状态（所有版本必需） -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- 可选：若需要主动操作 WiFi（如开启/关闭、连接热点），需添加 -->
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />

<!-- 关键：Android 6.0（API 23）+ 需动态申请的权限 -->
<!-- 场景1：获取 WiFi SSID/BSSID（需定位权限，因 SSID 属于敏感位置信息） -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<!-- 兼容低版本：粗定位（可选，优先用精定位） -->
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

<!-- 特殊场景：Android 10（API 29）+ 仅获取 WiFi 状态（无需定位）的替代权限 -->
<!-- 注意：仅适用于 Android 10+，且需关闭定位也能获取基础 WiFi 状态时使用 -->
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- 可选：Android 12（API 31）+ 获取附近 WiFi 列表（如扫描热点） -->
<uses-permission android:name="android.permission.ACCESS_WIFI_SCAN"
    android:usesPermissionFlags="neverForLocation" />
```





