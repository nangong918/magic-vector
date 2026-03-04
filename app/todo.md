**Todo**
====


AndroidX 升级 Jetpack Compose + Flutter

**AI的出现可以敏捷开发了，不要自己手写代码，学习理论是为了审核的更对更好。**

### 任务跟进

| 功能              | compose | flutter |
|-----------------|---------|---------|
| Start页面         | 完成      | 完成      |
| Main页面          | 待检查     | 待检查     |
| MessageListPage | 待检查     |         |
| MinePage        | 完成      |         |
| CreateAgent     | 待检查     |         |
| Chat页面          | 待检查     |         |
| Call页面          | 待检查     |         |
| VL页面            | 待检查     |         |
| AgentInfo页面     | 待检查     |         |
| Media页面         | 待开发     |         |


Control

检测到的RK列表
Get请求获取到的Agent列表


选择使用RK
RK设备状态：





### 项目功能


#### Agent
Agent
* 创建Agent
* 查看，修改，删除Agent
AgentList
* 选择Agent
* 接收Agent消息

#### Chat
* 视图：
  * Call唤醒视图
  * 文本Chat视图
  * emoji表情视图
* 当前的前置摄像头状况

#### Control
* 设备状态操作监控
  * RK与App连接状态（蓝牙，Wifi）
  * RK与SpringBoot连接状态
  * RK的Agent选用状态
  * App与SpringBoot连接状态
* 云操控平台(Live)
  * 向SpringBoot发送请求指令
  * 接收Nginx的Live推流
    * 另一台设备的Camera信道（Nginx）测试
    * 视频录制保存本地
* 离线蓝牙、Wifi操控
  * BLE蓝牙连接并发送指令
  * 连接RK创建的WIFI发送指令
  * Agent指令控制台输出

#### Mine
* Setting
* 视频
  * 云上录播记录播放
  * 本地视频播放
  * 本地视频上传云端
* （Test）


### 项目设计

* 数据缓存、持久化：
  * MMKV
  * SQLite

* 长连接：
  * WebSocket
  * MQTT

* 消息推送：
  * Firebase

* 音视频流媒体：
  * FFmpeg、x264
  * RTMP


### 详细设计

[Android详细设计.md](android/详细设计.md)



### todo任务

* 测试：将AndroidX替换为Compose的Activity然后进行运行，测试基本功能。

* 学习：学习Jetpack Compose的UI设计 + Android特性 -> 审核之前的UI设计是否合理
- 代码审核 + 重构UI

* 学习：学习MVI设计模式 -> 审核之前的MVI设计模式是否合理
- 代码审核 + 重构MVI框架

* 学习：学习UML的类，时序等设计 -> 审核之前的业务设计是否合理
- 代码审核 + 重构业务

* 解决过程中遇到的SpringBoot问题，设计UML，快速开发代码并测试。

* 完成Flutter化

* 学习：学习Flutter 的UI设计 + Android特性 -> 审核之前的UI设计是否合理
- 代码审核 + 重构UI

* 代码审核 + 重构MVI框架

* 代码审核 + 重构业务

- 完成Android的音视频流媒体开发
- 完成SpringBoot的音视频流媒体开发
- 完成Flutter的音视频流媒体开发


























