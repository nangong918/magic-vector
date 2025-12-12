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

#### 调用鸿蒙原生

首先需要使用鸿蒙的定制flutter SDK：flutter-ohos

用鸿蒙定制的flutter SDK创建项目之后会生成一个ohos的文件夹，内部就是鸿蒙的原生代码

需要下载`DevEce Studio`。使用`DevEce Studio`打开鸿蒙文件夹

鸿蒙原生中一般会有一个`EntryAbility`相当于Android的`MainActivity`。
鸿蒙的也是跟Android一样，主Ability继承的不是`UIAbility`而是`FlutterAbility`
在`FlutterAbility`有个方法跟Android一样需要实现，`configureFlutterEngine(flutterEngine: FlutterEngine)`
需要导入`import { FlutterAbility, FlutterEngine } from '@ohos/flutter_ohos';`

与Android不同，在执行鸿蒙原生的时候需要先进行build编译，生成`GeneratedPluginRegistrant`然后将内容注册进去`GeneratedPluginRegistrant.registerWith(flutterEngine)`
生成插件之后需要使用`this.addPlugin`将插件注册到鸿蒙的flutterEngine中

完整代码：
```ets
export default class EntryAbility extends FlutterAbility {
  configureFlutterEngine(flutterEngine: FlutterEngine) {
    super.configureFlutterEngine(flutterEngine)
    GeneratedPluginRegistrant.registerWith(flutterEngine)
    // 插件注册
    this.addPlugin(new FlutterChannelPlugin())
  }
}
```


在写插件的时候需要继承`FlutterPlugin`
继承参考：
`import {
FlutterPluginBinding,
FlutterPlugin,
MethodCall,
MethodChannel,
MethodResult,
} from "@ohos/flutter_ohos"`

在`FlutterPlugin`中主要需要实现的代码是：`onAttachedToEngine`
内部需要进行Channel注册并且实现`onMethodCall`，大致跟Android相同

以Wifi为例，完整代码：
```ets
export class FlutterChannelPlugin implements FlutterPlugin{
  private channel?: MethodChannel;

  getUniqueClassName(): string {
    return 'FlutterChannelPlugin';
  }

  onAttachedToEngine(binding: FlutterPluginBinding): void {
    // 注册channel
    this.channel = new MethodChannel(binding.getBinaryMessenger(),'com.clt.kylin/flutterPlugin')
    this.channel.setMethodCallHandler({
      // 包含异步的话需要使用async标注
      onMethodCall: async (call: MethodCall, result: MethodResult) => {
        switch (call.method) {
          case 'isWifiConnected':
            const isConnect: boolean = await WifiController.isWifiConnected()
            result.success(isConnect);
            break;
          case 'getWifiRssi':
            const rssi: number = await WifiController.getWifiRssi()
            result.success(rssi);
            break;
        }
      }
    });
  }

  onDetachedFromEngine(binding: FlutterPluginBinding): void {
    this.channel?.setMethodCallHandler(null)
  }
}
```

```ets
// wifi相关代码：
import wifi from '@ohos.wifi';

export class WifiController {

  // 检查WiFi连接状态
  public static async isWifiConnected(): Promise<boolean> {
    try {
      const info = await wifi.getLinkedInfo();
      console.debug("info: " + info + " info.connState: " + info.connState + " info.rssi: " + info.rssi + "wifi.ConnState.CONNECTED: " + wifi.ConnState.CONNECTED)
      return info !== null && info.connState === wifi.ConnState.CONNECTED;
    } catch (error) {
      console.error('获取WiFi连接状态失败:', JSON.stringify(error));
      return false;
    }
  }

  // 获取WiFi信号强度
  public static async getWifiRssi(): Promise<number> {
    try {
      const info = await wifi.getLinkedInfo();
      if (info && info.connState === wifi.ConnState.CONNECTED) {
        return info.rssi || -100; // 返回RSSI值，如果没有则返回-100
      }
      return -100;
    } catch (error) {
      console.error('获取WiFi RSSI失败:', JSON.stringify(error));
      return -100;
    }
	}
}
```


dart调用
```dart
class WifiChannel {
  WifiChannel._privateConstructor();

  static final WifiChannel _instance = WifiChannel._privateConstructor();

  factory WifiChannel() {
    return _instance;
  }

  // 创建Channel
  late final MethodChannel _channel = const MethodChannel('com.clt.kylin/flutterPlugin');

  // 异步获取数据
  Future<bool> isConnectedToWifi() async {
    try {
      final bool result = await _channel.invokeMethod('isWifiConnected');
      debugPrint('isConnectedToWifi result = $result');
      return result;
    } on PlatformException {
      debugPrint(' isConnectedToWifi PlatformException');
      return false;
    }
  }

  // 异步获取数据
  Future<int?> getWifiRssi() async {
    try {
      final int? result = await _channel.invokeMethod('getWifiRssi');
      debugPrint('getWifiRssi result = $result');
      return result;
    } on PlatformException {
      return null;
    }
  }
}
```

在调用的时候需要使用await等待异步完成
```dart
    if (Platform.isOhos) {
      // await等待完成
      final connected = await WifiChannel().isConnectedToWifi();
      if (connected) {
        final rssi = await WifiChannel().getWifiRssi();
        GlobalEventBus.eventBus.fire(NetEvent(NetEvent.pingValue,
            msg: rssi ?? NetEvent.noSignal));
      }
      else {
        GlobalEventBus.eventBus.fire(NetEvent(NetEvent.pingValue,
            msg: NetEvent.noSignal));
      }
    }
```

当然在获取wifi信息的时候会要求添加权限，鸿蒙的权限文件entry->src->main->`module.json5`中
获取wifi权限需要进行配置：

```json5
{
  "module": {
    "requestPermissions": [
      {
        "name": "ohos.permission.INTERNET"
      },
      {
        "name": "ohos.permission.GET_NETWORK_INFO"
      },
      {
        "name": "ohos.permission.GET_WIFI_INFO"
      },
    ]
  }
}
```
需要上述三个声明权限，不然无法获得wifi信息










