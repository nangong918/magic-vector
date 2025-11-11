


## 当前任务

### lv-1(待定)
Spring 项目部署在Docker -> 上传到阿里云服务器 todo Docker，Nginx -> 内网穿透(部署ES，Minlvus对服务器要求太高了)
简单的SpringAI智能体部署Docker + 内网穿透
压测, 修复bug, 提高系统的鲁棒性

先暂时不开嵌入式模块, AI模块制作完成就好好学习计算机理论 + SpringBoot技术 + Android技术(包括Jetpack Compose) -> 优化代码, 提高系统的鲁棒性

Android端优化, 暂时不开启树莓派 -> 研究树莓派

## lv0.5(杂七杂八小任务)
AI Emoji Activity （表情系统Mcp，心情系统）

集成minio, 反向代理

联网MCP

非结构化chat任务 RAG(向量)
结构化任务Mysql，Mongodb，Elastic Search，Neo4j

设计一个优先级list判断功能，eye会根据优先级来判断

Android User管理登录页面

### lv0(当前任务/核心任务)

#### 本月任务
1. 完成状态机
2. 完成vision线程阻塞与事件激活
3. 完成表情, motion的mcp
4. 完成重构升级


今日任务：
1.Android在connect的时候需要传递MCP的设置，暂时在MainApplication中写死。
2.Spring 和 Android 都改为接收MixLLM输入，并解析。
3.Android上传图片到后端的阻塞测试。


各种模式的测试demo
重构升级: 面向对象, 设计模式, UML, 框架(Android, Spring设计模式)

修复系统bug: 不能把系统可靠性依赖于AI, 要默认AI是完全不可信的
学习: 面向对象, 设计模式, UML -> 将下述的设计图绘制出来, 并尝试实现.

看cocoAI视频, 列出其核心功能和模式, 列出设计思路, 绘制设计流程图, 初步进行设计. 功能模式进行分级


学习计算机基础 -> 按照优化点逐个优化
部署docker, nginx, 内网穿透

实现websocket进行单个图片视觉，后续升级为Http上图片组和视频流

完成视觉之后 -> 研究计算机理论，解决系统不合理以及性能优化

研究降低token的方法

调用阿里百炼视觉模型
视频流传输考虑使用FFmpeg进行压缩, 节省带宽和模型token

更改嵌入式设备, 需要升级为树莓派.(树莓派ARM架构才能带动tflite)

最优先推进：

1. 开发板对比；资料学习；给出对比列表以及分析可执行方案：
  VAD部署可行性
  Github开源代码量
   淘宝上产品 -> 可行性分析
   方案：ESP32-S3? 树莓派4B? Arduino? RK3566?

   学习Esp32-S3 + ESP-IDF开发实验
   联网部署ESP32-S3
   构建头部VAD，表情等

2. 如果无法推进则
   Android Emoji + video人脸识别，动态眼睛，表情
   Android设备部署：视觉活动检测YOLOv8（目标检测）、OpenCV（帧活动检测）；视频活动检测：类似VAD音频活动检测


体验视觉模型，思考能做的事情


研究esp32-sc; 构建身体（头部和底部两部分）

Silero VAD集成ESP32-S3

主动调取视觉接口：1.寻找物品，2.寻找人，3.玩玩具

### lv1(计划任务)
增加一个HttpClient解决http请求资源复用问题
音频合成缓存，解决当前音频请求频率过高问题

设计一个eye MCP，实现：让AI看哪里，它就持续看哪里

图片视觉

视觉感知，人脸识别，说话看向人

### lv2(研究性任务)
(*)YOLOv8实现了目标检测，但是没有视频活动检测；目前是对YOLOv8的输出直接用kotlin代码判断是否有活动检测，可以考虑使用tensorflow训练tflite模型交给Android调用（规则集并不完善，并且存在考虑不到的维度问题，交给模型处理效果更好）

