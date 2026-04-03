

## 现有问题
### Android端现有流程
```mermaid
flowchart TD
    A[MainActivity启动] --> B[绑定ChatService]
    B --> C[MainVm接收ChatServiceBound意图]
    C --> D[RealtimeChatController.ensureUserConnection]
    D --> E[RealtimeChatWebSocketManager.initRealtimeChatWsClient]
    E --> F[startRealtimeWs建立连接]
    F --> G[WsManager.sendConnectInfo发送用户连接信息]
    H[用户点击具体Agent] --> I[RealtimeChatWebSocketManager.bindChannel]
    I --> J[WsManager.sendBindChannelInfo绑定Agent频道]
    K[收到WS消息] --> L[messageHandler.handleTextMessage]
    L --> M[WsManager.handleTextMessage解析]
    M --> N[更新ChatEventMapManager]

```

### SpringBoot端WS架构分析
```mermaid
flowchart TD
    A[客户端] --> B[建立连接到/agent/realtime/chat]
    A --> C[建立连接到/control/ws]
    B --> D[WsChatHandler]
    C --> E[ControlWsHandler]
    D --> F[PersistentConnectionManager]
    E --> G[ControlConsoleService]
    F --> H[处理聊天消息]
    G --> I[处理控制消息]
    J[服务器] --> K[维护多个独立会话]
    K --> L[无法统一用户认证]
    K --> M[无法统一在线状态]
```


## 优化后
### 优化后的Android WS架构设计

```mermaid
flowchart TD
    subgraph Android端
        A[WebSocketService<br/>创建CoroutineScope] --> B[UnifiedWebSocketManager]

        B --> C[连接管理<br/>Dispatchers.IO]
        C --> D[发送Channel<br/>容量32<br/>Channel<Message>]
        C --> E[重连机制<br/>含ping/pong心跳]
        C --> F[接收Channel<br/>容量32<br/>Channel<Message>]

        B --> G[消息分发器<br/>Dispatchers.Default<br/>从接收Channel消费消息]

        G --> H{接收Channel类型}

        H -->|agent| I1[AgentEventHandler<br/>Dispatchers.Default<br/>处理agent_list_sync]
        H -->|chat| I2[ChatEventHandler<br/>Dispatchers.Default<br/>处理chat_message_sync]
        H -->|stt| I3[STTEventHandler<br/>Dispatchers.Default<br/>处理stt_start_ack<br/>stt_text_data<br/>stt_error]
        H -->|llm| I4[LLMEventHandler<br/>Dispatchers.Default<br/>处理llm_start/llm_data<br/>llm_end/llm_error]
        H -->|tts| I5[TTSEventHandler<br/>Dispatchers.Main<br/>处理tts_data<br/>调用AudioPlayer播放]
        H -->|vl⚠️废弃| I6[VLEventHandler<br/>Dispatchers.Default<br/>处理vl_start/vl_data<br/>&#10060废弃通道]
        H -->|control| I7[ControlEventHandler<br/>Dispatchers.Default<br/>处理control_response]
        H -->|status| I8[StatusEventHandler<br/>Dispatchers.Default<br/>处理rk_status]
        H -->|system| I9[SystemEventHandler<br/>Dispatchers.Default<br/>处理error/system_message]
        H -->|connection| I10[ConnectionEventHandler<br/>Dispatchers.Default<br/>处理connect_ack/ping/pong]

        I1 --> J[withContext Dispatchers.Main<br/>UI回调更新]
        I2 --> J
        I3 --> J
        I4 --> J
        I5 --> K[AudioPlayer<br/>AudioTrack播放音频流]
        I6 --> L[VideoPlayer<br/>&#10060废弃通道]
        I7 --> J
        I8 --> J
        I9 --> J
        I10 --> J

        M[用户操作] --> N{操作类型}

        N -->|创建/删除/编辑Agent| O1[launch Dispatchers.Default<br/>构造Message<br/>channel:agent<br/>event:agent_update]
        N -->|发送聊天消息| O2[launch Dispatchers.Default<br/>构造Message<br/>channel:chat<br/>event:chat_message_send]
        N -->|控制RK设备| O3[launch Dispatchers.Default<br/>构造Message<br/>channel:control<br/>event:control_command_an]
        N -->|发送语音消息| O4[launch Dispatchers.IO<br/>构造Message<br/>channel:stt<br/>event:stt_start]

        O1 -->|send| D
        O2 -->|send| D
        O3 -->|send| D
        O4 -->|send| D

        O4 --> P[语音数据流处理<br/>Dispatchers.IO]
        P --> Q[循环读取麦克风buffer<br/>分片发送stt_audio_data<br/>每片带seq序列号]
        Q -->|send| D

        P --> R[发送stt_end结束识别]
        R -->|send| D

        S[视频流发送<br/>V2版本] --> T{传输方式选择<br/>各自独立协程}
        T -->|高速模式| U[launch Dispatchers.IO<br/>UDP推流到SpringBoot]
        T -->|防花屏模式| V[launch Dispatchers.IO<br/>RTMP推流到Nginx]

        U --> W[SpringBoot UDP服务]
        V --> X[Nginx-RTMP服务器]

        Y[UDP/RTMP播放<br/>V2版本] --> Z[launch Dispatchers.IO<br/>接收视频流]
        Z --> AA[launch Dispatchers.Default<br/>解码视频帧]
        AA --> AB[withContext Dispatchers.Main<br/>渲染到SurfaceView]

        D -->|接收循环<br/>Dispatchers.IO| B
        F -->|分发循环<br/>Dispatchers.Default| G

        AC[HTTP请求] --> AD[launch Dispatchers.IO<br/>GET /api/agents]
        AC --> AE[launch Dispatchers.IO<br/>GET /api/chat/history]

        AD --> AF[withContext Dispatchers.Main<br/>更新本地Agent缓存]
        AE --> AG[withContext Dispatchers.Main<br/>更新本地聊天缓存]
    end
```

