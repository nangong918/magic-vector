# Live功能实现报告

## 1. 本次任务完成情况自检

| 任务 | 状态 | 说明 |
| --- | --- | --- |
| 阅读并理解 `flutteraar` 项目结构与 `README.md` | 已完成 | 已按 `aarlib` 作为 SDK、`app` 作为 Android Demo 的方式落地。 |
| 将 `LiveActivity.kt` 的核心推流能力迁移到 SDK | 已完成 | 已把 `RTMP + x264 + faac(AAC)` 实时推流核心下沉到 `aarlib`，对外提供 Java Bridge。 |
| 区分 SDK 与 App 职责 | 已完成 | `aarlib` 只保留推流核心、JNI、FFmpeg 推流；`app` 保留 `Camera2`、预览、权限、页面交互。 |
| 将 FFmpeg 推流核心迁移到 `aarlib` | 已完成 | 已在 `aarlib` 增加 `FFmpegPushBridge` 与对应 native `FFmpeg RTMP` 推流实现。 |
| 在 `app` 写 Android Demo 验证 | 已完成，待你本地编译运行验证 | 已新增 Demo 页面，支持 Camera2 实时推流和 FFmpeg 文件推流。你已说明编译由你执行，因此当前状态以代码完成为准。 |
| 输出 Flutter 是否可直接实现 live 推/拉流方案分析 | 已完成 | 本报告第 4~7 节给出 Flutter 生态、Android/iOS 适配性、性能对比与建议。 |
| 输出“Live功能实现报告”并放到项目中 | 已完成 | 当前文件即本报告。 |

## 2. 本次已落地的代码改动

### 2.1 SDK 侧：`aarlib`

- 新增实时推流 Bridge：
  - `aarlib/src/main/java/com/demo/aarlib/live/LivePusherBridge.java`
  - `aarlib/src/main/java/com/demo/aarlib/live/LivePushConfig.java`
  - `aarlib/src/main/java/com/demo/aarlib/live/LivePushListener.java`
  - `aarlib/src/main/java/com/demo/aarlib/live/LiveErrorCode.java`
  - `aarlib/src/main/java/com/demo/aarlib/live/LiveFrameFormat.java`
- 新增 FFmpeg 文件推流 Bridge：
  - `aarlib/src/main/java/com/demo/aarlib/live/ffmpeg/FFmpegPushBridge.java`
- 补齐 native 实现与 CMake：
  - `aarlib/src/main/cpp/CMakeLists.txt`
  - `aarlib/src/main/cpp/AudioStream.cpp`
  - `aarlib/src/main/cpp/RtmpPusher.cpp`
  - `aarlib/src/main/cpp/ff_rtmp_pusher.h`
  - `aarlib/src/main/cpp/ff_rtmp_pusher.cpp`
  - `aarlib/src/main/cpp/ffmpeg_pusher_jni.cpp`
- 补齐预编译依赖接入：
  - `aarlib/src/main/jniLibs/arm64-v8a/libffmpeg.so`
  - `aarlib/src/main/jniLibs/armeabi-v7a/libffmpeg.so`
  - `aarlib/src/main/cpp/include/libav*` 等 FFmpeg 头文件
- 更新 `aarlib/build.gradle`：
  - 增加 `externalNativeBuild`
  - 增加 `ndk abiFilters`
  - 增加 `jniLibs` 目录声明

### 2.2 App 侧：`app`

- 新增直播 Demo 页面：
  - `app/src/main/java/com/example/flutteraar/ui/activity/LivePushDemoActivity.java`
  - `app/src/main/res/layout/activity_live_push_demo.xml`
- 新增 Demo 使用的 Camera2 采集代码：
  - `app/src/main/java/com/example/flutteraar/live/camera/Camera2Helper.java`
  - `app/src/main/java/com/example/flutteraar/live/camera/Camera2Listener.java`
  - `app/src/main/java/com/example/flutteraar/live/util/YuvUtil.java`
- 更新页面入口：
  - `app/src/main/java/com/example/flutteraar/MainActivity.java`
- 更新权限与 Activity 注册：
  - `app/src/main/AndroidManifest.xml`

## 3. 当前 SDK 与 App 的职责边界

### 3.1 归入 SDK 的能力

本次按你的要求，下沉到 `aarlib` 的是这些“Flutter 不方便直接做 JNI、而 Android Native 更适合沉淀”的能力：

- `RTMP` 连接与发包
- `x264` 视频编码
- `faac/AAC` 音频编码
- `FFmpeg` 媒体源读取与 RTMP 推流
- Java 层对 native 的 Bridge 封装

### 3.2 明确留在 App 的能力

这些没有放进 SDK，而是留在 `app` Demo，目的是后续 Flutter 或 Android 都可自行实现：