chat 声纹识别（避免vad将自己的声音误认为输入）
研究FFmpeg并集成在Android
人格系统：人格 = 记忆 + 算法（意图产生，态度倾向）
视频视觉 / WebRTC
VAD 实时打断 + 3D-Speaker声纹识别

声源定位: 因为有了视觉之后不能乱找person在哪里, 容易找错人. 需要根据声音进行声源定位

性能分析：了解新能分析工具，分别对后端、前端、嵌入式进行性能分析

### lv3(无关紧要)
部分View改用Jetpack compose: Chat页面和Emoji页面切换用compose相比xml更好
最后升级Flutter, 打通IOS用户
spring的线程池升级为虚拟线程
(*)spring ai -> golang + python + langchain
(*)docker容器化

Android AgentList Fragment, Agent详情Activity，抽离Emoji和chat的逻辑，实现打开Emoji页面不需要先打开Chat页面，研究Java设计模式。
Android本地缓存功能
性能：Android性能监听，监听ANR

研究计算机网络：考虑websocket，mqtt性能瓶颈，学习不同网络协议

Spring，Android多级缓存，解决频繁请求

多线程重新设计：Spring：websocket的调用线程创建；Android：Camera和Audio获取的线程创建；Android的kotlin协程异步

数据库性能分析与设计：Spring存储聊天记录

数据结构与算法设计：Android端聊天记录多数据源插入排序方法，（嵌入式）Cpp的STL最快数据结构分析与设计

内存，动态运行性能，内存泄漏分析：Android创建与开销，性能分析，内存泄漏检查。Spring内存分析，JSON等数据结构化反射性能开销。图片压缩，流传递。

### lv4 学习型任务

研究计算机网络
研究Java设计模式
多线程重新学习
多级缓存设计
数据库性能分析与设计
数据结构与算法设计
内存，动态运行性能，内存泄漏分析
(*)计算机组成原理
语言复习：Java，Cpp
框架复习：Android，树莓派，Spring（非微服务，分布式，那部分理解就好）

## 已完成

App 加密混淆 (2025/9/24)
Android MessageList + ChatActivity迁移 (2025/9/27)
Spring 使用webflux + sse 传输流式数据, Android 用okhttp-sse 接收流式数据 (2025/9/28)
Spring + Android 实现了SSE 的TTS问答语音合成 (2025/10/3)
Websocket测试 (2025/10/7)
添加VoiceWaveView语音波形图组件 (2025/10/7)
实现 OmniRealTimeNoVADTestChannel 音频处理功能 (2025/10/8)
完成前后端时音频聊天 (2025/10/11)
Omni实时多模态音频聊天 + 音量检测 (2025/10/12)
STT fun-asr-realtime 测试 (2025/10/13)
stt -> llm -> nlp -> tts 工作流完成 (2025/10/13)
chat memory(内存) + chat history(历史：mysql) (2025/10/17)
chat app agent创建 + 聊天 (2025/10/19)

**和 杨骞卉 ”分手“ (2025/10/20)**

完成文本聊天 + 前端流式展示 (2025/10/21)
Silero,WebRTC_Vad 集成 Android (2025/10/22) 
实时语音通话 (2025/10/23)
修复tts频繁调用 + ChatClient Connect Reset bug (2025/10/25)
YOLOv8集成Android(2025/10/26)
YOLOv8的Emoji追踪目标 (2025/10/27)
摄像头反转 (2025/10/28)
YOLOv8实现视频流目标活动检测 -> 用于未来调用视频流模型 (2025/10/28)
集成视觉模型 (2025/10/29)
剥离callManager, 实现Emoji + Chat页面的一同实时语音通话 (2025/10/30)
引入阿里百炼视觉理解模型 (2025/10/31)
集成阿里百炼视觉 + FunctionCall视觉回复 (2025/11/5)

## 其他
* 关于跳槽和工作方向选择：看看头部公司的技术方向，仔细看看是走**Android嵌入式**还是走**后端** （视觉模型完成之后就边看边选择，大概开始时间11月中旬）