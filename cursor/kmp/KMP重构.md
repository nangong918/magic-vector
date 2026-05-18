# KMP 重构与问题修复（demo/app -> demo/kmp）

本次针对你提出的 5 个问题，已经在 `demo/kmp` 中完成修复与迁移接入，并通过 `./gradlew :androidApp:assembleDebug` 编译验证通过。

---

## 1) 返回键点一下就回桌面

### 根因

`kmp` 当前是单 `MainActivity + Compose` 导航，不是多 Activity 栈。  
原先 `App.kt` 只维护了一个当前路由变量，没有维护页面回退栈；系统返回键会直接结束 `MainActivity`，所以就回桌面。

### 已修复

- 新增路由栈管理：`shared/src/commonMain/kotlin/com/vectordemo/ui/navigation/AppNavigator.kt`
  - `navigate()` 入栈
  - `goBack()` 出栈
  - `resetTo()` 重置栈
- `MainActivity` 接管系统返回键，优先调用 `AppNavigator.goBack()`；只有栈空才真正 finish Activity：
  - `androidApp/src/main/kotlin/com/demo/kmp/MainActivity.kt`
- `App.kt` 改为用栈式导航，不再是单变量覆盖：
  - `shared/src/commonMain/kotlin/com/demo/kmp/App.kt`

结论：这不是 Activity launchMode 配错，而是单 Activity 场景下没有自己维护页面栈。

---

## 2) 网络报错：CLEARTEXT communication to localhost not permitted

### 根因

Android 默认会限制明文 HTTP（尤其 targetSdk 高时更严格），且你的 `kmp` Manifest 里缺少网络权限与 network security config。

### 已修复

- 新增明文放行配置：
  - `androidApp/src/main/res/xml/network_config.xml`
- 在 `Manifest` 中接入 `android:networkSecurityConfig` 并补齐网络/多媒体权限：
  - `androidApp/src/main/AndroidManifest.xml`
  - 已加入 `INTERNET`、`ACCESS_NETWORK_STATE` 等

### 额外说明（很关键）

即使放开了 HTTP，`localhost` 在真机上指向“手机自己”，不是你的开发机。  
如果后端在电脑上，请把 URL 配成电脑局域网 IP（如 `192.168.x.x`）或模拟器场景用 `10.0.2.2`，不要用 `localhost`。

---

## 3) C++ / JNI 迁移是否应放 androidMain

你的理解是对的：KMP 下 C++/JNI 是 Android 专属能力，应该放 Android 侧（`androidApp` 或 `androidMain`），不放 `commonMain`。

### 已迁移

- C++ 源码与 CMake 已迁入：
  - `androidApp/src/main/cpp/**`
- `androidApp` 已开启 `externalNativeBuild + CMake + c++17`：
  - `androidApp/build.gradle.kts`
- JNI 桥接 Kotlin 类（保持原 C++ 符号期望的包名）已补齐：
  - `androidApp/src/main/kotlin/com/vectordemo/manager/KniManager.kt`
  - `androidApp/src/main/kotlin/com/vectordemo/manager/OnReceiveCppMessage.kt`
  - `androidApp/src/main/kotlin/com/vectordemo/domain/entity/kni/KniEntity.kt`
  - `androidApp/src/main/kotlin/com/vectordemo/domain/entity/kni/IntMsg.kt`
- STL/JNI 演示页已接入：
  - `androidApp/src/main/java/com/vectordemo/activity/STLActivity.kt`

---

## 4) AAR 没迁移，Android 功能缺失

### 已迁移

- 已把 `demo/app/aarlib` 下 AAR 复制到：
  - `demo/kmp/androidApp/aarlib/`
- `androidApp` 已通过 `fileTree(...*.aar)` 引入：
  - `androidApp/build.gradle.kts`
- 为直播依赖补齐 `media3` 依赖版本与库：
  - `gradle/libs.versions.toml`

### 已接入调用

- Live Push / Live Pull 原生页面迁入并可启动：
  - `androidApp/src/main/java/com/vectordemo/activity/LivePushDemoActivity.java`
  - `androidApp/src/main/java/com/vectordemo/activity/LivePullDemoActivity.java`