- `Camera2` 相机采集
- 预览视图 `TextureView`
- 权限申请
- 页面交互
- 推流按钮、状态文本、地址输入框
- 摄像头切换

### 3.3 这样划分的原因

这是为了保持 AAR 的职责单一：

- 让 Flutter 只调一个稳定的 Android SDK 接口，不碰复杂 JNI。
- 避免把预览控件、相机 UI、Activity 生命周期绑死在 SDK 内。
- 后续如果你做 iOS，可用同样的“上层 Flutter 页面 + 平台侧 Native SDK”模式保持接口一致。

## 4. Flutter 是否可以直接实现 Live 推流和拉流

### 4.1 结论

可以，但目前 **没有 Flutter 官方（Google 官方）提供的统一 live 推流/拉流 SDK**。  
Flutter 侧主要依赖第三方或厂商插件来实现：

- RTMP 推流类：
  - `rtmp_broadcaster`
  - `rtmp_stream`
- WebRTC / 实时互动类：
  - `flutter_webrtc`
  - `livekit_client`
  - Ant Media Flutter SDK
- 商业直播方案：
  - `zego_uikit_prebuilt_live_streaming`
  - Tencent `live_flutter_plugin`
  - Agora / ZEGO / TRTC 等厂商 Flutter SDK

也就是说：

- **Flutter 能做 live**
- **但不是“官方统一库”**
- **真正底层仍然是各平台 Native SDK 或第三方原生库**

### 4.2 Flutter 侧不同能力的适配性

| 方向 | Android | iOS | 说明 |
| --- | --- | --- | --- |
| Flutter RTMP 推流插件 | 较成熟 | 可用 | 常见实现是 Android 走 `RootEncoder/rtmp-rtsp-stream-client-java`，iOS 走 `HaishinKit`。 |
| Flutter WebRTC 插件 | 很成熟 | 很成熟 | 更适合超低延迟互动直播、连麦、会议。 |
| Flutter 直接做 FFmpeg/x264 JNI 级能力 | 一般 | 差 | Flutter 本身不适合直接承载复杂 JNI / C++ / NDK 细节。 |
| Flutter 作为跨平台 UI，平台各自实现 native live SDK | 很适合 | 很适合 | 这是更稳的工程化方式。 |

## 5. Flutter、Android Native、RK 设备三种视角下的建议

### 5.1 Flutter 跨平台 App 视角

如果目标是：

- Android / iOS 双端快速上线
- UI 统一
- 需求以“业务直播功能”为主

那么 Flutter 最合适的路径通常是：

1. Flutter 页面负责 UI
2. Android 和 iOS 分别接各自 Native 推拉流 SDK
3. Flutter 用 `MethodChannel` / plugin 调用

如果是纯第三方直播产品能力，甚至可以直接接：

- ZEGO
- Agora
- TRTC
- LiveKit

优点：

- 双端开发效率高
- iOS 容易补齐
- 厂商 SDK 在网络适配、弱网恢复、音视频 QoS 上更成熟

缺点：

- 底层可控性较弱
- 很难完全按照你的 `RTMP + x264 + FFmpeg` 技术栈统一
- 对后续 RK 系统烧录、深度定制不一定友好

### 5.2 Android 原生系统 App / RK 设备视角

如果目标是：

- 后续做系统级 Android App
- 烧入 RK 设备
- 深度控制编码链路、RTMP、FFmpeg、JNI、native 性能
- 可裁剪、可替换、可离线部署

那么你这次选择的方向是对的：

- 以 Android Native 为主
- 用 AAR 做能力封装
- 保留 `RTMP + x264 + FFmpeg` 这条可控链路

这条链路的价值在于：

- 对 RK 设备、嵌入式环境更可控
- 不依赖云厂商闭源直播 SDK
- 可以按设备能力逐步演进成：
  - 软件编码
  - FFmpeg 转封装
  - MediaCodec 硬编
  - RTMP / SRT / WebRTC 多协议扩展

### 5.3 对你当前项目的最优建议

建议未来分两层：

- **第一层：通用 AAR Live SDK**
  - RTMP
  - x264 / AAC
  - FFmpeg
  - 后续可扩展 MediaCodec
- **第二层：平台业务壳**
  - Android App Demo
  - Flutter Android plugin
  - 未来 iOS Native SDK + Flutter iOS plugin

这样你既能满足 RK 系统设备，又不堵死 Flutter / iOS 路径。

## 6. 不同方案性能与适配性对比

### 6.1 方案对比总表