### 优化后的SpringBoot WS架构

```mermaid
flowchart TD
    subgraph SpringBoot端
        A[UnifiedWsHandler<br/>WebSocket IO线程] --> B[WebSocket连接管理]

        B --> C[Android连接池<br/>userId -> session]
        B --> D[RK连接池<br/>deviceId -> session]
        B --> E[Android接收队列<br/>容量32<br/>BlockingQueue<Message>]
        B --> F[Android发送队列<br/>容量32<br/>BlockingQueue<Message>]
        B --> G[RK接收队列<br/>容量32<br/>BlockingQueue<Message>]
        B --> H[RK发送队列<br/>容量32<br/>BlockingQueue<Message>]

        A --> I[消息路由器<br/>快速解析channel]

        I --> J{Channel类型<br/>线程池隔离}

        J -->|connection| K1[ConnectionHandler<br/>业务线程池<br/>处理connect/connect_ack<br/>rk_connect/rk_connect_ack<br/>ping/pong]
        J -->|agent| K2[AgentHandler<br/>业务线程池<br/>处理agent_update<br/>存储agentId->userId<br/>返回agent_update_ack]
        J -->|chat| K3[ChatHandler<br/>业务线程池<br/>处理chat_message_send<br/>查agentId->userId<br/>构造chat_message_sync]
        J -->|control| K4[ControlHandler<br/>控制线程池<br/>处理control_command_an<br/>生成commandId转发RK<br/>处理command_result<br/>返回control_response]
        J -->|status| K5[StatusHandler<br/>业务线程池<br/>处理status_request转发RK<br/>处理rk_status转发Android]
        J -->|stt| K6[STTHandler<br/>STT线程池<br/>处理stt_start/stt_audio_data<br/>stt_end/stt_error<br/>调用ASR引擎返回stt_text_data]
        J -->|llm| K7[LLMHandler<br/>LLM线程池<br/>处理llm_start/llm_end<br/>流式生成llm_data<br/>异常返回llm_error]
        J -->|tts| K8[TTSHandler<br/>TTS线程池<br/>处理tts_start/tts_end<br/>合成tts_data音频流<br/>异常返回tts_error]
        J -->|vl<br/>⚠️废弃| K9[VLHandler<br/>VL线程池<br/>&#10060废弃通道<br/>处理vl_start/vl_data/vl_end<br/>向前兼容旧版本]
        J -->|system| K10[SystemHandler<br/>业务线程池<br/>处理error/system_message<br/>记录日志/系统通知]

        K1 --> L1[入队Android发送队列<br/>connect_ack/pong等]
        K2 --> L1
        K3 --> L1
        K4 --> L2[入队RK发送队列<br/>control_command_sb]
        K5 --> L2
        K4 --> L3[入队Android发送队列<br/>control_response]
        K5 --> L3
        K6 --> L3
        K7 --> L3
        K8 --> L3
        K9 --> L3
        K10 --> L3

        L1 --> F
        L2 --> H
        L3 --> F

        E -->|poll| I
        G -->|poll| I
        F -->|poll| A
        H -->|poll| A

        M[视频流接收<br/>V2版本] --> N{传输协议}
        N -->|UDP高速模式| O[UDPServer<br/>接收Android/RK推流]
        N -->|RTMP防花屏| P[RTMPServer<br/>Nginx-RTMP]

        O --> Q[视频流处理]
        P --> Q

        R[HTTP API] --> S1[GET /api/agents<br/>返回Agent列表]
        R --> S2[GET /api/chat/history<br/>返回聊天历史]
        R --> S3[POST /api/agent<br/>创建/更新/删除Agent]

        S1 --> T1[查询数据库]
        S2 --> T2[查询数据库]
        S3 --> T3[操作数据库<br/>广播agent_list_sync]

        T3 --> L1
    end
```


