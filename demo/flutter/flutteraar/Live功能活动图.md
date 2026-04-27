# Live功能活动图

## 1. 实时推流活动图

```mermaid
flowchart TD
    A["打开 LivePushDemoActivity"] --> B["onCreate()"]
    B --> C["bindViews() 初始化输入框/按钮"]
    C --> D["initListeners() 绑定按钮事件"]
    D --> E["ensurePermissions() 检查 CAMERA / RECORD_AUDIO"]
    D --> D1["initOrientationListener()"]

    E -->|已授权| F["initCameraPreview()"]
    E -->|未授权| E1["permissionLauncher.launch(...)"]
    E1 --> E2["onPermissionResult()"]
    E2 -->|授权成功| F
    E2 -->|授权失败| E3["updateStatus() + Toast"]

    F --> G["camera2Helper = new Camera2Helper.Builder()...build()"]
    G --> H["Camera2Helper.start()"]
    H --> I["Camera2Helper.openCamera()"]
    I --> J["Camera2Helper.createCameraPreviewSession()"]
    J --> K["onCameraOpened(Size previewSize, int displayOrientation)"]
    K --> L["LivePushDemoActivity.previewSize / previewDegree 赋值"]
    L --> M["updateButtons() 允许开始推流"]

    M --> N["点击 btnTogglePush"]
    N --> O["startLivePush()"]
    O --> P{"previewSize != null 且 liveUrl 非空?"}
    P -->|否| P1["updateStatus() 或 editLiveUrl.setError()"]
    P -->|是| Q["new LivePushConfig(...)"]
    Q --> R["new LivePusherBridge(config, this)"]

    R --> R1["LivePusherBridge.<init>()"]
    R1 --> R2["native_init()"]
    R2 --> R3["native_setVideoCodecInfo(...)"]
    R3 --> R4["native_setAudioCodecInfo(...)"]

    R4 --> S["livePusherBridge.startPush(liveUrl)"]
    S --> S1["LivePusherBridge.startPush()"]
    S1 --> S2["native_start(path)"]
    S2 --> S3["RtmpPusher.cpp::native_start"]
    S3 --> S4["start(void* args) 连接 RTMP"]
    S4 --> S5["RTMP_Connect()"]
    S5 --> S6["RTMP_ConnectStream()"]
    S6 --> S7["isPushing = true + packets.setRunning(true)"]
    S7 --> S8["callback(audioStream->getAudioTag()) 先发 AAC 头"]

    S --> T["audioCaptureTask = new AudioCaptureTask(livePusherBridge)"]
    T --> U["AudioCaptureTask.start()"]
    U --> V["AudioCaptureTask.run()"]
    V --> V1["audioRecord.startRecording()"]
    V1 --> V2["audioRecord.read(...)"]
    V2 --> V3["bridge.pushAudioFrame(...)"]
    V3 --> V4["LivePusherBridge.pushAudioFrame()"]
    V4 --> V5["native_pushAudio(data)"]
    V5 --> V6["RtmpPusher.cpp::native_pushAudio"]
    V6 --> V7["AudioStream::encodeData()"]
    V7 --> V8["callback(packet)"]
    V8 --> V9["packets.push(packet)"]

    J --> W["Camera2Helper.OnImageAvailableListenerImpl.onImageAvailable()"]
    W --> W1["YuvUtil.YUV420pRotate90/180() 按 rotateDegree 旋转"]
    W1 --> W2["camera2Listener.onPreviewFrame(dstData/yuvData)"]
    W2 --> X["LivePushDemoActivity.onPreviewFrame(byte[] yuvData)"]
    X --> X1["livePusherBridge.pushVideoFrame(yuvData, LiveFrameFormat.I420)"]
    X1 --> X2["LivePusherBridge.pushVideoFrame()"]
    X2 --> X3["native_pushVideo(data, frameFormat)"]
    X3 --> X4["RtmpPusher.cpp::native_pushVideo"]
    X4 --> X5["VideoStream::encodeVideo()"]
    X5 --> X6["sendSpsPps() / sendFrame()"]
    X6 --> X7["callback(packet)"]
    X7 --> X8["packets.push(packet)"]

    D1 --> ORI["OrientationEventListener.onOrientationChanged()"]
    ORI --> ORI1["camera2Helper.updatePreviewDegree(newPreviewDegree)"]
    ORI1 --> ORI2{"正在推流且 previewSize != null?"}
    ORI2 -->|是| ORI3["livePusherBridge.updateVideoCodecInfo(width, height)"]
    ORI2 -->|否| ORI4["仅更新 previewDegree 字段"]

    V9 --> Y["start(void* args) 发送线程循环"]
    X8 --> Y
    Y --> Y1["packets.pop(packet)"]
    Y1 --> Y2["RTMP_SendPacket(rtmp, packet, 1)"]
    Y2 --> Y3{发送成功?}
    Y3 -->|是| Y1
    Y3 -->|否| Z["throwErrToJava(ERROR_RTMP_SEND_PACKET)"]
    Z --> Z1["LivePusherBridge.errorFromNative(int errCode)"]
    Z1 --> Z2["listener.onError(errCode, message)"]
    Z2 --> Z3["LivePushDemoActivity.onError()"]
    Z3 --> Z4["stopLivePush()"]

    N -->|再次点击| AA["stopLivePush()"]
    AA --> AB["AudioCaptureTask.stop()"]
    AB --> AC["livePusherBridge.stopPush()"]
    AC --> AD["native_stop()"]
    AD --> AE["RtmpPusher.cpp::native_stop -> isPushing = false"]
    AE --> AF["livePusherBridge.release()"]
    AF --> AG["native_release()"]
```