| 方案 | 延迟 | CPU/功耗 | 图像质量可控性 | Android | iOS | RK/系统App适配 | 说明 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 当前方案：RTMP + x264 + AAC(faac) + JNI | 中低 | 高 | 很高 | 很好 | 需重做 | 很好 | 可控、适合深度定制，但软件编码更吃 CPU。 |
| FFmpeg 文件推流 / 转封装 | 低到中 | 低到中 | 中 | 很好 | 需重做 | 很好 | 如果输入本身已是 H.264/AAC，`remux` 非常省资源。 |
| MediaCodec 硬编 + RTMP | 低 | 低 | 中等 | 很好 | iOS 对应为 VideoToolbox | 很好 | 实时直播在 Android 上通常更优，功耗和吞吐明显更好。 |
| Flutter RTMP 插件 | 中低 | 取决于底层原生实现 | 中 | 好 | 好 | 一般 | 开发快，但底层控制力较弱。 |
| Flutter WebRTC 方案 | 很低 | 中 | 中高 | 很好 | 很好 | 一般 | 更适合互动直播、连麦、超低延迟，不是传统 RTMP 单向直播的最佳替代。 |
| 商业直播 SDK（ZEGO/TRTC/Agora） | 很低 | 优 | 中 | 很好 | 很好 | 一般 | 最快上线，但不适合作为你 RK 原生控制链路的唯一方案。 |

### 6.2 关于 `x264` 与 `MediaCodec` 的核心差异

#### `x264` 软件编码

优点：

- 参数控制最细
- 码率、GOP、profile、zerolatency 行为更容易深度调优
- 在某些场景下质量更稳定
- 更适合需要完全掌控编码行为的系统设备

缺点：

- CPU 占用高
- 长时间推流更容易发热
- 在移动端续航压力大
- 高分辨率实时推流时比硬编更容易掉帧

#### `MediaCodec` 硬编码

优点：

- 延迟通常更低
- CPU 与功耗显著更优
- 对长时间直播、移动设备更友好
- Android 端更适合做大分辨率实时推流

缺点：

- 各芯片行为不完全一致
- 编码质量和参数可控性一般不如 `x264`
- 某些厂商设备低延迟模式表现不稳定

### 6.3 关于 FFmpeg 推流与实时编码推流

#### FFmpeg 文件推流

适合：

- 本地文件推流
- 网络媒体源转推
- 已编码流 `copy/remux` 到 RTMP

优势：

- 如果输入就是 H.264/AAC，可只做封装转换，CPU 开销很低
- 非常适合中转、录播转直播、拉一路推一路

局限：

- 它不天然等于“实时摄像头直播”
- 如果做实时采集再走 FFmpeg 软件编码，本质上还是会有明显 CPU 开销

#### 实时摄像头推流

更典型路径是：

- Camera / AudioRecord 采集
- 编码
- RTMP 发包

如果继续使用你当前链路：

- Camera2 + AudioRecord 在 App
- `x264/faac/RTMP` 在 SDK

这就是最符合你当前工程目标的方式。

## 7. 对“Flutter 有没有更好方法”的最终判断

### 7.1 如果目标是“跨平台最快实现”

Flutter 确实有更省事的方法：

- 直播推流：`rtmp_stream` / `rtmp_broadcaster`
- 互动直播：`flutter_webrtc` / `livekit_client`
- 商业化能力：ZEGO / TRTC / Agora Flutter SDK

对于 Android + iOS 业务 App，这些方案会比你自己维护 `JNI + x264 + FFmpeg + RTMP` 更轻松。

### 7.2 如果目标是“RK 系统设备、原生能力沉淀、技术栈可控”

Flutter 不是更好的底层方案。  
它更适合作为上层 UI 壳，而不是底层音视频核心能力承载层。

对你当前目标而言，最佳判断是：

- **业务跨平台层面：Flutter 有更快方案**
- **底层可控与 RK 系统层面：当前 Native AAR 方案更合适**

### 7.3 最推荐的长期路线

建议分阶段推进：

1. **先把 Android AAR Live SDK 稳定下来**
   - 修完编译问题
   - 跑通 RTMP 实时推流
   - 跑通 FFmpeg 文件推流
2. **第二阶段补 Android Flutter plugin**
   - Flutter 页面调用 Android AAR
   - 先只做 Android
3. **第三阶段补 iOS Native Live SDK**
   - iOS 侧对齐接口
   - Flutter 再统一暴露跨平台 API
4. **第四阶段考虑 MediaCodec/VideoToolbox**
   - Android：MediaCodec
   - iOS：VideoToolbox
   - 软件编码只保留为兜底或特定场景

## 8. 当前实现的客观说明

本次我已经把你要求的核心结构和代码落地到项目中，但还有两点必须如实说明：

1. **我没有执行最终 Gradle 编译验证**
   - 这是因为你已明确说明由你本地编译并把问题再反馈给我。
2. **当前 iOS Native 版本没有实现**
   - 本次只完成 Android AAR 方向与方案报告。

如果你后续把编译错误、NDK/CMake 错误、so 打包错误、运行时报错发给我，我会继续沿当前结构修到可用。