### RK框架

```mermaid
flowchart TD
    subgraph RK设备端
        A[RKControlService] --> B[RKWebSocketManager]

        B --> C[连接管理]
        C --> D[发送队列<br/>容量32<br/>BlockingQueue<Message>]
        C --> E[重连机制<br/>含ping/pong心跳]
        C --> F[接收队列<br/>容量32<br/>BlockingQueue<Message>]

        B --> G[消息分发器<br/>按Channel路由]

        G --> H{接收Channel类型}

        H -->|connection| I1[ConnectionEventHandler<br/>处理rk_connect_ack<br/>ping/pong]
        H -->|control| I2[CommandExecutor<br/>处理control_command_sb<br/>解析command和params]
        H -->|status| I3[StatusRequestHandler<br/>处理status_request]
        H -->|system| I4[SystemEventHandler<br/>处理error/system_message]

        I1 --> J[连接状态管理<br/>更新注册状态]
        
        I2 --> K{命令类型}
        K -->|GPIO控制| L1[GPIOController<br/>执行GPIO指令]
        K -->|LED控制| L2[LEDController<br/>执行LED指令]
        K -->|传感器读取| L3[SensorReader<br/>读取传感器数据]
        K -->|其他| L4[OtherController<br/>执行其他指令]
        
        L1 --> M[获取执行结果<br/>code/message]
        L2 --> M
        L3 --> M
        L4 --> M
        
        M --> N[构造Message<br/>channel:control<br/>event:command_result<br/>data: commandId,code,message]
        
        I3 --> O[采集设备状态]
        O --> P[构造Message<br/>channel:status<br/>event:rk_status<br/>data: deviceId,battery,position]
        
        I4 --> Q[系统消息处理<br/>记录日志/错误处理]

        N -->|offer| D
        P -->|offer| D

        R[硬件事件/定时任务] --> S{事件类型}
        S -->|定时上报<br/>每30秒| T[定时器触发]
        S -->|传感器中断| U[传感器数据变化]
        S -->|按钮按下| V[按钮事件]
        
        T --> O
        U --> O
        V --> O

        D -->|poll| B
        F -->|poll| G
        
        W[视频流发送<br/>V2版本] --> X{传输方式选择}
        X -->|高速模式| Y[UDP推流<br/>直接发送到SpringBoot<br/>低延迟]
        X -->|防花屏模式| Z[RTMP推流<br/>发送到Nginx-RTMP<br/>稳定可靠]
        
        Y --> AA[SpringBoot UDP服务]
        Z --> AB[Nginx-RTMP服务器]
        
        AC[RK APP离线UDP推送] --> AD[Camera录制视频流]
        AD --> AE["UDP直推<br/>RK → Android<br/>点对点传输"]
        AE --> AF[Android端接收<br/>本地播放]
    end
```

