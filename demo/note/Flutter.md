**Flutter**
====

flutter的版本总是一个问题，因为各种适配原因，flutter版本总是不能使用最新的，
所以维护一套flutterDemo和flutterNew，两个的内容基本一致
主要区别就是flutter新旧版本的区别。

### flutter环境问题


`flutter pub get`的时候遇到flutter锁问题

```shell
Waiting for another flutter command to release the startup lock...
```

JDK版本问题：
```shell
Android Gradle plugin requires Java 17 to run. You are currently using Java 11.
```
解决方法：
```shell
flutter config --jdk-dir="C:\Users\clt\.jdks\dragonwell-17.0.18"

```


### 打包


[flutter三端打包.md](flutter/flutter三端打包.md)


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


#### 调用IOS原生


首先介绍一下IOS，IOS的文件目录结构：
```plaintext
ios/
├── Flutter/                # Flutter 引擎桥接文件（自动生成，无需手动修改）
│   ├── AppFrameworkInfo.plist  # Flutter 框架信息配置（含版本、架构等，自动生成）
│   ├── Generated.xcconfig  # 编译配置（Flutter 自动生成，关联 Flutter SDK 路径/编译参数）
│   └── Flutter-Debug/Release.xcconfig # 分环境编译配置（Debug/Release 模式差异化参数）
├── Runner/                 # iOS 原生代码核心目录（重点关注，高频修改）
│   ├── Assets.xcassets/    # 资源文件（App 图标、启动图、自定义图片资源）
│   ├── Base.lproj/         # 界面布局（Storyboard 可视化布局文件）
│   │   ├── Main.storyboard # 主界面布局（默认被 Flutter 视图覆盖，可自定义原生页面）
│   │   └── LaunchScreen.storyboard # 启动页（App 冷启动时展示，可修改布局/图片）
│   ├── Info.plist          # 应用核心配置（权限、Bundle ID、版本、启动项、URL Scheme 等）
│   ├── AppDelegate.swift   # 应用入口（生命周期管理、Flutter 引擎初始化、平台通道注册）
│   ├── Runner-Bridging-Header.h # OC/Swift 混编桥接文件（OC 代码暴露给 Swift 调用）
│   ├── ViewController.swift # 主控制器（承载 Flutter 视图容器，可扩展原生控件交互）
│   └── Assets/             # 自定义资源（非图标/启动图类资源，如音频、视频、配置文件）
├── Runner.xcodeproj/       # Xcode 项目文件（工程配置，含编译目标、签名、依赖等）
├── Runner.xcworkspace/     # Xcode 工作空间（推荐打开入口，整合项目+Pods 依赖）
└── Podfile/Podfile.lock    # CocoaPods 依赖配置（第三方库版本/依赖关系，lock 为锁定文件）
```



一般的IOS项目中IOS继承的是`UIResponder, UIApplicationDelegate`。
而在Flutter中，继承的是`FlutterAppDelegate`

flutter调用IOS没有Android和鸿蒙的`flutterEngine`和`configureFlutterEngine`
需要使用另一种写法：
打开`AppDelegate.swift`，这是ios的主要控制页面
主页面进行方法注册
```swift
import UIKit
import Flutter
import SystemConfiguration
import SystemConfiguration.CaptiveNetwork
import CoreLocation  // 新增：iOS 13+ 需要位置权限获取 WiFi 信息

@UIApplicationMain
@objc class AppDelegate: FlutterAppDelegate {
  var wifiMethodChannel: FlutterMethodChannel?
  var locationManager: CLLocationManager?  // 新增：位置管理器
  var wifiPermissionCompletion: ((Bool) -> Void)?  // 新增：权限回调
  override func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
  ) -> Bool {
    GeneratedPluginRegistrant.register(with: self)
    let vc = self.window?.rootViewController as! FlutterViewController

    // 新增的 WiFi Channel
    self.wifiMethodChannel = FlutterMethodChannel.init(
        name: "com.clt.kylin/ios_wifi",
        binaryMessenger: vc.binaryMessenger
    )

    self.wifiMethodChannel!.setMethodCallHandler{(call, result) in
        switch call.method {
        case "isWifiConnect":
            self.checkWifiStatus { isConnected in
                result(isConnected)
            }

        case "getWifiRssi":
            self.getWifiRssiValue { rssi in
                result(rssi)
            }

        default:
            result(FlutterMethodNotImplemented)
        }
    }

    // 初始化位置管理器（用于获取 WiFi 信息）
    self.locationManager = CLLocationManager()
    self.locationManager?.delegate = self

    return super.application(application, didFinishLaunchingWithOptions: launchOptions)
  }

    // WiFi 连接状态检测（使用异步方式）
    private func checkWifiStatus(completion: @escaping (Bool) -> Void) {
        // iOS 13+ 需要检查位置权限
        self.checkLocationPermission { [weak self] hasPermission in
            guard hasPermission else {
                completion(false)
                return
            }

            guard let interfaces = CNCopySupportedInterfaces() as? [String] else {
                completion(false)
                return
            }

            for interface in interfaces {
                guard let interfaceInfo = CNCopyCurrentNetworkInfo(interface as CFString) as? [String: AnyObject] else {
                    continue
                }

                // 如果能够获取到网络信息，说明已连接 WiFi
                if let ssid = interfaceInfo[kCNNetworkInfoKeySSID as String] as? String,
                   !ssid.isEmpty {
                    completion(true)
                    return
                }
            }
            completion(false)
        }
    }

    // 获取 WiFi 信号强度
    private func getWifiRssiValue(completion: @escaping (Int?) -> Void) {
        // 注意：在 iOS 13+ 中，CNCopyCurrentNetworkInfo 不再返回 RSSI
        completion(-50)
    }

    // 检查位置权限（iOS 13+ 获取 WiFi 信息需要）
    private func checkLocationPermission(completion: @escaping (Bool) -> Void) {
        let status: CLAuthorizationStatus

        if #available(iOS 14.0, *) {
            status = self.locationManager?.authorizationStatus ?? .notDetermined
        } else {
            status = CLLocationManager.authorizationStatus()
        }

        switch status {
        case .authorizedAlways, .authorizedWhenInUse:
            // 已有权限
            completion(true)

        case .notDetermined:
            // 请求权限
            self.wifiPermissionCompletion = completion
            self.locationManager?.requestWhenInUseAuthorization()

        case .denied, .restricted:
            // 用户拒绝或受限
            print("位置权限被拒绝或受限，无法获取 WiFi 信息")
            completion(false)

        @unknown default:
            completion(false)
        }
    }
}

// 当调用 locationManager.requestWhenInUseAuthorization() 请求位置权限时，系统会弹出一个权限对话框。
extension AppDelegate: CLLocationManagerDelegate {
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        switch status {
        case .authorizedAlways, .authorizedWhenInUse:
            self.wifiPermissionCompletion?(true)

        case .denied, .restricted, .notDetermined:
            self.wifiPermissionCompletion?(false)

        @unknown default:
            self.wifiPermissionCompletion?(false)
        }
        self.wifiPermissionCompletion = nil
    }
}
```

