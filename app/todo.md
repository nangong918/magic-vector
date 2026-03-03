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



### 项目功能

* Agent
  * 创建Agent
  * 查看，修改，删除Agent

* AgentList
  * 选择Agent
  * 接收Agent消息

* Chat
  * Agent 部署RK to Chat
  * App 通过文本Chat
  * App 通过唤醒CallChat
    * VL Chat

* Option
  * 云操控平台(Live)
  * 离线蓝牙、Wifi操控
  * 云上录播记录播放
  * 另一台设备Camera直播

* Mine
  * Setting
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

* Start页面
  * 从SQLite中获取User数据检查是否已经登录，已经登录跳转`Main页面`，没有登录跳转`Login页面`

* Login/Register页面
  * `Login页面`账号密码登录，登录成功获取`userId`，`accessToken`存储在SQLite
  * 没有账号密跳转`Register页面`

* Agent
  * 创建Agent
  * 查看，修改，删除Agent


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


