### 整体数据流
```mermaid
graph LR
    subgraph Android
        A1[用户操作]
        A2[发送队列<br/>容量32]
        A3[WebSocket客户端]
        A4[接收队列<br/>容量32]
        A5[消息分发器]
        A6[UI更新]
    end

    subgraph SpringBoot
        B1[WebSocket服务端]
        B2[Android接收队列<br/>容量32]
        B3[消息路由器]
        B4[Android发送队列<br/>容量32]
        B5[RK接收队列<br/>容量32]
        B6[RK发送队列<br/>容量32]
        B7[连接管理器]
        B8[Agent绑定服务]
        B9[控制转发服务]
    end

    subgraph RK设备
        C1[发送队列<br/>容量32]
        C2[WebSocket客户端]
        C3[接收队列<br/>容量32]
        C4[命令执行器]
        C5[状态采集器]
    end

    A1 -->|bind_agent| A2
    A1 -->|chat_message| A2
    A1 -->|control_command| A2

    A2 -->|出队| A3
    A3 -->|WebSocket| B1

    B1 -->|入队| B2
    B2 -->|出队| B3

    B3 -->|bind_agent| B8
    B3 -->|chat_message| B8
    B3 -->|control_command| B9

    B8 -->|转发聊天| B4
    B9 -->|转发控制| B6

    B4 -->|出队| B1
    B6 -->|出队| B1

    B1 -->|WebSocket| C2
    C2 -->|入队| C3
    C3 -->|出队| C4
    C3 -->|出队| C5

    C4 -->|命令结果| C1
    C5 -->|状态上报| C1

    C1 -->|出队| C2
    C2 -->|WebSocket| B1

    B1 -->|入队| B5
    B5 -->|出队| B3
    B3 -->|转发结果| B4
    B3 -->|广播状态| B4

    B4 -->|出队| B1
    B1 -->|WebSocket| A3

    A3 -->|入队| A4
    A4 -->|出队| A5
    A5 -->|chat_message| A6
    A5 -->|control_response| A6
    A5 -->|rk_status| A6
```


### 数据结构设计

#### Event

SpringBoot

```java
@Data
public class ClientEvent {
    private String channel;
    private Map<String, String> data;
}

@Data
public class ServerEvent {
    private String channel;
    private Map<String, String> data;
}
```

Android (App/RK)

```kotlin
data class ClientEvent(
    val channel: String,
    val data: Map<String, String> = emptyMap()
)
data class ServerEvent(
    val channel: String,
    val data: Map<String, String> = emptyMap()
)
```

#### Event 枚举设计

##### 1. 连接管理类（三端通用）

| Event | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `connect` | Android→SB | 建立用户连接 | `userId` |
| `connect_ack` | SB→Android | 连接确认 | `code`, `message` |
| `rk_connect` | RK→SB | RK设备注册 | `deviceId`, `firmware` |
| `rk_connect_ack` | SB→RK | RK注册确认 | `code`, `message` |
| `ping` | 双向 | 心跳请求 | - |
| `pong` | 双向 | 心跳响应 | - |

> ping, pong超时策略（如连续3次无响应则断开重连）

##### 2. Agent数据同步类（Android ↔ SB）

| Event | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `agent_list_sync` | SB→Android | Agent列表变更推送 | `AgentChatDto` 列表 |
| `agent_update` | Android→SB | 用户操作Agent（创建/更新/删除） | `AgentChatDto` |

##### 3. 聊天消息类（Android ↔ SB）

| Event | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `chat_message_sync` | SB→Android | 聊天消息推送 | `ChatMessageDto` |
| `chat_message_send` | Android→SB | 用户发送消息 | `ChatMessageDto` |

##### 4. 音频处理类（Android ↔ SB）

| Event | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `stt_start` | Android→SB | 开始语音识别（STT） | `agentId` |
| `stt_start_ack` | SB→Android | 语音识别确认 | `agentId` |
| `stt_audio_data` | Android→SB | 语音数据流 | `agentId`, `base64AudioStream`, `seq` |
| `stt_text_data` | SB→Android | 识别的文本结果流 | `agentId`, `textStream`, `seq` |
| `stt_end` | Android→SB | 结束语音识别 | `agentId` |
| `stt_error` | Android→SB | 语音识别异常 | `agentId`, `code`, `message` |
| `llm_start` | SB→Android | 开始LLM处理 | `agentId` |
| `llm_data` | SB→Android | LLM文本流 | `agentId`, `textStream`, `seq` |
| `llm_end` | SB→Android | LLM处理结束 | `agentId` |
| `llm_error` | SB→Android | LLM处理异常 | `agentId`, `code`, `message` |
| `tts_start` | SB→Android | 开始语音合成（TTS） | `agentId` |
| `tts_data` | SB→Android | TTS音频流 | `agentId`, `base64AudioStream`, `seq` |
| `tts_end` | SB→Android | TTS合成结束 | `agentId` |
| `tts_error` | SB→Android | TTS合成异常 | `agentId`, `code`, `message` |
| `vl_start` | Android→SB | 开始视觉理解（VL） | `agentId`（v2版本中获取视频源的方式只有两种：Udp推流SB或SB主动拉取RTMP） |
| `vl_data` | SB→Android | 视觉理解结果 | `agentId`, `content` |
| `vl_end` | SB→Android | 视觉理解结束 | `agentId` |
| `vl_error` | SB→Android | 视觉理解异常 | `agentId`, `code`, `message` |