## 9. 本次新增：Live Pull 拉流播放 Demo（对应 CursorQuestion 任务）

### 9.1 已完成内容

本次已在 `flutteraar` 落地独立拉流页：

- `app/src/main/java/com/example/flutteraar/ui/activity/LivePullDemoActivity.java`
- `app/src/main/res/layout/activity_live_pull_demo.xml`
- `MainActivity` 增加 `Live Pull Demo` 入口
- `AndroidManifest.xml` 增加 Activity 注册
- `gradle/libs.versions.toml` 与 `app/build.gradle` 增加 Media3 依赖

拉流页支持两种路径：

1. **RTMP 直拉播放**（`rtmp://IP:1935/stream/live`）  
   - 通过 `androidx.media3:media3-datasource-rtmp` 提供 RTMP DataSource；
2. **HLS 播放**（`http://IP:8080/hls/live/index.m3u8`）  
   - 通过 `media3-exoplayer-hls` 播放 nginx 输出的 m3u8。

### 9.2 对 `VideoPreviewActivity.kt` 的复用评估（你要求检查的本地工程）

检查了你给出的：

- `C:/Github/FFmpegAndroid-master/app/src/main/java/com/frank/ffmpeg/activity/VideoPreviewActivity.kt`
- `C:/Github/FFmpegAndroid-master/app/src/main/java/com/frank/ffmpeg/controller/MediaPlayController.kt`

结论：

- 该实现核心是 Android `MediaPlayer.setDataSource(filePath)` + `SurfaceView`。
- 更偏向“文件/普通 URL 播放 + 预览条控制”场景。
- 没有现成 RTMP DataSource 封装，不适合直接复用为当前 `RTMP + x264/AAC` 拉流 Demo。
- 因此本次采用 `Media3` 重新实现独立 `Live Pull Demo`，与当前 `flutteraar` 架构更一致。

### 9.3 播放器选型与上网核对结论

基于 Android 官方 Media3 文档与 API 说明核对后，本次选择：

- `media3-exoplayer` + `media3-ui` 负责播放和控件；
- `media3-exoplayer-hls` 负责 HLS；
- `media3-datasource-rtmp` 负责 RTMP 数据源接入。

原因：

- 对 Android Demo 集成成本最低；
- 不需要再引入额外 FFmpeg 播放器 SDK；
- 同时覆盖你当前链路中的 RTMP 和 nginx HLS；
- 后续如果要接入 iOS，可在 Flutter 插件层保持接口一致，平台侧分别实现。

### 9.4 你本地验证建议

建议按以下顺序验证：

1. 在 `Live Push Demo` 里把流推到：`rtmp://<你的主机IP>:1935/stream/live`；
2. 打开 `Live Pull Demo`，先试 RTMP 地址直拉；
3. 点击“一键转 HLS”后，验证 `http://<你的主机IP>:8080/hls/live/index.m3u8`；
4. 如果 RTMP 不通但 HLS 通，优先检查 1935 端口与局域网访问策略；
5. 如果 HLS 不通，检查 nginx 容器内 `/tmp/hls/live/index.m3u8` 是否生成。

## 10. 本次迁移：同步到 `app` 与 `flutternew`

### 10.1 AAR 同步说明

按你的要求，本次已把 `aarlib-release.aar` 同步到两端消费工程使用：

- `demo/app/aarlib/aarlib-release.aar`
- `demo/flutter/flutternew/android/app/libs/aarlib-release.aar`

说明：

- 当前仓库内未包含 `flutteraar/aarlib/build/outputs/aar` 编译产物目录；
- 因此本次先确保 `app` 与 `flutternew` 使用同一份 `aarlib-release.aar`，避免两端 SDK 版本不一致。

### 10.2 `app`（Kotlin + Compose + MVI）已完成内容

已在 `demo/app` 新增并接入 Live 推拉流 Demo：

- 新增 Activity：
  - `app/src/main/java/com/vectordemo/activity/LivePushDemoActivity.java`
  - `app/src/main/java/com/vectordemo/activity/LivePullDemoActivity.java`
- 新增 Camera2 工具：
  - `app/src/main/java/com/vectordemo/live/camera/Camera2Helper.java`
  - `app/src/main/java/com/vectordemo/live/camera/Camera2Listener.java`
  - `app/src/main/java/com/vectordemo/live/util/YuvUtil.java`
- 新增布局：
  - `app/src/main/res/layout/activity_live_push_demo.xml`
  - `app/src/main/res/layout/activity_live_pull_demo.xml`
- Main 页导航接入：
  - `DemoRoute` 增加 `LIVE_PUSH / LIVE_PULL`
  - `MainVm` 增加入口与 Effect
  - `MainActivity` 增加跳转分发