```mermaid
flowchart TD
    A["进入直播页"] --> B["检查并申请相机/麦克风权限"]
    B --> C{"权限是否通过?"}
    C -->|否| D["提示用户授权并停止后续流程"]
    C -->|是| E["启动相机预览"]
    E --> F["用户输入 RTMP 地址"]
    F --> G["点击开始实时推流"]
    G --> H["初始化推流引擎（音视频编码器 + RTMP 连接）"]
    H --> I["开始采集视频帧"]
    H --> J["开始采集音频帧"]
    I --> K["编码后发送到直播服务器"]
    J --> K
    K --> L{"推流是否正常?"}
    L -->|是| M["持续推流并更新状态"]
    L -->|否| N["提示错误并自动停止推流"]
    M --> O{"用户是否停止/离开页面?"}
    O -->|否| M
    O -->|是| P["停止采集、断开连接、释放资源"]
```

## 2. FFmpeg 文件推流活动图

```mermaid
flowchart TD
    A["打开 LivePushDemoActivity"] --> B["输入 editInputPath 和 editFilePushUrl"]
    B --> C["点击 btnPushFile"]
    C --> D["startFilePush()"]
    D --> E{"inputPath / liveUrl 非空?"}
    E -->|否| E1["setError()"]
    E -->|是| F["FFmpegPushBridge.pushStreamAsync(inputPath, liveUrl, callback)"]
    F --> G["EXECUTOR.execute(...)"]
    G --> H["FFmpegPushBridge.pushStream()"]
    H --> I["nativePushStream(inputPath, liveUrl)"]
    I --> J["ffmpeg_pusher_jni.cpp::Java_com_demo_aarlib_live_ffmpeg_FFmpegPushBridge_nativePushStream"]
    J --> K["new FFRtmpPusher()"]
    K --> L["FFRtmpPusher.open(input, output)"]
    L --> M["avformat_open_input() 读取媒体源"]
    M --> N["avformat_find_stream_info()"]
    N --> O["avformat_alloc_output_context2(..., 'flv', liveUrl)"]
    O --> P["avformat_new_stream() 复制音视频流参数"]
    P --> Q["avio_open2()"]
    Q --> R["avformat_write_header()"]
    R --> S["FFRtmpPusher.push()"]
    S --> T["av_read_frame()"]
    T --> U{是否音视频包?}
    U -->|否| T
    U -->|是| V["rescale(...)"]
    V --> W["av_interleaved_write_frame()"]
    W --> X{发送成功?}
    X -->|是| T
    X -->|否| Y["return ret"]
    Y --> Z["FFRtmpPusher.close()"]
    Z --> AA["callback.onCompleted(resultCode, message)"]
    AA --> AB["LivePushDemoActivity.updateStatus(message)"]
```