##### 5. 控制命令类（Android ↔ SB ↔ RK）

| Event | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `control_command_an` | Android→SB | 控制RK设备 | `deviceId`, `command`, `params`（Map<String, String>） |
| `control_command_sb` | SB→RK | 转发控制命令 | `commandId`, `command`, `params`（Map<String, String>） |
| `command_result` | RK→SB | 命令执行结果 | `commandId`, `code`, `message` |
| `control_response` | SB→Android | 控制结果响应 | `deviceId`, `code`, `message` |

##### 6. 设备状态类（RK ↔ SB ↔ Android）

| Event | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `status_request` | Android/SB→RK | 请求设备状态 | `deviceId` |
| `rk_status` | RK→SB | 设备状态上报 | `deviceId`, `battery`, `position` |
| `rk_status` | SB→Android | 转发设备状态 | `deviceId`, `battery`, `position` |

##### 7. 系统消息类（三端通用）

| Event | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `error` | 双向 | 错误信息 | `code`, `message` |
| `system_message` | SB→双向 | 系统通知 | `event`, `param`（Map<String, String>） |

---


#### Channel设计

Channel为一类Event的通道

UnifiedWsHandler + MessageRouter (Channel分发器) + ChannelHandler + 线程池架构

| Channel | 包含Event | 方向 | 说明                       |
|---------|----------|------|--------------------------|
| connection | connect, connect_ack, rk_connect, rk_connect_ack, ping, pong | 三端双向 | 连接管理、心跳保活                |
| agent | agent_list_sync, agent_update | Android ↔ SB | Agent配置同步                |
| chat | chat_message_sync, chat_message_send | Android ↔ SB | 聊天消息传输                   |
| stt | stt_start, stt_start_ack, stt_audio_data, stt_text_data, stt_end, stt_error | Android ↔ SB | 语音识别数据流                  |
| llm | llm_start, llm_data, llm_end, llm_error | SB → Android | LLM文本流                   |
| tts | tts_start, tts_data, tts_end, tts_error | SB → Android | TTS音频流                   |
| vl | vl_start, vl_data, vl_end, vl_error | Android ↔ SB | 视觉理解数据流（V2版本由UDP或RTMP替代） |
| control | control_command_an, control_command_sb, command_result, control_response | Android → SB → RK → SB → Android | 设备控制命令                   |
| status | status_request, rk_status | RK ↔ SB ↔ Android | 设备状态上报与查询                |
| system | error, system_message | 三端双向 | 系统消息与错误通知                |

#### UnifiedWsHandler 类图

```mermaid
classDiagram
    class UnifiedWsHandler {
        -MessageRouter messageRouter
        -ConnectionManager connectionManager
        -ObjectMapper objectMapper
        -Map~String, WebSocketSession~ pendingAuthSessions
        +afterConnectionEstablished(session)
        +handleTextMessage(session, message)
        +afterConnectionClosed(session, status)
        +handleTransportError(session, exception)
        -authenticateSession(session, userId)
        -sendError(session, code, message)
    }
    
    class MessageRouter {
        -Map~WsEvent, ChannelHandler~ handlers
        -ExecutorService businessExecutor
        -ExecutorService sttExecutor
        -ExecutorService controlExecutor
        -ExecutorService llmExecutor
        -ExecutorService ttsExecutor
        +registerHandler(channel, handler)
        +route(session, event)
        -selectExecutor(channel) ExecutorService
        -getUserIdFromSession(session) String
    }
    
    class ConnectionManager {
        -ConcurrentHashMap~String, WebSocketSession~ androidSessions
        -ConcurrentHashMap~String, WebSocketSession~ rkSessions
        -ConcurrentHashMap~String, String~ deviceToUser
        +registerAndroid(userId, session)
        +registerRk(deviceId, session, userId)
        +getAndroidSession(userId) WebSocketSession
        +getRkSession(deviceId) WebSocketSession
        +unregister(userId)
        +unregisterRk(deviceId)
        +getUserIdByDeviceId(deviceId) String
    }
    
    class AgentBindingManager {
        -ConcurrentHashMap~String, String~ agentToUser
        -ConcurrentHashMap~String, Set~ userToAgents
        +bindAgent(agentId, userId)
        +unbindAgent(agentId, userId)
        +getUserIdByAgentId(agentId) String
        +getAgentsByUserId(userId) Set~String~
    }
    
    UnifiedWsHandler --> MessageRouter
    UnifiedWsHandler --> ConnectionManager
    MessageRouter --> ConnectionManager
    MessageRouter --> AgentBindingManager
```


