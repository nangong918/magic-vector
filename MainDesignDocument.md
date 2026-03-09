**MainDesignDocument**
====

此文档是整个项目的主要设计文档，我只维护整个设计文档不维护任何代码，
你负责根据我的设计文档进行代码实现。

本文件仅作为总设计入口与模块索引，不承载各模块的详细实现记录。

## 项目功能

项目功能设计大纲，仅供参考；详情需要参考各个模块的`DesignDocument.md`

#### Login/Register
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
    * 修改密码
    * 登出
* 视频
    * 云上录播记录播放
    * 本地视频播放
    * 本地视频上传云端
* （Test；仅测试分支有，此分支不是）


## Android 设计文档

[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)

## SpringBoot 设计文档

[SpringBootDesignDocument.md](springboot/docs/SpringBootDesignDocument.md)