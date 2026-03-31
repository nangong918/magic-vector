

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
        A[MainActivity] --> B[ChatService]
        B --> C[UnifiedWebSocketManager]

        C --> D[连接管理]
        D --> E[发送队列<br/>容量32]
        D --> F[重连机制]
        D --> G[接收队列<br/>容量32]

        C --> H[消息分发器]
        H --> I{消息类型}

        I -->|chat_message| J[ChatEventMapManager]
        I -->|control_response| K[ControlEventManager]
        I -->|rk_status| L[DeviceStatusManager]
        I -->|bind_ack| M[AgentEventManager]

        J --> N[UI更新]
        K --> N
        L --> N
        M --> N

        O[用户操作] --> P{操作类型}
        P -->|选择Agent| Q[bind_agent]
        P -->|发送消息| R[chat_message]
        P -->|控制RK| S[control_command]

        Q -->|入队| E
        R -->|入队| E
        S -->|入队| E

        E -->|出队| C
        G -->|出队| H
    end
```

### 优化后的SpringBoot WS架构

```mermaid
flowchart TD
    subgraph SpringBoot端
        A[UnifiedWsHandler] --> B[WebSocket连接管理]

        B --> C[Android连接池<br/>userId -> session]
        B --> D[RK连接池<br/>deviceId -> session]
        B --> E[Android接收队列<br/>容量32]
        B --> F[Android发送队列<br/>容量32]
        B --> G[RK接收队列<br/>容量32]
        B --> H[RK发送队列<br/>容量32]

        A --> I[消息路由器]

        I --> J{消息类型}

        J -->|connect| K[注册Android连接]
        J -->|rk_connect| L[注册RK连接]
        J -->|bind_agent| M[AgentBindingService]
        J -->|chat_message| N[ChatForwardService]
        J -->|control_command| O[ControlForwardService]
        J -->|command_result| P[结果回传]
        J -->|status_report| Q[状态广播]

        M --> R[存储 agentId -> userId]
        N --> S[根据agentId查userId]
        S --> T[入队Android发送队列]

        O --> U[根据deviceId查RK连接]
        U --> V[入队RK发送队列]

        P --> W[根据userId查Android连接]
        W --> X[入队Android发送队列]

        Q --> Y[根据deviceId查userId]
        Y --> Z[入队Android发送队列]

        T --> F
        V --> H
        X --> F
        Z --> F

        E -->|出队| I
        F -->|出队| A
        G -->|出队| I
        H -->|出队| A
    end
```


### RK框架

```mermaid
flowchart TD
    subgraph RK设备端
        A[RKControlService] --> B[RKWebSocketManager]

        B --> C[连接管理]
        C --> D[心跳保活]
        C --> E[断线重连]
        C --> F[发送队列<br/>容量32]
        C --> G[接收队列<br/>容量32]

        B --> H[消息收发]

        H --> I{消息类型}

        I -->|control_command| J[命令执行器]
        I -->|status_request| K[状态上报器]

        J --> L[马达控制]
        J --> M[传感器读取]
        J --> N[LED控制]

        L --> O[入队发送队列]
        M --> O
        N --> O

        K --> P[采集传感器数据]
        P --> Q[入队发送队列]

        R[硬件事件] --> S[传感器中断]
        S --> K
        R --> T[按钮按下]
        T --> K

        F -->|出队| B
        G -->|出队| H
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

#### Channel 枚举设计

##### 1. 连接管理类（三端通用）

| Channel | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `connect` | Android→SB | 建立用户连接 | `userId` |
| `connect_ack` | SB→Android | 连接确认 | `code`, `message` |
| `rk_connect` | RK→SB | RK设备注册 | `deviceId`, `firmware` |
| `rk_connect_ack` | SB→RK | RK注册确认 | `code`, `message` |
| `ping` | 双向 | 心跳请求 | - |
| `pong` | 双向 | 心跳响应 | - |

> ping, pong超时策略（如连续3次无响应则断开重连）

##### 2. Agent数据同步类（Android ↔ SB）

| Channel | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `agent_list_sync` | SB→Android | Agent列表变更推送 | `AgentChatDto` 列表 |
| `agent_update` | Android→SB | 用户操作Agent（创建/更新/删除） | `AgentChatDto` |

##### 3. 聊天消息类（Android ↔ SB）

| Channel | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `chat_message_sync` | SB→Android | 聊天消息推送 | `ChatMessageDto` |
| `chat_message_send` | Android→SB | 用户发送消息 | `ChatMessageDto` |

##### 4. 音频处理类（Android ↔ SB）

| Channel | 方向 | 说明 | data字段 |
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