#### ChannelHandler 全量划分

```mermaid
classDiagram
    class ChannelHandler {
        <<interface>>
        +handle(session, event)
        +getSupportedChannel() WsEvent
    }
    
    class ConnectChannelHandler {
        -ConnectionManager connectionManager
        +handle(session, event)
        -handleConnect(session, event)
        -handleRkConnect(session, event)
        -sendAck(session, channel, code, message)
    }
    
    class PingChannelHandler {
        +handle(session, event)
        -handlePing(session)
        -handlePong(session)
    }
    
    class AgentChannelHandler {
        -AgentBindingManager bindingManager
        -AgentService agentService
        +handle(session, event)
        -handleAgentUpdate(session, event)
        -broadcastAgentList(userId)
    }
    
    class ChatChannelHandler {
        -AgentBindingManager bindingManager
        -ChatForwardService chatForwardService
        +handle(session, event)
        -handleChatMessageSend(session, event)
    }
    
    class SttChannelHandler {
        -SttService sttService
        -AudioBufferManager bufferManager
        +handle(session, event)
        -handleSttStart(session, event)
        -handleSttAudioData(session, event)
        -handleSttEnd(session, event)
        -handleSttError(session, event)
    }
    
    class LlmChannelHandler {
        -LlmService llmService
        -LlmStreamManager streamManager
        +handle(session, event)
        -handleLlmStart(session, event)
        -handleLlmData(session, event)
        -handleLlmEnd(session, event)
        -handleLlmError(session, event)
    }
    
    class TtsChannelHandler {
        -TtsService ttsService
        -AudioStreamManager streamManager
        +handle(session, event)
        -handleTtsStart(session, event)
        -handleTtsData(session, event)
        -handleTtsEnd(session, event)
        -handleTtsError(session, event)
    }
    
    class VlChannelHandler {
        -VlService vlService
        +handle(session, event)
        -handleVlStart(session, event)
        -handleVlData(session, event)
        -handleVlEnd(session, event)
        -handleVlError(session, event)
    }
    
    class ControlChannelHandler {
        -ControlForwardService controlService
        -ConnectionManager connectionManager
        +handle(session, event)
        -handleControlCommand(session, event)
        -handleCommandResult(session, event)
        -sendControlResponse(userId, deviceId, code, message)
    }
    
    class StatusChannelHandler {
        -StatusService statusService
        -ConnectionManager connectionManager
        +handle(session, event)
        -handleStatusRequest(session, event)
        -handleRkStatus(session, event)
        -broadcastStatus(deviceId, status)
    }
    
    class SystemChannelHandler {
        -SystemMessageService systemService
        +handle(session, event)
        -handleError(session, event)
        -handleSystemMessage(session, event)
    }
    
    ChannelHandler <|.. ConnectChannelHandler
    ChannelHandler <|.. PingChannelHandler
    ChannelHandler <|.. AgentChannelHandler
    ChannelHandler <|.. ChatChannelHandler
    ChannelHandler <|.. SttChannelHandler
    ChannelHandler <|.. LlmChannelHandler
    ChannelHandler <|.. TtsChannelHandler
    ChannelHandler <|.. VlChannelHandler
    ChannelHandler <|.. ControlChannelHandler
    ChannelHandler <|.. StatusChannelHandler
    ChannelHandler <|.. SystemChannelHandler
```


#### MessageRouter 整体通信图