- Manifest 与依赖：
  - `AndroidManifest.xml` 增加 `CAMERA` 权限和两个 Activity 注册
  - `gradle/libs.versions.toml` 与 `app/build.gradle.kts` 增加 Media3 播放依赖

### 10.3 `flutternew` 已完成内容

#### 10.3.1 Android 原生 Demo（按你要求标注 `(Android)`）

为保持你原有“Flutter 调 Android Native”的迁移方式，已实现：

- Android 原生页面：
  - `android/app/src/main/java/com/demo/flutternew/live/activity/LivePushDemoActivity.java`
  - `android/app/src/main/java/com/demo/flutternew/live/activity/LivePullDemoActivity.java`
- Android Camera2 工具：
  - `android/app/src/main/java/com/demo/flutternew/live/camera/Camera2Helper.java`
  - `android/app/src/main/java/com/demo/flutternew/live/camera/Camera2Listener.java`
  - `android/app/src/main/java/com/demo/flutternew/live/util/YuvUtil.java`
- Android 布局：
  - `android/app/src/main/res/layout/activity_live_push_demo.xml`
  - `android/app/src/main/res/layout/activity_live_pull_demo.xml`
- Flutter 通道桥接：
  - `android/app/src/main/kotlin/com/demo/flutternew/manager/LiveDemoBridgeManager.kt`
  - `MainActivity.kt` 完成 manager 注册
- Flutter 页面（明确标注 Android）：
  - `lib/page/live_push_android_page.dart`
  - `lib/page/live_pull_android_page.dart`
- Flutter 路由/目录接入：
  - `lib/config/app_route.dart`
  - `lib/manager/catalog_manager.dart`

#### 10.3.2 纯 Dart 方案调研结论（Android+iOS）

按你的要求上网核对后，纯 Dart + 插件路线可行：

- **推流（跨平台）**：`rtmp_broadcaster`
  - Android 基于 `rtmp-rtsp-stream-client-java`
  - iOS 基于 `HaishinKit`
- **拉流（跨平台）**：`flutter_vlc_player`
  - 支持 RTMP/HLS 在 Android+iOS 播放

因此满足你第 4 点条件（可实现双平台推流与播放）。

#### 10.3.3 已新增 `(跨平台)` 两个 Demo

本次已在 `flutternew` 落地：

- `lib/page/live_push_cross_platform_page.dart`
  - 基于 `rtmp_broadcaster`，支持摄像头预览、开始/停止推流、切换摄像头
- `lib/page/live_pull_cross_platform_page.dart`
  - 基于 `flutter_vlc_player`，支持 RTMP/HLS 地址播放与停止

并已接入路由与目录入口：

- `Live Push Demo (跨平台)`
- `Live Pull Demo (跨平台)`

同时完成必要依赖与平台配置：

- `pubspec.yaml` 新增：
  - `rtmp_broadcaster: ^2.3.4`
  - `flutter_vlc_player: ^7.4.4`
- `android/app/build.gradle.kts` 增加：
  - Media3 依赖
  - `packaging` 兼容配置（`project.clj` 排除、`libc++_shared.so` pickFirst）
- `android/build.gradle.kts` + `android/settings.gradle.kts` 增加 `jitpack` 仓库
- `ios/Runner/Info.plist` 增加：
  - `NSCameraUsageDescription`
  - `NSMicrophoneUsageDescription`
  - `NSAppTransportSecurity/NSAllowsArbitraryLoads`

补充：

- 该仓库当前未包含 `flutternew/ios/Podfile` 文件；
- 若你本地 iOS 编译遇到插件链接问题，请在本地 Podfile 中确认平台版本与插件要求一致（例如 `platform :ios, '9.0'` 及 Flutter 默认 post_install 配置），然后执行 `pod install`。

## 11. 本次新增：RTSP 文件推流实现结论（flutteraar）

### 11.1 先回答你的关键问题：RTSP 要不要先配 nginx？

结论：**你当前这份 nginx（`nginx-rtmp-module`）不能直接作为 RTSP 服务端**。  
它支持的是 RTMP/HLS 这条链路，不是 RTSP 的 ANNOUNCE/SETUP/RECORD 会话模型。

所以：

- `nginx.conf` 不需要（也无法）通过简单改配置就变成 RTSP 推流服务；
- RTSP 需要独立服务端（推荐 `MediaMTX`，也可用 SRS 的 RTSP 能力）。

### 11.2 我这次是否完成了 RTSP 文件推流？

结论：**已完成一个独立 Demo（不改你原 RTMP Demo 行为）**。

已落地改动：

- 新增独立页面：
  - `app/src/main/java/com/example/flutteraar/ui/activity/LiveRtspFilePushDemoActivity.java`
  - `app/src/main/res/layout/activity_live_rtsp_file_push_demo.xml`
