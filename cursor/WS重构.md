

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
        D --> E[发送队列]
        D --> F[重连机制]

        C --> G[消息分发器]
        G --> H{消息类型}

        H -->|chat_message| I[ChatEventMapManager]
        H -->|control_response| J[ControlEventManager]
        H -->|rk_status| K[DeviceStatusManager]
        H -->|bind_ack| L[AgentEventManager]

        I --> M[UI更新]
        J --> M
        K --> M
        L --> M

        N[用户操作] --> O{操作类型}
        O -->|选择Agent| P[bind_agent]
        O -->|发送消息| Q[chat_message]
        O -->|控制RK| R[control_command]

        P --> C
        Q --> C
        R --> C
    end
```

### 优化后的SpringBoot WS架构

```mermaid
flowchart TD
    subgraph SpringBoot端
        A[UnifiedWsHandler] --> B[WebSocket连接管理]

        B --> C[Android连接池<br/>userId -> session]
        B --> D[RK连接池<br/>deviceId -> session]

        A --> E[消息路由器]

        E --> F{消息类型}

        F -->|connect| G[注册Android连接]
        F -->|rk_connect| H[注册RK连接]
        F -->|bind_agent| I[AgentBindingService]
        F -->|chat_message| J[ChatForwardService]
        F -->|control_command| K[ControlForwardService]

        I --> L[存储 agentId -> userId]

        J --> M[根据agentId查userId]
        M --> N[通过Android连接推送]

        K --> O[根据deviceId查RK连接]
        O --> P[通过RK连接转发]

        Q[RK状态上报] --> R[根据deviceId查userId]
        R --> S[通过Android连接推送]
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
        
        B --> F[消息收发]
        
        F --> G{消息类型}
        
        G -->|control_command| H[命令执行器]
        G -->|status_request| I[状态上报器]
        
        H --> J[马达控制]
        H --> K[传感器读取]
        H --> L[LED控制]
        
        I --> M[采集传感器数据]
        M --> N[上报状态]
        
        O[硬件事件] --> P[传感器中断]
        P --> I
        O --> Q[按钮按下]
        Q --> I
    end
```

### 整体数据流
```mermaid
graph LR
    subgraph Android
        A1[用户操作]
        A2[WebSocket客户端]
        A3[消息分发器]
        A4[UI更新]
    end

    subgraph SpringBoot
        B1[WebSocket服务端]
        B2[连接管理器]
        B3[消息路由器]
        B4[Agent绑定服务]
        B5[控制转发服务]
    end

    subgraph RK设备
        C1[WebSocket客户端]
        C2[命令执行器]
        C3[状态采集器]
    end

    A1 -->|1. bind_agent| A2
    A1 -->|2. chat_message| A2
    A1 -->|3. control_command| A2

    A2 <-->|WebSocket| B1

    B1 --> B2
    B2 --> B3

    B3 -->|bind_agent| B4
    B3 -->|chat_message| B4
    B3 -->|control_command| B5

    B4 -->|转发聊天| B2
    B5 -->|转发控制| B1

    B1 <-->|WebSocket| C1

    C1 --> C2
    C1 --> C3

    C2 -->|命令结果| C1
    C3 -->|状态上报| C1

    B2 -->|推送消息| A2
    A2 --> A3
    A3 -->|chat_message| A4
    A3 -->|control_response| A4
    A3 -->|rk_status| A4
```
