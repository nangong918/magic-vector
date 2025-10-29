


## 当前任务

### lv-1(待定)
Spring 项目部署在Docker -> 上传到阿里云服务器 todo Docker，Nginx -> 内网穿透(部署ES，Minlvus对服务器要求太高了)
简单的SpringAI智能体部署Docker + 内网穿透
压测, 修复bug, 提高系统的鲁棒性

### lv0(当前任务)
实现websocket进行单个图片视觉，后续升级为Http上图片组和视频流

调用阿里百炼视觉模型
视频流传输考虑使用FFmpeg进行压缩, 节省带宽和模型token

AI Emoji Activity

阿里百炼视觉理解模型

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

联网mcp

chat mcp(tools -> 本地测试)
chat rag(向量)

设计一个优先级list判断功能，eye会根据优先级来判断

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
部分View改用Jetpack compose
最后升级Flutter, 打通IOS用户
spring的线程池升级为虚拟线程
(*)spring ai -> golang + python + langchain

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