```mermaid
flowchart TB
    subgraph WebSocket线程
        A[UnifiedWsHandler<br/>接收消息] --> B[解析JSON为ClientEvent]
        B --> C[MessageRouter.route]
    end
    
    C --> D{选择线程池}
    
    D -->|connect/rk_connect/ping| E[businessExecutor]
    D -->|agent_update/chat_message_send| E
    D -->|control_command_an/command_result| F[controlExecutor]
    D -->|stt_*| G[sttExecutor]
    D -->|llm_*| H[llmExecutor]
    D -->|tts_*| I[ttsExecutor]
    D -->|vl_*| J[vlExecutor]
    D -->|status_request/rk_status| E
    D -->|error/system_message| E
    
    subgraph businessExecutor [业务线程池 - 核心10/最大20]
        E --> K[ConnectChannelHandler]
        E --> L[PingChannelHandler]
        E --> M[AgentChannelHandler]
        E --> N[ChatChannelHandler]
        E --> O[StatusChannelHandler]
        E --> P[SystemChannelHandler]
    end
    
    subgraph controlExecutor [控制命令线程池 - 核心5/最大10]
        F --> Q[ControlChannelHandler]
    end
    
    subgraph sttExecutor [STT线程池 - 核心2/最大4]
        G --> R[SttChannelHandler]
    end
    
    subgraph llmExecutor [LLM线程池 - 核心3/最大6]
        H --> S[LlmChannelHandler]
    end
    
    subgraph ttsExecutor [TTS线程池 - 核心2/最大4]
        I --> T[TtsChannelHandler]
    end
    
    subgraph vlExecutor [VL线程池 - 核心1/最大2]
        J --> U[VlChannelHandler]
    end
    
    K --> V[更新ConnectionManager]
    L --> W[发送pong响应]
    M --> X[更新AgentBindingManager]
    N --> Y[ChatForwardService]
    O --> Z[StatusBroadcastService]
    P --> AA[SystemMessageService]
    Q --> AB[ControlForwardService]
    R --> AC[SttService + 音频处理]
    S --> AD[LlmService + 流式处理]
    T --> AE[TtsService + 音频合成]
    U --> AF[VlService + 视觉理解]
    
    Y --> AG[AndroidMessageQueue]
    Z --> AG
    AA --> AG
    AB --> AH[RkMessageQueue]
    
    AG --> AI[发送给Android客户端]
    AH --> AJ[发送给RK设备]
```


#### 线程池类图


```mermaid
classDiagram
    class ThreadPoolConfig {
        +businessExecutor() ExecutorService
        +sttExecutor() ExecutorService
        +controlExecutor() ExecutorService
        +llmExecutor() ExecutorService
        +ttsExecutor() ExecutorService
        +vlExecutor() ExecutorService
        +monitoringExecutor() ScheduledExecutorService
    }
    
    class CustomThreadPool {
        -String poolName
        -ThreadPoolExecutor executor
        -AtomicLong submittedCount
        -AtomicLong completedCount
        +submit(task) Future
        +getActiveCount() int
        +getQueueSize() int
        +getPoolStatus() PoolStatus
    }
    
    class PoolStatus {
        +String poolName
        +int corePoolSize
        +int maxPoolSize
        +int activeCount
        +int poolSize
        +int queueSize
        +long completedTaskCount
        +long submittedCount
    }
    
    class ThreadPoolMonitor {
        -List~CustomThreadPool~ pools
        -AlertService alertService
        +monitorPools()
        +checkBackPressure()
        +logPoolStatus()
        +alertIfThresholdExceeded()
    }
    
    class MessageRouter {
        -ExecutorService businessExecutor
        -ExecutorService sttExecutor
        -ExecutorService controlExecutor
        -ExecutorService llmExecutor
        -ExecutorService ttsExecutor
        -ExecutorService vlExecutor
        +route(session, event)
        -selectExecutor(channel) ExecutorService
    }
    
    ThreadPoolConfig --> CustomThreadPool : creates
    CustomThreadPool --> PoolStatus : exposes
    ThreadPoolMonitor --> CustomThreadPool : monitors
    MessageRouter --> CustomThreadPool : uses
```

#### 线程池对象图