```mermaid
flowchart TD
    A["进入直播页"] --> B["填写媒体输入源和 RTMP 地址"]
    B --> C["点击开始 FFmpeg 推流"]
    C --> D{"输入参数是否完整?"}
    D -->|否| E["提示用户补全参数"]
    D -->|是| F["后台启动文件推流任务"]
    F --> G["读取输入媒体（本地文件或网络流）"]
    G --> H["建立到 RTMP 服务端的输出通道"]
    H --> I["按时间顺序发送音视频包"]
    I --> J{"发送是否成功?"}
    J -->|是| K["持续发送直到媒体结束"]
    J -->|否| L["记录失败并回调结果"]
    K --> M{"媒体是否结束?"}
    M -->|否| I
    M -->|是| N["结束推流并释放资源"]
    N --> O["页面显示完成状态"]
    L --> O
```

## 3. 关键字段速览

- `LivePushDemoActivity.pushing`
  - 当前实时推流状态位，控制按钮、音频采集循环、视频帧是否继续送入 SDK。
- `LivePushDemoActivity.previewSize`
  - 由 `onCameraOpened()` 回填，是 `startLivePush()` 生成 `LivePushConfig` 的基础。
- `LivePushDemoActivity.previewDegree`
  - 由 `Camera2` 打开时传回，用于决定宽高是否交换。
- `LivePusherBridge.started`
  - SDK 内部是否已启动 native 推流。
- `LivePusherBridge.mute`
  - 控制 `pushAudioFrame()` 是否跳过 PCM 上送。
- `RtmpPusher.cpp::isPushing`
  - native RTMP 线程是否继续发送队列包。
- `RtmpPusher.cpp::packets`
  - 音视频编码后的 `RTMPPacket` 队列。


## 4. Live 拉流播放活动图

```mermaid
flowchart TD
    A["打开 LivePullDemoActivity"] --> B["bindViews() 初始化 PlayerView / URL 输入框 / 按钮"]
    B --> C["默认 URL: rtmp://IP:1935/stream/live"]
    C --> D["点击开始拉流播放"]
    D --> E{"URL 是否为空?"}
    E -->|是| E1["editPullUrl.setError()"]
    E -->|否| F["ensurePlayer() 初始化 Media3 ExoPlayer"]
    F --> G["DefaultDataSource.Factory + DefaultMediaSourceFactory"]
    G --> H["player.setMediaItem(MediaItem.fromUri(url))"]
    H --> I["player.prepare() + player.play()"]
    I --> J["Player.Listener.onPlaybackStateChanged()"]
    J --> J1{"STATE?"}
    J1 -->|BUFFERING| K["状态: 缓冲中"]
    J1 -->|READY| L["状态: 播放中(显示 live offset)"]
    J1 -->|ENDED| M["状态: 流结束 + pullPlaying=false"]
    J --> N["onPlayerError() -> 状态: 播放失败"]
    N --> O["用户点击停止播放 / onDestroy()"]
    M --> O
    O --> P["player.stop() + clearMediaItems() + release()"]
```

```mermaid
flowchart TD
    A["输入 RTMP 地址"] --> B["点击 RTMP 地址一键转 HLS 预览地址"]
    B --> C{"是否 rtmp:// 开头且格式合法?"}
    C -->|否| D["Toast 提示格式不合法"]
    C -->|是| E["提取 host 与 streamName"]
    E --> F["拼接 http://host:8080/hls/streamName/index.m3u8"]
    F --> G["回填输入框并可直接开始播放"]
```