- 主页面新增入口：
  - `app/src/main/java/com/example/flutteraar/MainActivity.java`
- 注册 Activity：
  - `app/src/main/AndroidManifest.xml`
- 底层 FFmpeg 推流从“只支持 RTMP(FLV)”改为“按输出 URL 自动选择 RTMP/RTSP”：
  - `aarlib/src/main/cpp/live/ff_rtmp_pusher.cpp`
  - `aarlib/src/main/cpp/live/ff_rtmp_pusher.h`
  - `aarlib/src/main/java/com/demo/aarlib/live/ffmpeg/FFmpegPushBridge.java`

### 11.3 是否需要新增 C++ 依赖库？

结论：**本次代码层面没有新增第三方 C++ 库**，继续复用你现有 `libffmpeg.so`。  
但有一个前提：你的 `libffmpeg.so` 必须编译进了 `rtsp` 协议/复用器相关能力。

如果本地运行 RTSP 推流时报 `Protocol not found` / `Could not write header`，说明当前 FFmpeg 裁剪配置不含 RTSP 输出能力，需要重编 `libffmpeg.so`。

可参考下载/源码来源：

- FFmpeg 官方源码：[https://ffmpeg.org/download.html](https://ffmpeg.org/download.html)
- 你提供的 `FFmpegAndroid-master` 也可作为 Android 交叉编译脚本参考（其 PushActivity 默认仍走 RTMP 逻辑）。

### 11.4 对你给的 `PushActivity.kt` 的判断

`C:\\Github\\FFmpegAndroid-master\\app\\src\\main\\java\\com\\frank\\ffmpeg\\activity\\PushActivity.kt` 只是调用 `FFmpegPusher().pushStream(filePath, liveUrl)`，  
其 native 侧同样使用 `ff_rtmp_pusher.cpp` 并写死 `flv` 输出格式，因此**它本身不能直接证明 RTSP 已可用**。

### 11.5 你在 Docker(Linux) 上的最小可用建议

推荐新增一个 RTSP 服务容器（如 MediaMTX），例如默认监听 `8554`，然后在新 Demo 填：

- 输入源：`http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4`（或本地 mp4）
- 输出：`rtsp://<你的服务器IP>:8554/live/stream`

这条链路与现有 nginx 的 RTMP/HLS 链路可以并行存在，互不冲突。

## 12. 本次补充完成：按你最新要求落地

### 12.1 RTSP 推流页改为“本地 mp4 选择并推流”

已在 `flutteraar` 完成：

- `app/src/main/java/com/example/flutteraar/ui/activity/LiveRtspFilePushDemoActivity.java`
  - 新增系统文件选择（SAF `OpenDocument`）；
  - 支持点击按钮选择本地视频文件；
  - 选中文件后自动复制到 App 缓存目录并回填可用本地路径；
  - 保留 RTSP 地址校验，直接使用 FFmpeg 推流。
- `app/src/main/res/layout/activity_live_rtsp_file_push_demo.xml`
  - 新增按钮：`选择本地 mp4 文件`。

这样你的 RTSP 方向就变成了“文件推流”链路，不影响原先 Camera2 的 RTMP 实时推流链路。

### 12.2 Docker 环境新增 RTSP 服务（支持两机联调）

已在你现有 compose 中新增 `mediamtx` 服务：

- `demo/springboot/docker/docker-compose.yml`
  - 新增 `mediamtx`（镜像 `bluenviron/mediamtx:latest`）；
  - 暴露端口 `8554`。

并补充文档：

- `demo/springboot/docker/README.md`
- `demo/springboot/nginx-docker/README.md`

明确说明：

- nginx 继续负责 RTMP/HLS；
- RTSP 由 MediaMTX 承担；
- 两者可并行运行。

### 12.3 RTSP 拉流能力实现（复用原拉流 Demo）

没有新建页面，直接复用并增强原 `Live Pull Demo`：

- `app/src/main/java/com/example/flutteraar/ui/activity/LivePullDemoActivity.java`
  - 新增 RTSP 播放逻辑；
  - RTSP 分支使用 `RtspMediaSource.Factory().setForceUseRtpTcp(true)`，优先 TCP，便于局域网/端口场景联调；
  - 新增“一键填写 RTSP 地址”能力（按当前 URL 主机生成 `rtsp://<host>:8554/live/stream`）。
- `app/src/main/res/layout/activity_live_pull_demo.xml`
  - 输入框提示改为 `RTMP/HLS/RTSP`；
  - 新增按钮：`按当前主机一键填写 RTSP 地址`；
  - 页面说明补充 RTSP 示例地址。
- `gradle/libs.versions.toml`
  - 新增依赖项 `media3-exoplayer-rtsp`。
- `app/build.gradle`
  - 新增 `implementation libs.media3.exoplayer.rtsp`。

### 12.4 两台手机同 App 联调步骤（你要的目标流程）

1. 启动服务端（在 `demo/springboot` 目录）：
   - `docker compose -f docker/docker-compose.yml up -d nginx mediamtx`
2. 手机 A：
   - 打开 `RTSP File Push Demo`；
   - 点击“选择本地 mp4 文件”；
   - 输出地址填：`rtsp://<服务器IP>:8554/live/stream`；
   - 点击开始推流。
3. 手机 B：
   - 打开 `Live Pull Demo`；
   - 填同一个地址：`rtsp://<服务器IP>:8554/live/stream`；
   - 点击开始拉流播放。

### 12.5 不影响原 RTMP 能力的确认

本次所有改动均为“新增与扩展”，未删除或替换你原 RTMP 推拉流链路：

- 原 `LivePushDemoActivity`（Camera2 + AudioRecord + RTMP）保持不变；
- 原 nginx RTMP/HLS 配置保持可用；
- 拉流页在保留 RTMP/HLS 的基础上新增 RTSP。

## 13. 实测问题修复记录（RTSP 推/拉流）

### 13.1 现象与根因

你反馈的 `RTSP 推流失败 code=-541478725`，结合 App 日志可定位为：

- `ff_rtmp_pusher` 日志明确出现：`av_read_frame err=-541478725`；
- 该错误是输入文件读到结尾（EOF），属于“文件推流正常结束”，不应当提示失败。

同时，MediaMTX 日志里的 `i/o timeout` 与拉流端音频解码异常，和推流端时间戳处理有关：

- 原实现直接使用文件原始时间戳做 sleep，同步时可能出现长等待；
- RTSP 会话在长等待阶段被服务端判定超时；
- 部分设备会因音频时间戳不连续触发 `AudioSink UnexpectedDiscontinuity`。

### 13.2 本次修复

已修复代码：

- `aarlib/src/main/cpp/live/ff_rtmp_pusher.cpp`
  - 将 `AVERROR_EOF` 视为正常完成（返回 `0`）；
  - 对每路流的 `pts/dts` 做“从 0 开始”的归一化；
  - 限制单次 sleep 上限（200ms），避免长时间无包触发 RTSP 超时；
  - 输出时间戳做单调修正，降低播放器侧时间戳跳变风险；
  - 仅在有效时间戳上做 rescale，避免 `AV_NOPTS_VALUE` 引发异常值。
- `app/src/main/java/com/example/flutteraar/ui/activity/LivePullDemoActivity.java`
  - RTSP 分支默认禁用音频轨（视频优先，提升设备兼容性，规避部分机型 AAC 解码崩溃）。

### 13.3 修复后联调建议

1. 先启动推流端并确认状态显示“RTSP 推流完成/进行中”；
2. 在推流进行阶段尽快启动第二台手机拉流；
3. 使用 `rtsp://<服务器IP>:8554/live/stream`；
4. 若仍异常，优先采集两段日志：
   - `ff_rtmp_pusher` 的 `open/push/write_frame` 日志；
   - `LivePullDemoActivity` 的 `onPlayerError` 栈。

## 14. 本次新增：WebRTC 视频通话 Demo（可行性调研 + 已落地实现）

### 14.1 可行性结论（先回答你“能不能做”）

可以实现，并且已按你当前工程结构落地到仓库：

- Android 端：新增 WebRTC Demo（绑定 ID -> 呼叫 -> 接听 -> 通话页）；
- 后端：在 `springboot` 增加 WebSocket 信令中转；
- 网络穿透：新增 `coturn`（TURN）服务，弱网/跨网段时作为中继；
- 文档：本节给出依赖选型、信令流转、后端部署与联调步骤。

### 14.2 上网调研后的依赖选型结论

#### Android WebRTC 依赖

本次选型：`io.github.webrtc-sdk:android:125.6422.07`

原因：

- 该版本可直接从 Maven Central 获取（避免依赖历史上的 JCenter 分发）；
- API 兼容 `org.webrtc.*`（`PeerConnection`、`SurfaceViewRenderer`、`VideoTrack` 等）；
- 版本更新较新，适合新项目 demo 验证。

#### Android 信令通信依赖

- `com.squareup.okhttp3:okhttp:4.12.0`
  - 用于 WebSocket 连接 SpringBoot 信令服务；
  - 连接稳定、集成简单，适合 Demo。

#### STUN/TURN 结论

- STUN 只负责“探测公网地址”，不能保证所有网络都能打洞成功；
- TURN 用于打洞失败时中继媒体流；
- 因此 Demo 保留两层：
  - STUN：`stun:stun.l.google.com:19302`
  - TURN：本地 docker 的 `coturn`（`turn:<host>:3478`）。

### 14.3 数据流转设计（你关心的“Call 怎么让对方收到”）

#### 角色拆分

- **SpringBoot 信令层**：只中转消息，不处理音视频编码；
- **Android WebRTC 层**：负责采集摄像头/麦克风、SDP 协商、ICE 打洞、媒体传输；
- **TURN 层（coturn）**：在 P2P 不通时中继媒体。

#### 呼叫流程

1. 用户进入 `WebRTC Demo`，弹窗绑定本机 ID；不绑定直接返回。
2. 绑定后，客户端建立：
   - `ws://<host>:48888/ws/webrtc?uid=<selfId>`
3. A 输入 B 的 ID，点击 Call：
   - 发 `call_invite` 给 B。
4. B 收到来电弹窗，点击接听：
   - 发 `call_accept` 给 A；
   - A/B 同时进入 `WebRtcCallActivity`。
5. 双方进入通话页后：
   - 先发 `call_joined`；
   - Caller 在收到对方 `call_joined` 后发 `offer`；
   - Callee 回 `answer`；
   - 双向持续交换 `ice_candidate`。
6. 建链成功后开始音视频传输：
   - 上方本地画面，下方远端画面。
7. 任意一方挂断：
   - 发 `hangup`，双方关闭连接并退出通话页。

### 14.4 后端是否需要？

需要，至少需要“信令后端”。

说明：

- WebRTC 本身不规定信令协议；
- 要实现“输入对方 ID 后对方收到来电”，就必须有一个中心中转消息；
- 你现有 `springboot` 非常适合承担这层。

本次后端新增：

- WebSocket 注册：`/ws/webrtc`
- 信令处理：按 `uid` 路由以下消息：
  - `call_invite`
  - `call_accept`
  - `call_reject`
  - `call_joined`
  - `offer`
  - `answer`
  - `ice_candidate`
  - `hangup`

### 14.5 本次代码落地清单

#### `flutteraar/app` 新增

- `ui/activity/WebRtcDemoActivity.java`
  - 绑定本机 ID、输入对方 ID、发起呼叫、接听来电。
- `ui/activity/WebRtcCallActivity.java`
  - 通话页（本地/远端画面 + 静音 + 摄像头开关 + 挂断）。
- `webrtc/signaling/WebRtcSignalingClient.java`
  - Android WebSocket 信令客户端。
- `webrtc/signaling/WebRtcSignalTypes.java`
  - 信令消息类型常量。
- `res/layout/activity_webrtc_demo.xml`
- `res/layout/activity_webrtc_call.xml`

#### `flutteraar/app` 修改

- `MainActivity.java`
  - 增加 `WebRTC Demo` 入口。
- `AndroidManifest.xml`
  - 注册 `WebRtcDemoActivity`、`WebRtcCallActivity`。
- `app/build.gradle`
  - 增加 `okhttp` 与 `webrtc` 依赖。
- `gradle/libs.versions.toml`
  - 增加 `okhttp`、`webrtc` 版本与库声明。

#### `springboot/demo` 新增

- `config/WebRtcSignalingConfig.java`
  - 注册 WebSocket 路由 `/ws/webrtc`。
- `webrtc/WebRtcSignalingHandler.java`
  - 信令会话管理、按 uid 点对点转发、离线错误回执。

#### `springboot/docker` 修改

- `docker-compose.yml`
  - 新增 `coturn` 服务（`3478` + 中继端口段）。
- `nginx-docker/conf/nginx.conf`
  - 新增 `/ws/` 到 SpringBoot 的 WebSocket 透传。
- `docker/nginx/nginx.conf`
  - 兼容补充 WebSocket Upgrade 头。
- `docker/README.md`
  - 补充 WebRTC 信令 + TURN 启动与排障说明。

### 14.6 联调步骤（两台手机）

1. 启动后端：
   - `docker compose -f demo/springboot/docker/docker-compose.yml up -d springboot nginx coturn`
2. 两台手机安装同一版 `flutteraar app`。
3. A 进入 `WebRTC Demo`：
   - 绑定 `deviceA`
   - 对方 ID 填 `deviceB`
   - 点 `Call`
4. B 进入 `WebRTC Demo`：
   - 绑定 `deviceB`
   - 收到来电弹窗后点“接听”。
5. 双方进入通话页验证：
   - 上方是否显示本地画面；
   - 下方是否显示对方画面；
   - 验证静音、关摄像头、挂断按钮行为。

### 14.7 当前实现边界（如实说明）

- 当前 ID 绑定为“内存会话态”，服务重启后在线状态清空（Demo 设计）；
- TURN 使用静态账号（`webrtc/webrtc123`），适合内网或开发验证；
- 若公网部署，建议把 TURN 改为短时动态凭证（`use-auth-secret` + 服务端签发）；
- 当前优先实现 1v1 呼叫，不含群组房间与通话记录持久化。