| Channel | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `control_command_an` | Android→SB | 控制RK设备 | `deviceId`, `command`, `params`（Map<String, String>） |
| `control_command_sb` | SB→RK | 转发控制命令 | `commandId`, `command`, `params`（Map<String, String>） |
| `command_result` | RK→SB | 命令执行结果 | `commandId`, `code`, `message` |
| `control_response` | SB→Android | 控制结果响应 | `deviceId`, `code`, `message` |

##### 6. 设备状态类（RK ↔ SB ↔ Android）

| Channel | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `status_request` | Android/SB→RK | 请求设备状态 | `deviceId` |
| `rk_status` | RK→SB | 设备状态上报 | `deviceId`, `battery`, `position` |
| `rk_status` | SB→Android | 转发设备状态 | `deviceId`, `battery`, `position` |

##### 7. 系统消息类（三端通用）

| Channel | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `error` | 双向 | 错误信息 | `code`, `message` |
| `system_message` | SB→双向 | 系统通知 | `event`, `param`（Map<String, String>） |

---

#### Channel 枚举定义

##### Kotlin 版本（Android / RK）

```kotlin
enum class WsChannel(val value: String) {
    // 连接管理
    CONNECT("connect"),
    CONNECT_ACK("connect_ack"),
    RK_CONNECT("rk_connect"),
    RK_CONNECT_ACK("rk_connect_ack"),
    PING("ping"),
    PONG("pong"),

    // Agent数据同步
    AGENT_LIST_SYNC("agent_list_sync"),
    AGENT_UPDATE("agent_update"),

    // 聊天消息
    CHAT_MESSAGE_SYNC("chat_message_sync"),
    CHAT_MESSAGE_SEND("chat_message_send"),

    // STT 语音识别
    STT_START("stt_start"),
    STT_START_ACK("stt_start_ack"),
    STT_AUDIO_DATA("stt_audio_data"),
    STT_TEXT_DATA("stt_text_data"),
    STT_END("stt_end"),
    STT_ERROR("stt_error"),

    // LLM 语言模型
    LLM_START("llm_start"),
    LLM_DATA("llm_data"),
    LLM_END("llm_end"),
    LLM_ERROR("llm_error"),

    // TTS 语音合成
    TTS_START("tts_start"),
    TTS_DATA("tts_data"),
    TTS_END("tts_end"),
    TTS_ERROR("tts_error"),

    // VL 视觉理解
    VL_START("vl_start"),
    VL_DATA("vl_data"),
    VL_END("vl_end"),
    VL_ERROR("vl_error"),

    // 控制命令
    CONTROL_COMMAND_AN("control_command_an"),
    CONTROL_COMMAND_SB("control_command_sb"),
    COMMAND_RESULT("command_result"),
    CONTROL_RESPONSE("control_response"),

    // 设备状态
    STATUS_REQUEST("status_request"),
    RK_STATUS("rk_status"),

    // 系统消息
    ERROR("error"),
    SYSTEM_MESSAGE("system_message")
}
```

Java 版本（SpringBoot）

```java
public enum WsChannel {
    // 连接管理
    CONNECT("connect"),
    CONNECT_ACK("connect_ack"),
    RK_CONNECT("rk_connect"),
    RK_CONNECT_ACK("rk_connect_ack"),
    PING("ping"),
    PONG("pong"),

    // Agent数据同步
    AGENT_LIST_SYNC("agent_list_sync"),
    AGENT_UPDATE("agent_update"),

    // 聊天消息
    CHAT_MESSAGE_SYNC("chat_message_sync"),
    CHAT_MESSAGE_SEND("chat_message_send"),

    // STT 语音识别
    STT_START("stt_start"),
    STT_START_ACK("stt_start_ack"),
    STT_AUDIO_DATA("stt_audio_data"),
    STT_TEXT_DATA("stt_text_data"),
    STT_END("stt_end"),
    STT_ERROR("stt_error"),

    // LLM 语言模型
    LLM_START("llm_start"),
    LLM_DATA("llm_data"),
    LLM_END("llm_end"),
    LLM_ERROR("llm_error"),

    // TTS 语音合成
    TTS_START("tts_start"),
    TTS_DATA("tts_data"),
    TTS_END("tts_end"),
    TTS_ERROR("tts_error"),

    // VL 视觉理解
    VL_START("vl_start"),
    VL_DATA("vl_data"),
    VL_END("vl_end"),
    VL_ERROR("vl_error"),

    // 控制命令
    CONTROL_COMMAND_AN("control_command_an"),
    CONTROL_COMMAND_SB("control_command_sb"),
    COMMAND_RESULT("command_result"),
    CONTROL_RESPONSE("control_response"),

    // 设备状态
    STATUS_REQUEST("status_request"),
    RK_STATUS("rk_status"),

    // 系统消息
    ERROR("error"),
    SYSTEM_MESSAGE("system_message");

    private final String value;

    WsChannel(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
```