- Camera/YUV 工具类迁入：
  - `androidApp/src/main/java/com/vectordemo/live/**`
- 布局文件迁入：
  - `androidApp/src/main/res/layout/activity_live_push_demo.xml`
  - `androidApp/src/main/res/layout/activity_live_pull_demo.xml`
- `Manifest` 注册了 Live/STL Activity。

---

## 5) Android 上被识别成 iOS，导致大量“不支持”

### 根因

你原来“被识别 iOS”的核心问题其实有两层：

1. `commonMain` 里的页面路由把 `LivePush/LivePull/StlCpp` 统一跳到 `UNSUPPORTED`。  
2. 平台判断虽有 `supportsXxx`，但没有统一的 `platformType` 可读标识，不利于日志/文案/业务排查。

### 已修复

- 新增平台类型定义与 expect/actual：
  - `shared/src/commonMain/kotlin/com/vectordemo/domain/platform/PlatformFeatures.kt`
  - `shared/src/androidMain/.../PlatformFeatures.android.kt` -> `ANDROID`
  - `shared/src/iosMain/.../PlatformFeatures.ios.kt` -> `IOS`
- `MainVm` 使用 `PlatformFeatures.platformType` 生成平台相关文案：
  - `shared/src/commonMain/kotlin/com/vectordemo/viewModel/activity/MainVm.kt`
- `App.kt` 路由已修正，不再把 Android 能力强制打到 `UNSUPPORTED`。
- 增加 Android 原生能力桥接：
  - `shared/src/commonMain/kotlin/com/vectordemo/ui/navigation/NativeFeatureBridge.kt`
  - `androidApp/src/main/java/com/vectordemo/activity/NativeFeatureRouter.kt`
  - Android 点击 Live/STL 时会直接拉起对应原生 Activity。

---

## 本次新增/修改的核心文件

- 导航与返回栈
  - `shared/src/commonMain/kotlin/com/vectordemo/ui/navigation/AppNavigator.kt`
  - `shared/src/commonMain/kotlin/com/demo/kmp/App.kt`
  - `androidApp/src/main/kotlin/com/demo/kmp/MainActivity.kt`
- 平台判断
  - `shared/src/commonMain/kotlin/com/vectordemo/domain/platform/PlatformFeatures.kt`
  - `shared/src/androidMain/kotlin/com/vectordemo/domain/platform/PlatformFeatures.android.kt`
  - `shared/src/iosMain/kotlin/com/vectordemo/domain/platform/PlatformFeatures.ios.kt`
- Android 原生能力桥接
  - `shared/src/commonMain/kotlin/com/vectordemo/ui/navigation/NativeFeatureActions.kt`
  - `shared/src/commonMain/kotlin/com/vectordemo/ui/navigation/NativeFeatureBridge.kt`
  - `androidApp/src/main/java/com/vectordemo/activity/NativeFeatureRouter.kt`
- JNI/C++
  - `androidApp/src/main/cpp/**`
  - `androidApp/src/main/kotlin/com/vectordemo/manager/**`
  - `androidApp/src/main/kotlin/com/vectordemo/domain/entity/kni/**`
  - `androidApp/src/main/java/com/vectordemo/activity/STLActivity.kt`
- AAR/直播
  - `androidApp/aarlib/*.aar`
  - `androidApp/src/main/java/com/vectordemo/activity/LivePushDemoActivity.java`
  - `androidApp/src/main/java/com/vectordemo/activity/LivePullDemoActivity.java`
  - `androidApp/src/main/java/com/vectordemo/live/**`
  - `androidApp/src/main/res/layout/activity_live_push_demo.xml`
  - `androidApp/src/main/res/layout/activity_live_pull_demo.xml`
- 网络与权限
  - `androidApp/src/main/AndroidManifest.xml`
  - `androidApp/src/main/res/xml/network_config.xml`

---

## 验证结果

已执行并通过：

- `./gradlew :androidApp:assembleDebug`

构建成功，说明上述迁移与接入在当前工程可编译通过。

