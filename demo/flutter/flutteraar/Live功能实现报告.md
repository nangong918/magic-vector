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
