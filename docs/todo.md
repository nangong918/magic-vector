


## 当前任务

### lv-1(待定)
Spring 项目部署在Docker -> 上传到阿里云服务器 todo Docker，Nginx -> 内网穿透(部署ES，Minlvus对服务器要求太高了)
简单的SpringAI智能体部署Docker + 内网穿透

### lv0(当前任务)
chat memory(内存)
chat history(历史：mysql)

chat mcp(tools -> 本地测试)

chat rag(向量)

chat app agent创建
chat app agent聊天

研究esp32-sc; 构建身体（头部和底部两部分）

### lv1(计划任务)
联网mcp
chat vad
chat 声纹识别（避免vad将自己的声音误认为输入）

音频合成缓存，解决当前音频请求频率过高问题

图片视觉

### lv2(研究性任务)
研究FFmpeg并集成在Android
人格系统：人格 = 记忆 + 算法（意图产生，态度倾向）
视频视觉 / WebRTC
VAD 实时打断 + 3D-Speaker声纹识别

### lv3(无关紧要)
部分View改用Jetpack compose
最后升级Flutter, 打通IOS用户
spring的线程池升级为虚拟线程


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