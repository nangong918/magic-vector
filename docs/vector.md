vector剩余事项：

RK3588开发

Android流上传流程优化：
* 目前：cameraX -> bitmap -> SurfaceView -> yolo -> Udp -> Spring
* 方案1：cameraX -> bitmap -> OpenGL -> yolo -> FFmpeg -> RTMP/RTSP -> Spring
* 方案2：cameraX -> FFmpeg -> OpenGL -> yolo -> RTMP/RTSP -> Spring

IoT操控：wifi，mqtt，BLE

Agent（最后）


### 当前任务

* ESP32 + RK3588芯片开发
* RK3588设备操控App（BLE）
* Spring



### 产品设计

Iot应用各层

1. 应用交互层（人机交互）
    * Flutter App（适配鸿蒙 / Android/IOS 平板）
        * 向云端Spring Boot发送云端数据请求。
        * 向RK端Android Server（上位机）发送Http请求
        * 向RK 端应用层（下位机）通过BLE低功耗蓝牙发送指令
        * 通过WIFI传输
            * 文件传输：TCP可靠传递文件
            * 实时流接收：接收UDP实时流媒体数据
2. 服务层（指令分发，云存储，Agent服务，媒体流，异常日志）
    * 边缘计算层：RK内部的Android应用，通过Http、AIDL与RK的Android镜像内部的Android应用层进行交互。
        * 提供给Flutter本地的数据服务
        * 提供给Flutter的上未接蓝牙指令服务
    * Spring Boot云服务：部署在阿里云上的云端计算服务
        * 提供不同User的数据存储服务
        * 提供AI Agent服务：STT，TTS，LLM，VL
        * 提供RTMP终端推拉流服务：RK推送数据到云端，云端数据下发到Flutter供监控
        * 提供设备异常上云服务
3. 硬件执行层（数据处理 + 物理输出）
    * RK 端应用层（下位机）
        * UI展示：通过`OpenGL`将需要渲染的帧数据展示在RK设备屏幕上
        * 采集：
            * 通过`MediaRecord`获取Bitmap并交给YOLOv8目标活动检测
            * 通过`AudioRecord`获取音频数据并交给交给VAD
        *
        推流：通过MediaRecord获取Bitmap并通过FFmpeg封装为I、P、B帧的流媒体，通过RTMP直接交给云端SpringBoot服务（因为YOLOv8的入参是jpg/png的bytes所以不适用MediaRecord直接录制视频，直接复用bitmap）
        * GPIO操纵：
            * 在连接云端服务时，AI Agent可以通过WebSocket直接连接RK设备并操控GPIO引脚舵机等设备（无需中间层flutter）
            * 在离线状态下，flutter直接与Android Server连接，使用BLE蓝牙发送指令并简单操控舵机。

传输采取的方案：

还需要实现的步骤：

硬件执行层

* 运行RK设备
* 将Apk打包封装为Android镜像并烧录到RK设备
* RK设备操控GPIO引脚

应用层

* 创建Flutter项目并实现Android，IOS，鸿蒙三端稳定打包以及更新。（开头，先开发Android，毕竟Flutter适配三端较难）
* 在线：
    * Android原生/Flutter RTMP推拉流
    * 接收云端报警
* 离线：
    * Android原生/Flutter `BLE蓝牙`操控RK
    * Android原生/Flutter UDP接收RK应用的裸流数据
    * Android原生/Flutter 向RK直接传输WIFI TCP传输文件

边缘服务层

* 搭建Android Server；RK设备上的Android Server和Android Client直接使用AIDL或者Http进行通讯
* 异常上云
* SpringBoot部分页面数据能力迁移到Android Server

云服务层

* `RTMP/RTSP`推拉流
* 异常上云报警推送
* `Agent Memory`记忆以及 `向量数据库`记忆工程，`知识图谱`知识工程，提示词工程



### 可行性任务

[硬件参考](https://ottodiy.tech/docs/bom)

#### RK
* RK3588烧入App
* 编写JNI控制RK的GPIO引脚
* 拼接S90舵机在GPIO引脚，Android操控
* 拼接LCD屏幕在GPIO引脚，Android投送显示
* 拼接ESP32CAM摄像头在GPIO引脚，Android获取数据
* 拼接录音器在GPIO引脚，Android获取数据
* 拼接发声器在GPIO引脚，Android控制发声
* 拼接SDIO/UART IoT 控制，Android WIFI，BLE控制
* 给RK设备拼接电源



#### Android(RK)
* 语音唤醒
* WebRTC等VAD音频活动检测
* RK Camera数据流交给YOLOv8目标检测
* CAM流交给FFmpeg封装为I、P、B帧的流媒体h.264
* RTSP、RTMP推流
* BLE接收FlutterApp的蓝牙指令
* WIFI接收SpringBoot的WebSocket AI响应
* 业务
  * 表情模块
  * 情感模块
  * 自主行为模块



#### SpringBoot
* 提供AI Agent服务：STT，TTS，LLM，VL
* 提供不同User的数据存储服务
* RTSP、RTMP推拉流
* `Agent Memory`记忆以及 `向量数据库`记忆工程，`知识图谱`知识工程，提示词工程



#### Flutter
* 跨平台：跨Android，IOS，鸿蒙
* Agent预览：设定，聊天记录
* RTSP、RTMP拉流
* 渲染：OpenGL（Android）、普通渲染（IOS，鸿蒙）
* BLE蓝牙下发指令
* WIFI离线接收RK数据