获取wifi状态的原生ios代码：
```dart
import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

class IosWifiInfo {

  IosWifiInfo._privateConstructor();

  static final IosWifiInfo _instance = IosWifiInfo._privateConstructor();


  factory IosWifiInfo() {
    return _instance;
  }

  late final MethodChannel _channel = const MethodChannel('com.clt.kylin/ios_wifi');

  /// 检查是否连接 WiFi
  Future<bool> isWifiConnect() async {
    try {
      final bool result = await _channel.invokeMethod('isWifiConnect');
      return result;
    } on PlatformException catch (e) {
      debugPrint("Failed to check WiFi connection: '${e.message}'.");
      return false;
    }
  }

  /// 获取 WiFi 信号强度 (RSSI)
  Future<int?> getWifiRssi() async {
    try {
      final int result = await _channel.invokeMethod('getWifiRssi');
      return result;
    } on PlatformException catch (e) {
      debugPrint("Failed to get WiFi RSSI: '${e.message}'.");
      return null;
    }
  }
}
```
调用的话直接跟之前一样就好了，需要使用`await`

权限补充, 注意IOS13之后需要动态权限申请了。
```xml
    <!-- 新增以下 WiFi 权限配置 -->
<key>NSLocationWhenInUseUsageDescription</key>
<string>需要获取WiFi信息</string>

<key>NSLocationAlwaysUsageDescription</key>
<string>需要获取WiFi信息</string>

<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>需要获取WiFi信息</string>

<key>HotspotConfiguration</key>
<true/>

    <!-- 从 iOS 13 开始需要此权限才能获取 WiFi 信息 -->
<key>NSLocalNetworkUsageDescription</key>
<string>需要检查网络连接状态</string>
```


## Android 过度 Flutter


### 异步网络流程

参考Android的流程：
OkHttp + Retrofit + Kotlin携程 进行响应式异步http、ws请求
Gson进行数据序列化
ViewModel + LiveData在Activity上进行UI更新


Flutter对应流程

| Android 端技术            | Flutter 端技术                       | 核心作用            |
|------------------------|-----------------------------------|-----------------|
| OkHttp3                | Dio（主流）/dart:io（底层）               | HTTP/WSS 请求底层实现 |
| Retrofit2              | retrofit（基于 Dio 封装）               | 注解式接口封装、参数解析    |
| Kotlin 协程（Coroutine）   | Dart 异步（async/await）+ Isolate（可选） | 响应式异步请求         |
| Gson                   | json_serializable（编译期生成）          | JSON 序列化 / 反序列化 |
| ViewModel + LiveData   | Riverpod/Provider/Bloc/GetX       | 跨组件状态管理、UI 更新   |
| Activity/Fragment      | Flutter Widget（StatefulWidget）    | UI 渲染、生命周期管理    |


网络请求首先要创建API接口：
Android使用Retrofit自动注入创建接口：
```kotlin
interface ApiRequest {
    @GET("/agent/getInfo")
    suspend fun getAgentInfo(
        @Query("agentId") agentId: String
    ): BaseResponse<AgentResponse>
}
```
Retrofit创建的接口能自动的将接口地址和字段名称绑定
Flutter






### UI组件

StatefulWidget: 有状态组件

StatelessWidget: 无状态组件

ValueNotifier: 类似Android的LiveData