```mermaid
graph TB
    subgraph ThreadPoolConfig
        A[ThreadPoolConfig]
    end
    
    subgraph BusinessThreadPool [业务线程池]
        B1[corePoolSize: 10]
        B2[maxPoolSize: 20]
        B3[queueCapacity: 1000]
        B4[keepAliveTime: 60s]
        B5[rejectPolicy: CallerRunsPolicy]
        B6[threadName: business-%d]
    end
    
    subgraph ControlThreadPool [控制命令线程池]
        C1[corePoolSize: 5]
        C2[maxPoolSize: 10]
        C3[queueCapacity: 200]
        C4[keepAliveTime: 60s]
        C5[rejectPolicy: CallerRunsPolicy]
        C6[threadName: control-%d]
    end
    
    subgraph SttThreadPool [STT线程池]
        D1[corePoolSize: 2]
        D2[maxPoolSize: 4]
        D3[queueCapacity: 100]
        D4[keepAliveTime: 60s]
        D5[rejectPolicy: DiscardOldestPolicy]
        D6[threadName: stt-%d]
    end
    
    subgraph LlmThreadPool [LLM线程池]
        E1[corePoolSize: 3]
        E2[maxPoolSize: 6]
        E3[queueCapacity: 200]
        E4[keepAliveTime: 60s]
        E5[rejectPolicy: CallerRunsPolicy]
        E6[threadName: llm-%d]
    end
    
    subgraph TtsThreadPool [TTS线程池]
        F1[corePoolSize: 2]
        F2[maxPoolSize: 4]
        F3[queueCapacity: 100]
        F4[keepAliveTime: 60s]
        F5[rejectPolicy: DiscardOldestPolicy]
        F6[threadName: tts-%d]
    end
    
    subgraph VlThreadPool [VL线程池]
        G1[corePoolSize: 1]
        G2[maxPoolSize: 2]
        G3[queueCapacity: 50]
        G4[keepAliveTime: 60s]
        G5[rejectPolicy: DiscardOldestPolicy]
        G6[threadName: vl-%d]
    end
    
    A --> BusinessThreadPool
    A --> ControlThreadPool
    A --> SttThreadPool
    A --> LlmThreadPool
    A --> TtsThreadPool
    A --> VlThreadPool
```

#### 异步处理层次设计

```mermaid
gantt
    title WebSocket消息处理时序图
    dateFormat HH:mm:ss.SSS
    axisFormat %H:%M:%S
    
    section WebSocket线程
    接收消息 :t0, 00:00:00.000, 1ms
    JSON解析 :t1, after t0, 1ms
    路由分发 :t2, after t1, 1ms
    
    section 业务线程池
    ConnectHandler处理 :t3, after t2, 50ms
    AgentHandler处理 :t4, after t2, 100ms
    ChatHandler处理 :t5, after t2, 80ms
    
    section 控制线程池
    ControlHandler处理 :t6, after t2, 30ms
    转发给RK :t7, after t6, 20ms
    
    section STT线程池
    STT开始 :t8, after t2, 10ms
    音频循环处理 :t9, after t8, 500ms
    STT结束 :t10, after t9, 10ms
    
    section LLM线程池
    LLM开始 :t11, after t2, 10ms
    LLM流式处理 :t12, after t11, 2000ms
    LLM结束 :t13, after t12, 10ms
    
    section 发送队列
    入队消息 :t14, after t3, 1ms
    出队发送 :t15, after t14, 5ms
```


#### 线程池隔离设计原则

```mermaid
flowchart TB
    subgraph WebSocket线程
        A[WebSocket IO Thread]
    end
    
    subgraph 线程池隔离层
        B[业务线程池<br/>处理普通业务]
        C[控制线程池<br/>高优先级快速响应]
        D[STT线程池<br/>音频处理隔离]
        E[LLM线程池<br/>流式处理隔离]
        F[TTS线程池<br/>音频合成隔离]
        G[VL线程池<br/>视觉处理隔离]
    end
    
    subgraph 共享资源
        H[(数据库连接池)]
        I[(Redis)]
        J[HTTP客户端池]
    end
    
    A -->|1. 快速分发| B
    A -->|2. 快速分发| C
    A -->|3. 快速分发| D
    A -->|4. 快速分发| E
    A -->|5. 快速分发| F
    A -->|6. 快速分发| G
    
    B -->|共享| H
    C -->|共享| H
    D -->|独占| J
    E -->|独占| I
    F -->|独占| J
    G -->|独占| J
    
    subgraph 背压控制
        K[队列容量监控]
        L[拒绝策略]
        M[动态调整线程数]
    end
    
    B -.-> K
    C -.-> K
    D -.-> K
    E -.-> K
    F -.-> K
    G -.-> K
```




























