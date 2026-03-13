**MainDesignDocument**
====

此文档是整个项目的主要设计文档，我只维护整个设计文档不维护任何代码，
你负责根据我的设计文档进行代码实现。

本文件仅作为总设计入口与模块索引，不承载各模块的详细实现记录。

## 项目功能

项目功能设计大纲，仅供参考；详情需要参考各个模块的`DesignDocument.md`

#### Start/Login/Register
* 闪屏
  * 跳转登录或者主页面
* 登录
* 注册

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
    * Agent指令控制台输出（含在线查询与离线缓存）
* 云操控平台(Live)
    * 向SpringBoot发送请求指令
    * 接收Nginx的Live推流
        * 另一台设备的Camera信道（Nginx）测试
        * 视频录制保存本地
    * App与App推拉流测试（推流/拉流模式切换）
* 离线蓝牙、Wifi操控
    * BLE蓝牙连接并发送指令
    * 连接RK创建的WIFI发送指令
    * Agent指令控制台输出
    * RK创建WIFI后向Android发送UDP摄像头帧并实时显示
    * RK端联调前，RK链路先按TODO占位

#### Mine
* Setting
    * 修改密码
    * 登出
* 视频
    * 云上录播记录播放
    * 本地视频播放
    * 本地视频上传云端
* 本次迭代范围
    * Setting（修改密码、登出）：实现基础流程
    * 云上录播记录播放：实现页面与接口骨架，FFmpeg转m3u8服务端方案先落设计
    * 本地视频播放：实现
    * 本地视频上传云端：实现断点续传接口骨架（MinIO），可运行链路优先
* （Test；仅测试分支有，此分支不是）


## Android 设计文档

[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)

## SpringBoot 设计文档

[SpringBootDesignDocument.md](springboot/docs/SpringBootDesignDocument.md)

## Agent 主流程约束（本次新增）

* Main 首页导航语义统一为：`Agent`（聊天 Agent 相关）、`Control`（设备状态操作监控）、`Mine`（我的）。
* `Agent` 页在无数据时显示中心大按钮创建 Agent；存在数据时显示 Agent 列表。
* 创建/查看/编辑/删除 Agent 统一采用 Main 页面内全屏 Compose 组合函数弹层，不再依赖独立 Activity 返回值。
* Agent 列表刷新与页面间事件同步优先使用 `StateFlow/SharedFlow`，`eventBus` 仅作为兜底方案，不作为主方案。
* 聊天消息数据源统一抽象为：`HTTP(首次/重连补偿)` + `WebSocket(实时)` + `Room(离线缓存)`，由 Android 与 SpringBoot 模块设计文档分别落地细节。