

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
    subgraph 初始化与连接管理
        Start([应用启动]) --> InitWS[初始化 WebSocketService<br/>创建 CoroutineScope]
        InitWS --> CreateManager[创建 UnifiedWebSocketManager]
        CreateManager --> ConnLoop{连接状态}
        ConnLoop -->|未连接| Connect[connect 协程 IO<br/>建立 WS 连接]
        Connect --> SendAuth[发送认证信息<br/>channel:connection, event:auth]
        SendAuth --> WaitAck[等待 connect_ack]
        WaitAck -->|成功| StartPing[启动 ping/pong 心跳]
        WaitAck -->|失败/超时| ReconnDelay[延迟重连<br/>指数退避]
        ReconnDelay --> Connect
        StartPing --> ConnEstablished[连接已建立]
        ConnEstablished --> DispatchLoop[启动接收分发循环<br/>Dispatchers.Default]
    end

    subgraph 消息发送通道
        UserAction[用户操作] --> ActionType{操作类型}
        ActionType -->|创建/删除/编辑 Agent| AgentUpdate[构造 agent_update 消息<br/>channel:agent]
        ActionType -->|发送聊天文本| ChatSend[构造 chat_message_send<br/>channel:chat]
        ActionType -->|控制 RK 设备| ControlCmd[构造 control_command<br/>channel:control]
        ActionType -->|开始语音交互| VoiceStart[启动语音交互流程]
        ActionType -->|HTTP 请求| HttpReq[协程 IO 请求<br/>GET /api/agents 或 /api/chat/history]
        HttpReq --> UpdateCache[更新本地缓存<br/>withContext Main]

        AgentUpdate --> SendMsg[发送到发送 Channel<br/>容量 32]
        ChatSend --> SendMsg
        ControlCmd --> SendMsg

        SendMsg --> WsSend[WebSocket 发送<br/>协程 IO]
    end

    subgraph 语音交互详细流程
        VoiceStart --> CheckAgentState{isAgentReplying?}
        CheckAgentState -->|是| RejectVoice[忽略本次唤醒/录音<br/>提示忙碌]
        RejectVoice --> EndVoice([结束])
        CheckAgentState -->|否| WakeUp[讯飞离线唤醒 SDK 唤醒]
        WakeUp --> StartRecord[开始录音<br/>启动 2s 超时定时器]
        StartRecord --> ParallelVoice{并行执行}

        ParallelVoice --> AudioPush[循环读取麦克风 buffer<br/>发送 stt_audio_data 分片<br/>含 seq 序列号]
        AudioPush --> WsSend

        ParallelVoice --> VideoPush[视频推流独立协程<br/>UDP/RTMP 发送视频帧]
        VideoPush --> VideoOut[视频流输出]

        ParallelVoice --> VadTimer[2s 超时检测]
        VadTimer -->|2s 内无语音| StopRec1[停止录音<br/>发送 stt_end]
        VadTimer -->|2s 内检测到语音| VadLoop[VAD 循环检测]
        VadLoop -->|静音超过阈值| StopRec2[停止录音<br/>发送 stt_end]

        StopRec1 --> WaitReply[等待服务器回复]
        StopRec2 --> WaitReply
    end

    subgraph 消息接收与分发
        DispatchLoop --> RecvMsg[从接收 Channel 取消息<br/>容量 32]
        RecvMsg --> ParseChannel{解析 channel 字段}

        ParseChannel -->|agent| AgentHandler[AgentEventHandler<br/>处理 agent_list_sync]
        ParseChannel -->|chat| ChatHandler[ChatEventHandler<br/>处理 chat_message_sync]
        ParseChannel -->|stt| SttHandler[STTEventHandler<br/>处理 stt_start_ack, stt_text_data, stt_error]
        ParseChannel -->|llm| LlmHandler[LLMEventHandler<br/>处理 llm_start/llm_data/llm_end]
        ParseChannel -->|tts| TtsHandler[TTSEventHandler<br/>流式 tts_*, 非批量回复路径]
        ParseChannel -->|instruction_list| InstListHandler[InstructionListEventHandler<br/>收到 instruction_list, 展开入队]
        ParseChannel -->|control| CtrlHandler[ControlEventHandler<br/>收到 control_command / control_response]
        ParseChannel -->|system| SysHandler[SystemEventHandler<br/>处理 error, system_message]
        ParseChannel -->|status| StatusHandler[StatusEventHandler<br/>处理 rk_status]
        ParseChannel -->|connection| ConnHandler[ConnectionEventHandler<br/>处理 ping/pong/connect_ack]

        AgentHandler --> UiUpdate[更新 UI<br/>withContext Main]
        ChatHandler --> UiUpdate
        SttHandler --> UiUpdate
        LlmHandler --> UiUpdate
        InstListHandler --> UiUpdate
        StatusHandler --> UiUpdate
        ConnHandler --> Heartbeat[更新心跳时间]

        SysHandler --> CheckAgentStateChange{是否为 agent_start_reply<br/>或 agent_end_reply?}
        CheckAgentStateChange -->|agent_start_reply| SetReplying[设置 isAgentReplying = true<br/>关闭唤醒 + 禁用录音]
        CheckAgentStateChange -->|agent_end_reply| SetIdle[设置 isAgentReplying = false<br/>恢复唤醒 + 恢复录音]
        CheckAgentStateChange -->|其他| LogError[记录错误或忽略]

        CtrlHandler --> CheckMcp{是否为 MCP 指令?<br/>且非 instruction_list 编排内}
        CheckMcp -->|是| SubmitInstruction[提交到 InstructionExecutor 队列<br/>独立控制通道场景]
        CheckMcp -->|否| ForwardControl[转发给普通控制处理器]

        TtsHandler --> SubmitTtsStream[流式路径: 提交 TTS 分片到播放或缓冲<br/>与批量 instruction_list 互斥由会话状态约定]
        InstListHandler --> SubmitBatch[批量路径: 按序提交列表项到 InstructionExecutor 队列]
    end

    subgraph 顺序指令执行器
        InstructionExecutor[InstructionExecutor<br/>单线程协程 Dispatchers.Main.immediate] --> QueueLoop[循环从队列取指令]
        QueueLoop --> InstType{指令类型}
        InstType -->|TTS 音频| PlayTTS[调用 AudioPlayer 播放]
        PlayTTS --> WaitPlay[等待播放完成回调]
        WaitPlay --> SendComplete["发送 instruction_result<br/>channel:instruction_list, event:instruction_result<br/>携带 request_id、index、type、status"]
        InstType -->|MCP 指令| ExecMcp{目标设备?}
        ExecMcp -->|Android| ExecLocal[执行本地指令<br/>如 GPIO/舵机/LCD]
        ExecMcp -->|RK| ForwardRk[通过 WebSocket 转发给 RK<br/>等待 RK 回复确认]
        ExecLocal --> WaitLocalDone[等待执行完成]
        ForwardRk --> WaitRkAck[等待 RK command_result<br/>本地聚合后再发 instruction_result]
        WaitLocalDone --> SendComplete
        WaitRkAck --> SendComplete
        SendComplete --> QueueLoop
    end

    subgraph 视频推流独立模块
        VideoPush --> VideoMode{传输模式}
        VideoMode -->|高速模式| UdpSend[UDP 推流到 SpringBoot UDP 服务]
        VideoMode -->|防花屏模式| RtmpSend[RTMP 推流到 Nginx-RTMP]
        UdpSend --> VideoEnd[持续推流直至停止]
        RtmpSend --> VideoEnd

        VideoPlay[拉流播放] --> PlayMode{播放来源}
        PlayMode -->|UDP| UdpRecv[UDP 接收协程 IO]
        PlayMode -->|RTMP| RtmpRecv[RTMP 拉流协程 IO]
        UdpRecv --> Decode[解码视频帧 Dispatchers.Default]
        RtmpRecv --> Decode
        Decode --> Render[渲染到 SurfaceView<br/>withContext Main]
    end

    subgraph 异常与状态恢复
    AnyError["何环节发生异常<br/>如 WS 断开、超时、解析失败"] --> ResetIdle[调用 resetToIdle]
    ResetIdle --> StopPlaying[停止当前 TTS 播放]
    ResetIdle --> ClearQueue[清空 InstructionExecutor 队列]
    ResetIdle --> SetIdleState[设置 isAgentReplying = false<br/>恢复唤醒 + 恢复录音]
    SetIdleState --> NotifyServer[发送 error_reset 通知服务器]
    NotifyServer --> ReconnectWs[触发 WebSocket 重连]
    ReconnectWs --> ConnLoop
    end
    
    %% 连接关系补全
    WsSend -.-> RecvMsg
    SendComplete -.-> WsSend
    NotifyServer -.-> WsSend
    SetReplying -.-> CheckAgentState
    SetIdle -.-> CheckAgentState
    InstructionExecutor -.-> UiUpdate
    ExecLocal -.-> UiUpdate
```

### 优化后的SpringBoot WS架构

```mermaid
flowchart TD
    subgraph SpringBoot端完整活动图
        Start([SpringBoot启动]) --> Init[初始化组件<br/>WebSocket Server<br/>UDPServer/RTMPServer<br/>线程池等]

        %% 主循环：WebSocket消息处理
        Init --> WsLoop[WebSocket IO线程<br/>持续接收消息]
        WsLoop --> ParseMsg[解析消息<br/>提取channel和event]
        
        ParseMsg --> RouteChannel{Channel路由}
        
        %% 各Handler处理
        RouteChannel -->|connection| ConnHandler[ConnectionHandler<br/>处理connect/rk_connect/ping/pong]
        ConnHandler --> SendAck[发送connect_ack/pong]
        SendAck --> WsLoop
        
        RouteChannel -->|agent| AgentHandler[AgentHandler<br/>处理agent_update]
        AgentHandler --> StoreAgent[存储agentId->userId]
        StoreAgent --> BroadcastList[广播agent_list_sync给所有Android]
        BroadcastList --> WsLoop
        
        RouteChannel -->|chat| ChatHandler[ChatHandler<br/>处理chat_message_send]
        ChatHandler --> QueryAgent[查询agentId对应的userId]
        QueryAgent --> SendSync[构造chat_message_sync<br/>发送给目标Android]
        SendSync --> WsLoop
        
        RouteChannel -->|control| CtrlHandler[ControlHandler<br/>处理control_command_an / command_result]
        CtrlHandler --> GenCmdId[生成commandId]
        GenCmdId --> ForwardToRk[转发给RK设备]
        ForwardToRk --> WaitResult[等待command_result]
        WaitResult --> SendResponse[返回control_response给Android]
        SendResponse --> WsLoop
        
        RouteChannel -->|instruction_list| InstListSbHandler[InstructionListChannelHandler<br/>处理 instruction_result]
        InstListSbHandler --> UpdateTracker[更新InstructionTracker<br/>标记指令完成]
        UpdateTracker --> CheckAllDone{所有指令完成?}
        CheckAllDone -->|是| TriggerEndReply[触发发送agent_end_reply]
        CheckAllDone -->|否| WsLoop
        
        RouteChannel -->|status| StatusHandler[StatusHandler<br/>处理status_request]
        StatusHandler --> QueryRk[转发给RK获取状态]
        QueryRk --> SendStatus[转发rk_status给Android]
        SendStatus --> WsLoop
        
        RouteChannel -->|stt| SttHandler[STTHandler<br/>处理stt_start/stt_audio_data/stt_end]
        SttHandler --> SttStart[stt_start: 创建会话<br/>初始化SessionCoordinator]
        SttHandler --> SttData[stt_audio_data: 转发音频流到STT服务]
        SttData --> SttFragment[STT碎片实时返回<br/>转发给Android UI]
        SttHandler --> SttEnd[stt_end: 获取STT最终结果]
        SttEnd --> SetSttResult[设置SessionCoordinator.sttResult]
        
        RouteChannel -->|vl| VlHandler[VLHandler 重新激活<br/>处理vl_start/vl_data/vl_end]
        VlHandler --> VlProcess["接收视频帧，调用VL服务"]
        VlProcess --> SetVlResult[设置SessionCoordinator.vlResult]
        
        RouteChannel -->|llm| LlmHandler[LLMHandler<br/>处理llm_start/llm_end]
        LlmHandler --> LlmCall["调用LLM服务（非流式）"]
        LlmCall --> LlmOutput[接收LLM整体输出]
        LlmOutput --> TriggerParse[触发文本解析]
        
        RouteChannel -->|tts| TtsHandler[TTSHandler<br/>处理tts_start/tts_end]
        TtsHandler --> TtsCall[调用TTS服务合成音频]
        TtsCall --> SendAudio["返回tts_data音频流给Android<br/>流式场景; 批量回复由 BatchDispatcher 走 instruction_list 通道"]
        SendAudio --> WsLoop
        
        RouteChannel -->|system| SysHandler[SystemHandler<br/>处理system_message/error]
        SysHandler --> LogEvent[记录日志/系统通知]
        LogEvent --> WsLoop

        %% 会话协调模块（异步）
        subgraph 会话协调与LLM触发
            SessionCoord[SessionCoordinator<br/>每个语音会话独立实例]
            SetSttResult --> SessionCoord
            SetVlResult --> SessionCoord
            SessionCoord --> CheckSync{STT和VL都到达?<br/>或超时}
            CheckSync -->|是| BuildContext[构建STT+VL上下文]
            BuildContext --> SendToLlm[通过LLMHandler发送给LLM]
            CheckSync -->|超时| TimeoutFallback[仅使用已有结果]
            TimeoutFallback --> BuildContext
        end

        %% 文本解析与批量下发模块
        subgraph 文本解析与批量下发
            TriggerParse --> TextParser[文本过滤分析器<br/>解析句子+MCP指令List]
            TextParser --> GenList["生成有序指令列表<br/>如: [TTS文本, MCP指令, TTS文本, ...]"]
            GenList --> SendStartReply[通过SystemHandler<br/>发送agent_start_reply给Android/RK]
            SendStartReply --> BatchDispatcher[批量指令下发器]
            BatchDispatcher --> SendList[一次性发送 instruction_list<br/>channel:instruction_list]
            SendList --> InitTracker[初始化InstructionTracker<br/>记录所有指令pending状态]
        end

        %% 指令追踪与完成通知
        subgraph 指令追踪与完成通知
            InitTracker --> WaitResults[等待instruction_result]
            UpdateTracker --> WaitResults
            TriggerEndReply --> SendEndReplyMsg[发送agent_end_reply给Android/RK]
            SendEndReplyMsg --> CleanSession[清理SessionCoordinator和Tracker]
        end
        
        %% 视频流独立处理（非WebSocket）
        subgraph 视频流处理
            VideoStart[UDP或RTMP视频流到达] --> VideoReceive[UDPServer/RTMPServer接收]
            VideoReceive --> ExtractFrame[提取视频帧]
            ExtractFrame --> VlHandler
        end
        
        %% HTTP API处理
        subgraph HTTP API
            HttpReq[HTTP请求到达] --> HttpRoute{路径路由}
            HttpRoute -->|GET /api/agents| QueryAgents[查询数据库Agent列表]
            HttpRoute -->|GET /api/chat/history| QueryHistory[查询聊天历史]
            HttpRoute -->|POST /api/agent| ModifyAgent[创建/更新/删除Agent]
            ModifyAgent --> BroadcastAgentSync[广播agent_list_sync给所有Android]
            QueryAgents --> HttpResp[返回JSON]
            QueryHistory --> HttpResp
            BroadcastAgentSync --> HttpResp
        end
    
        %% 异常与恢复
        subgraph 异常处理与恢复
            AnyException[任何环节发生异常<br/>超时/服务调用失败/解析错误] --> LogError[记录错误日志]
            LogError --> SendErrorToAndroid[发送error/system_message给Android]
            SendErrorToAndroid --> ResetSession[重置会话状态<br/>清理SessionCoordinator和Tracker]
            ResetSession --> SendEndReplyOnError[发送agent_end_reply<br/>确保设备恢复唤醒和录音]
            SendEndReplyOnError --> WsLoop
        end
    
        %% 连接断开处理
        subgraph 连接断开处理
            Disconnect[Android或RK断开连接] --> RemoveFromPool[从连接池移除]
            RemoveFromPool --> CleanSessionOnDisconnect[清理该设备相关的会话]
            CleanSessionOnDisconnect --> WsLoop
        end
    end
    
    %% 外部服务调用标注
    SttData -.->|调用| STT[STT服务]
    VlProcess -.->|调用| VL[VL服务]
    LlmCall -.->|调用| LLM[LLM服务]
    TtsCall -.->|调用| TTS[TTS服务]
```


### RK框架

```mermaid
flowchart TD
    subgraph RK设备端完整活动图
        Start([RK设备启动]) --> Init[初始化组件<br/>RKControlService<br/>RKWebSocketManager<br/>GPIO/LED/传感器驱动<br/>视频采集模块]
        
        %% WebSocket 连接与重连
        Init --> WsConnect[建立WebSocket连接<br/>协程IO]
        WsConnect --> SendRegister[发送rk_connect注册信息<br/>携带deviceId]
        SendRegister --> WaitAck[等待rk_connect_ack]
        WaitAck -->|成功| SetConnected[连接状态=已注册<br/>启动心跳ping/pong]
        WaitAck -->|失败/超时| ReconnectDelay[延迟重连<br/>指数退避]
        ReconnectDelay --> WsConnect
        
        SetConnected --> StartDispatch[启动消息接收分发循环<br/>独立协程]
        StartDispatch --> RecvLoop[从接收队列取消息<br/>容量32]
        
        %% 消息分发
        RecvLoop --> ParseMsg[解析消息channel]
        ParseMsg --> RouteChannel{Channel路由}
        
        RouteChannel -->|connection| ConnHandler[ConnectionEventHandler]
        ConnHandler --> HandlePing[处理ping/pong<br/>更新心跳时间]
        HandlePing --> RecvLoop
        
        RouteChannel -->|instruction_list| InstListRkHandler[InstructionListEventHandler<br/>解析 instruction_list<br/>按序执行本设备相关项]
        InstListRkHandler --> RkSeqLoop["RK 顺序执行循环<br/>TTS 项 → 音频线程 / MCP 项 → 硬件执行"]
        RkSeqLoop --> RkInstResult["发送 instruction_result<br/>channel:instruction_list<br/>含 requestId、index、status"]
        RkInstResult --> RecvLoop

        RouteChannel -->|control| CtrlHandler[CommandExecutor<br/>单条 control_command_sb]
        CtrlHandler --> ParseCommand[解析control_command_sb<br/>提取commandId, type, params]
        ParseCommand --> ExecType{命令类型}
        
        ExecType -->|GPIO控制| GpioExec[GPIOController<br/>设置高低电平/PWM]
        ExecType -->|LED控制| LedExec[LEDController<br/>控制LED亮灭/颜色]
        ExecType -->|舵机控制| ServoExec[ServoController<br/>控制角度/速度]
        ExecType -->|LCD显示| LcdExec[LCDController<br/>显示文本/图标]
        ExecType -->|传感器读取| SensorExec[SensorReader<br/>读取温度/湿度/距离等]
        ExecType -->|其他| OtherExec[OtherController<br/>执行其他定制指令]
        
        GpioExec --> GetResult[获取执行结果<br/>code, message]
        LedExec --> GetResult
        ServoExec --> GetResult
        LcdExec --> GetResult
        SensorExec --> GetResult
        OtherExec --> GetResult
        
        GetResult --> BuildResult[构造command_result消息<br/>channel:control, event:command_result<br/>携带commandId, code, message]
        BuildResult --> SendResult[将结果放入发送队列]
        SendResult --> RecvLoop
        
        RouteChannel -->|status| StatusHandler[StatusRequestHandler]
        StatusHandler --> CollectStatus[采集设备状态<br/>电池电量/网络信号/各传感器值]
        CollectStatus --> BuildStatus[构造rk_status消息<br/>channel:status, event:rk_status<br/>携带deviceId, statusMap]
        BuildStatus --> SendStatus[将状态放入发送队列]
        SendStatus --> RecvLoop
        
        RouteChannel -->|system| SysHandler[SystemEventHandler]
        SysHandler --> HandleSysMsg[处理error/system_message<br/>记录日志/执行系统动作]
        HandleSysMsg --> RecvLoop
        
        RouteChannel -->|其他| Ignore[忽略或记录警告]
        Ignore --> RecvLoop
        
        %% 主动事件上报（定时/中断）
        subgraph 主动事件上报
            TimerEvent[定时器 每30秒] --> TriggerStatus[触发状态采集]
            SensorInterrupt[传感器中断<br/>如人体红外] --> TriggerStatus
            ButtonPress[按钮按下] --> TriggerEvent[触发按钮事件]
            
            TriggerStatus --> CollectStatus
            TriggerEvent --> BuildButtonMsg[构造button_event消息<br/>channel:status, event:button_pressed]
            BuildButtonMsg --> SendStatus
        end
        
        %% 发送队列处理
        subgraph 消息发送
            SendQueueLoop[发送队列处理协程<br/>从发送队列取消息] --> WsSend[通过WebSocket发送<br/>协程IO]
            WsSend --> CheckSendResult{发送成功?}
            CheckSendResult -->|是| SendQueueLoop
            CheckSendResult -->|否| MarkDisconnect[标记连接断开<br/>触发重连]
            MarkDisconnect --> ReconnectDelay
        end
        
        %% 视频推流独立模块
        subgraph 视频推流
            CameraInit[初始化摄像头] --> VideoLoop[循环采集视频帧]
            VideoLoop --> ChooseMode{传输模式选择<br/>可配置}
            ChooseMode -->|高速模式| UdpPush[UDP推流<br/>直接发送到SpringBoot UDP服务]
            ChooseMode -->|防花屏模式| RtmpPush[RTMP推流<br/>推送到Nginx-RTMP服务器]
            ChooseMode -->|点对点模式| P2pPush["UDP直推<br/>RK → Android 本地推流"]
            
            UdpPush --> CheckUdpResult{推流正常?}
            RtmpPush --> CheckRtmpResult{推流正常?}
            P2pPush --> CheckP2pResult{推流正常?}
            
            CheckUdpResult -->|失败| RetryPush[重试/切换模式]
            CheckRtmpResult -->|失败| RetryPush
            CheckP2pResult -->|失败| RetryPush
            RetryPush --> VideoLoop
            CheckUdpResult -->|成功| VideoLoop
            CheckRtmpResult -->|成功| VideoLoop
            CheckP2pResult -->|成功| VideoLoop
        end
        
        %% 指令执行超时与异常处理
        subgraph 指令执行超时
            ParseCommand --> StartTimeout[启动超时定时器<br/>例如5秒]
            StartTimeout --> ExecType
            GetResult --> CancelTimeout[取消超时定时器]
            CancelTimeout --> BuildResult
            StartTimeout -->|超时未返回| TimeoutHandler[构造超时错误结果<br/>code=TIMEOUT]
            TimeoutHandler --> BuildResult
        end
        
        %% 连接断开与恢复
        subgraph 连接断开处理
            WsDisconnect[WebSocket断开] --> ClearQueues[清空发送/接收队列]
            ClearQueues --> SetDisconnected[连接状态=未注册]
            SetDisconnected --> StopHeartbeat[停止心跳]
            StopHeartbeat --> ReconnectDelay
        end
        
        %% 异常处理与状态恢复
        subgraph 全局异常处理
            AnyError[任何未捕获异常<br/>如指令执行崩溃/内存不足] --> LogError[记录错误日志]
            LogError --> SendErrorReport[发送error_report消息给SpringBoot]
            SendErrorReport --> ResetHardware[复位相关硬件<br/>GPIO恢复安全状态]
            ResetHardware --> ReconnectWs[触发WebSocket重连]
            ReconnectWs --> ReconnectDelay
        end
        
        %% 空闲状态维护
        subgraph 空闲状态
            IdleCheck[定期检查连接状态] -->|连接正常且无任务| WaitEvent[等待新指令或事件]
            WaitEvent --> RecvLoop
            WaitEvent --> TimerEvent
            WaitEvent --> SensorInterrupt
            WaitEvent --> ButtonPress
        end
    end
    
    %% 外部连接
    SendResult -.->|WebSocket| SpringBoot[SpringBoot]
    SendStatus -.->|WebSocket| SpringBoot
    UdpPush -.->|UDP| SpringBootUdp[SpringBoot UDP服务]
    RtmpPush -.->|RTMP| Nginx[Nginx-RTMP服务器]
    P2pPush -.->|UDP| Android[Android端]
```

### 整体数据流
```mermaid
graph LR
    subgraph "Android（App，RK上层）"
        A1[用户操作/语音唤醒]
        A2[发送队列<br/>容量32]
        A3[WebSocket客户端]
        A4[接收队列<br/>容量32]
        A5[消息分发器]
        A6[UI更新]
        A7[InstructionExecutor<br/>顺序执行器]
        A8[AudioPlayer]
    end

    subgraph SpringBoot
        B1[WebSocket服务端]
        B2[Android接收队列<br/>容量32]
        B3[消息路由器]
        B4[Android发送队列<br/>容量32]
        B5[RK接收队列<br/>容量32]
        B6[RK发送队列<br/>容量32]
        B7[连接管理器]
        B8[Agent/聊天服务]
        B9[控制转发服务]
        B10[STT/VL会话协调器]
        B11[LLM/TTS服务调用]
        B12[文本解析与批量下发]
        B13[指令追踪器]
    end

    subgraph "RK设备（RK下层）"
        C1[发送队列<br/>容量32]
        C2[WebSocket客户端]
        C3[接收队列<br/>容量32]
        C4[命令执行器]
        C5[状态采集器]
    end

    subgraph 外部服务
        STT[STT服务]
        VL[VL服务]
        LLM[LLM服务]
        TTS[TTS服务]
    end

    subgraph 流媒体
        UDP[UDP Server]
        RTMP[Nginx-RTMP]
    end

    %% Android → SpringBoot 消息
    A1 -->|agent_update / chat_message_send / control_command_an / stt_start / stt_audio_data / stt_end| A2
    A2 --> A3
    A3 -->|WebSocket| B1
    B1 --> B2
    B2 --> B3

    %% SpringBoot 内部路由
    B3 -->|agent/chat| B8
    B3 -->|control| B9
    B3 -->|stt| B10
    B3 -->|instruction_list：Android/RK→SB instruction_result| B13

    %% 外部服务调用
    B10 -->|音频流| STT
    STT -->|碎片| B10
    B10 -->|最终文本| B11
    B10 -->|视频帧| VL
    VL -->|结果| B10
    B11 -->|上下文| LLM
    LLM -->|整体输出| B12
    B12 -->|逐条合成或拼装| TTS
    TTS -->|嵌入 instruction 项| B12
    B12 -->|instruction_list| B4

    %% 控制命令转发
    B9 -->|control_command_sb| B6
    B6 --> B1
    B1 -->|WebSocket：control / instruction_list 等| C2

    %% RK 处理
    C2 --> C3
    C3 --> C4
    C3 --> C5
    C4 -->|command_result| C1
    C5 -->|rk_status| C1
    C1 --> C2
    C2 -->|WebSocket| B1
    B1 --> B5
    B5 --> B3
    B3 -->|command_result / rk_status| B4

    %% SpringBoot → Android 推送
    B4 --> B1
    B1 -->|WebSocket| A3
    A3 --> A4
    A4 --> A5
    A5 -->|agent_list_sync / chat_message_sync / control_response / rk_status / tts_data / instruction_list / system_message| A6
    A5 -->|instruction_list| A7
    A5 -->|tts_data 流式非批量| A6
    A7 -->|逐条执行| A8
    A8 -->|播放完成| A7
    A7 -->|instruction_result channel:instruction_list| A2

    %% 视频流独立通道
    Android -.->|UDP/RTMP| UDP
    Android -.->|UDP/RTMP| RTMP
    RK -.->|UDP/RTMP| UDP
    RK -.->|UDP/RTMP| RTMP
    UDP -.-> B10
    RTMP -.-> B10
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


三端消息体复用同一套字段语义时，以 **`CommonResultDto`**（请求追踪 + 结果码 + 描述）、**`StreamSeqDto`**（流式分片：agent、序号、末帧、时间戳）为基类；具体 Event 的 Request/Response 再继承其一。实现位置：

**CommonResultDto**

```java
@Data
public class CommonResultDto {
    private String requestId;    // 请求追踪ID
    private Integer code;        // 200=成功
    private String message;      // 描述信息
}
```

**StreamSeqDto**

```java
@Data
public class StreamSeqDto {
    private String agentId;
    private String seq;          // 流序号
    private Boolean isLast;      // 是否最后一帧
    private Long timestamp;      // 时间戳
}
```

**与现有类型的关系（示例）**

- 流式 TTS 分片：**`TtsDataResponse` extends `StreamSeqDto`**，子类字段见 `springboot/.../response/TtsDataResponse.java`（`base64AudioStream`）；Android 同形 `TtsDataResponse.kt`。
- 控制命令载荷：**`ControlCommandResponse` extends `CommonResultDto`**，子类字段 `commandId`、`command`、`params`；Android 同形 `ControlCommandResponse.kt`。

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

| Event | 方向 | 说明 | data字段                                          |
|---------|------|------|-------------------------------------------------|
| `stt_start` | Android→SB | 开始语音识别（STT） | `agentId`                                       |
| `stt_start_ack` | SB→Android | 语音识别确认 | `agentId`                                       |
| `stt_audio_data` | Android→SB | 语音数据流 | `agentId`, `base64AudioStream`, `seq`           |
| `stt_text_data` | SB→Android | 识别的文本结果流 | `agentId`, `textStream`, `seq`                  |
| `stt_end` | Android→SB | 结束语音识别 | `agentId`                                       |
| `stt_error` | Android→SB | 语音识别异常 | `agentId`, `code`, `message`                    |
| `llm_start` | SB→Android | 开始LLM处理 | `agentId`                                       |
| `llm_data` | SB→Android | LLM文本流 | `agentId`, `textStream`, `seq`                  |
| `llm_end` | SB→Android | LLM处理结束 | `agentId`                                       |
| `llm_error` | SB→Android | LLM处理异常 | `agentId`, `code`, `message`                    |
| `tts_start` | SB→Android | 开始语音合成（TTS） | `agentId`                                       |
| `tts_data` | SB→Android | TTS音频流 | `agentId`, `base64AudioStream`, `text`, `seq`         |
| `tts_end` | SB→Android | TTS合成结束 | `agentId`                                       |
| `tts_error` | SB→Android | TTS合成异常 | `agentId`, `code`, `message`                    |
| `vl_start` | Android→SB | 开始视觉理解（VL） | `agentId`（v2版本中获取视频源的方式只有两种：Udp推流SB或SB主动拉取RTMP） |
| `vl_data` | SB→Android | 视觉理解结果 | `agentId`, `content`                            |
| `vl_end` | SB→Android | 视觉理解结束 | `agentId`                                       |
| `vl_error` | SB→Android | 视觉理解异常 | `agentId`, `code`, `message`                    |

##### 5. 控制命令类（Android ↔ SB ↔ RK）

| Event | 方向            | 说明 | data字段                                                  |
|---------|---------------|------|---------------------------------------------------------|
| `control_command_an` | Android→SB    | 控制RK设备 | `deviceId`, `command`, `params`（Map<String, String>）    |
| `control_command_sb` | SB→RK/Android | 转发控制命令 | `commandId`, `target`, `command`, `params`（Map<String, String>） |
| `command_result` | RK→SB         | 命令执行结果 | `commandId`, `code`, `message`                          |
| `control_response` | SB→Android    | 控制结果响应 | `deviceId`, `code`, `message`                           |

##### 6. 设备状态类（RK ↔ SB ↔ Android）

| Event | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `status_request` | Android/SB→RK | 请求设备状态 | `deviceId` |
| `rk_status` | RK→SB | 设备状态上报 | `deviceId`, `battery`, `position` |
| `rk_status` | SB→Android | 转发设备状态 | `deviceId`, `battery`, `position` |

##### 7. 指令批量处理类（三端通用）

**设计说明**：`instruction_list` / `instruction_result` 独占 **`instruction_list` 通道**，
由 **`InstructionListChannelHandler`（SB）** 与 **`InstructionListEventHandler`（Android / RK）** 处理，
用于把「TTS 与 MCP」绑定为**同一有序批次**，与 **`tts` 通道 + `TTSEventHandler`（流式预览）**、**`control` 通道 + `ControlEventHandler`（单条控制）** 解耦，避免两路消息在客户端自行拼时序。

| Event | Channel | 方向 | 说明 | data字段 |
|--------------------|-------------|------|----------------------------|----------|
| `instruction_list`   | `instruction_list` | SB → Android / RK（及需同步的下发面） | 一次下发完整有序列表（TTS 片段 + MCP 项） | 见下「载荷结构」 |
| `instruction_result` | `instruction_list` | Android / RK → SB | 列表内**单条**执行结果（按 index 推进追踪） | `requestId`, `index`, `type`（`Instruction` 枚举）, `status`, `code`, `message` |

**列表项与现有 DTO 对齐规则**

| 项类型 | 基类链 | 对齐的已有类型 | 说明 |
|--------|--------|----------------|------|
| TTS | **`StreamSeqDto` ← `TtsDataResponse`** | `TtsDataResponse`（`base64AudioStream`） | 一项 = 一条可播放分片；`seq` / `isLast` 与同批内多块音频一致 |
| MCP | **`CommonResultDto` ← `ControlCommandResponse`** | `ControlCommandResponse`（`commandId`、`command`、`params`） | 一项 = 一条控制语义；`requestId`/`code`/`message` 继承自 `CommonResultDto` |

**列表项 DTO（`type` 字段使用 `Instruction`，禁止魔法字符串）**

```java
// 在 TtsDataResponse（extends StreamSeqDto，含 base64AudioStream、text）上增加列表编排字段
@Data
@EqualsAndHashCode(callSuper = true)
public class InstructionTtsItem extends TtsDataResponse {
    private Instruction type = Instruction.TTS;
    private Integer index;
}

// 在 ControlCommandResponse（extends CommonResultDto，含 commandId/command/params/target）上增加列表编排字段
@Data
@EqualsAndHashCode(callSuper = true)
public class InstructionMcpItem extends ControlCommandResponse {
    private Instruction type = Instruction.MCP;
    private Integer index;
    /** 执行侧路由（与下行 push 的 target 语义不同，见实现注释） */
    private String executionTarget;
    private String deviceId;
}
```

> 实现说明：若工程里 `InstructionMcpItem` 暂不继承 `ControlCommandResponse`（避免与 SB 下行 `target` 字段语义混用），则保持「继承 `CommonResultDto` + 与 `ControlCommandResponse` 同名字段」即可，**`type` 仍须为 `Instruction` 枚举**。

##### 8. 系统消息类（三端通用）

| Event | 方向 | 说明 | data字段 |
|---------|------|------|----------|
| `error` | 双向 | 错误信息 | `code`, `message` |
| `system_message` | SB→双向 | 系统通知 | `event`, `param`（Map<String, String>） |

---

#### Channel设计

Channel为一类Event的通道

UnifiedWsHandler + MessageRouter (Channel分发器) + ChannelHandler + 线程池架构

| Channel | 包含Event | 方向 | 说明                                               |
|---------|----------|------|--------------------------------------------------|
| connection | connect, connect_ack, rk_connect, rk_connect_ack, ping, pong | 三端双向 | 连接管理、心跳保活                                        |
| agent | agent_list_sync, agent_update | Android ↔ SB | Agent配置同步                                        |
| chat | chat_message_sync, chat_message_send | Android ↔ SB | 聊天消息传输                                           |
| stt | stt_start, stt_start_ack, stt_audio_data, stt_text_data, stt_end, stt_error | Android ↔ SB | 语音识别数据流                                          |
| llm | llm_start, llm_data, llm_end, llm_error | SB → Android | LLM文本流                                           |
| tts | tts_start, tts_data, tts_end, tts_error | SB → Android | TTS 音频流（**流式 / 非混合时序**；混合时序回复走 `instruction_list`） |
| instruction_list | instruction_list, instruction_result | SB ↔ Android / RK | **混合时序编排**：有序 TTS+MCP，与 `instruction_result` 成对追踪  |
| vl | vl_start, vl_data, vl_end, vl_error | Android ↔ SB | 视觉理解数据流（V2版本由UDP或RTMP替代）                         |
| control | control_command_an, control_command_sb, command_result, control_response | Android → SB → RK → SB → Android | **单条**设备控制（非混合时序 `instruction_list` 编排内）             |
| status | status_request, rk_status | RK ↔ SB ↔ Android | 设备状态上报与查询                                        |
| system | error, system_message | 三端双向 | 系统消息与错误通知                                        |

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
        -Map~String, ChannelHandler~ handlers
        -ExecutorService businessExecutor
        -ExecutorService sttExecutor
        -ExecutorService controlExecutor
        -ExecutorService instructionListExecutor
        -ExecutorService llmExecutor
        -ExecutorService ttsExecutor
        -ExecutorService vlExecutor
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
        +getSupportedChannel() String
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
        -SessionCoordinator sessionCoordinator
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

    class InstructionListChannelHandler {
        -InstructionTracker instructionTracker
        -BatchDispatchService batchDispatchService
        -ConnectionManager connectionManager
        +handle(session, event)
        -handleInstructionResult(session, event)
        -onAllInstructionsDone(requestId)
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
    ChannelHandler <|.. InstructionListChannelHandler
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
    D -->|instruction_list/instruction_result| IL[instructionList 线程池]
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

    subgraph instListExecPool [指令批量线程池 - 核心2/最大4]
        IL --> QIL[InstructionListChannelHandler]
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
    QIL --> ABIL[InstructionTracker + BatchDispatch回写]
    R --> AC[SttService + SessionCoordinator]
    S --> AD[LlmService + 流式处理]
    T --> AE[TtsService + 音频合成]
    U --> AF[VlService + 视觉理解]

    Y --> AG[AndroidMessageQueue]
    Z --> AG
    AA --> AG
    QIL --> AG
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
        +instructionListExecutor() ExecutorService
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
        -ExecutorService instructionListExecutor
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

    subgraph InstructionListThreadPool [指令批量线程池]
        H1[corePoolSize: 2]
        H2[maxPoolSize: 4]
        H3[queueCapacity: 200]
        H4[keepAliveTime: 60s]
        H5[rejectPolicy: CallerRunsPolicy]
        H6[threadName: inst-list-%d]
    end

    A --> BusinessThreadPool
    A --> ControlThreadPool
    A --> SttThreadPool
    A --> LlmThreadPool
    A --> TtsThreadPool
    A --> VlThreadPool
    A --> InstructionListThreadPool
```


#### Android 泳道图（Swimlane）

```mermaid
flowchart TB
    subgraph Main [Dispatchers.Main - UI线程]
        direction TB
        M1[UI更新<br/>LiveData/Flow collect]
        M2[状态变化<br/>isAgentReplying]
        M3[播放器回调<br/>AudioPlayer completion]
        M4[视频渲染<br/>SurfaceView更新]
    end

    subgraph Default [Dispatchers.Default - CPU密集型]
        direction TB
        D1[消息分发器<br/>接收Channel消费<br/>含 InstructionListEventHandler]
        D2[JSON解析]
        D3[指令解析]
        D4[视频解码]
    end

    subgraph IO [Dispatchers.IO - 网络/文件]
        direction TB
        I1[WebSocket连接<br/>发送/接收]
        I2[HTTP请求<br/>API调用]
        I3[文件读写<br/>缓存]
        I4[UDP/RTMP推流<br/>视频发送]
    end

    subgraph Single [单线程协程 - 顺序执行器]
        direction TB
        S1[InstructionExecutor<br/>单线程上下文<br/>Dispatchers.Main.immediate]
        S2[TTS播放队列]
        S3[MCP指令队列]
    end

    Main --> Default
    Main --> IO
    Main --> Single
    Default --> IO
    Single --> Main
```

#### 异步处理层次设计

```mermaid
gantt
    title SpringBoot WebSocket消息处理时序图
    dateFormat HH:mm:ss.SSS
    axisFormat %H:%M:%S

    section WebSocket IO线程
        接收消息 :t0, 00:00:00.000, 1ms
        JSON解析与路由分发 :t1, after t0, 2ms

    section 业务线程池
        ConnectHandler处理 :t2, after t1, 30ms
        AgentHandler处理 :t3, after t1, 80ms
        ChatHandler处理 :t4, after t1, 60ms
        StatusHandler处理 :t5, after t1, 40ms

    section 控制线程池
        ControlHandler处理 :t6, after t1, 20ms
        转发指令给RK :t7, after t6, 15ms
        等待command_result :t8, after t7, 200ms
        返回control_response :t9, after t8, 10ms

    section STT线程池
        STT开始（创建会话） :t10, after t1, 5ms
        音频流循环处理 :t11, after t10, 800ms
        STT结束（获取最终文本） :t12, after t11, 20ms

    section LLM线程池
        LLM请求开始 :t13, after t12, 5ms
        LLM流式处理（非流式实际等待） :t14, after t13, 2500ms
        LLM结束（获取整体输出） :t15, after t14, 10ms

    section 文本解析与批量下发
        文本过滤解析 :t16, after t15, 50ms
        发送agent_start_reply :t17, after t16, 10ms
        批量下发instruction_list :t18, after t17, 20ms

    section TTS线程池
        TTS请求句子1 :t19, after t16, 10ms
        TTS音频合成1 :t20, after t19, 1500ms
        TTS请求句子2 :t21, after t20, 10ms
        TTS音频合成2 :t22, after t21, 1200ms

    section 指令追踪与发送队列
        入队Android消息 :t23, after t18, 5ms
        出队发送给Android :t24, after t23, 10ms
        等待instruction_result :t25, after t24, 3000ms
        全部完成发送agent_end_reply :t26, after t25, 10ms
```


#### RK 下层（C++）线程池设计

RK上层的Android已经完成了绝大部分任务，下层只需要GPIO，视频流采集，MCP执行，音频播放等

```mermaid
flowchart TB
    subgraph "主线程 / 控制线程"
        A[WebSocket 接收线程] --> B[命令解析器]
        B --> C[任务队列<br/>线程安全队列]
    end

    subgraph "工作线程池 (Worker Pool)"
        D[Worker Thread 1] --> E[GPIO 控制]
        D --> F[舵机控制]
        G[Worker Thread 2] --> H[传感器读取]
        G --> I[LCD 显示]
        J[Worker Thread 3] --> K[视频采集编码]
        J --> L[UDP/RTMP 推流]
    end

    subgraph "专用线程"
        M[状态上报定时器线程] --> N[采集状态 -> 发送队列]
        O[音频播放线程] --> P[播放 TTS/提示音]
    end

    C -->|分发任务| D
    C -->|分发任务| G
    C -->|分发任务| J
    N -->|通过 WebSocket 发送| A
    P -->|独立音频输出| Q[Audio Sink]
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
        IL[指令批量线程池<br/>instruction_list / Tracker]
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
    A -->|3. 快速分发| IL
    A -->|4. 快速分发| D
    A -->|5. 快速分发| E
    A -->|6. 快速分发| F
    A -->|7. 快速分发| G

    B -->|共享| H
    C -->|共享| H
    IL -->|共享| H
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
    IL -.-> K
    D -.-> K
    E -.-> K
    F -.-> K
    G -.-> K
```



### 状态机设计

#### Android（App，RK上层）状态机状态图

```mermaid
stateDiagram-v2
    [*] --> 空闲状态

    空闲状态 --> 唤醒中: 讯飞唤醒SDK触发
    唤醒中 --> 录音等待: 唤醒成功
    录音等待 --> 空闲状态: 2s无语音超时

    state 录音等待 {
        [*] --> 启动2s定时器
        启动2s定时器 --> 等待语音
    }

    录音等待 --> 录音中: 2s内检测到语音

    state 录音中 {
        [*] --> 音频推流
        音频推流 --> 视频推流
        视频推流 --> STT碎片展示
        STT碎片展示 --> 音频推流
    }

    录音中 --> 等待服务器处理: VAD检测说话停止

    state 等待服务器处理 {
        [*] --> 等待STT最终结果
        等待STT最终结果 --> 等待VL结果
        等待VL结果 --> 等待LLM
        等待LLM --> 等待指令列表
    }

    等待服务器处理 --> Agent回复中: 收到 instruction_list

    state Agent回复中 {
        [*] --> 播放TTS
        播放TTS --> 执行MCP指令: TTS播放完成
        执行MCP指令 --> 播放TTS: 指令执行完成且还有下一句
        执行MCP指令 --> [*]: 所有指令完成
    }

    Agent回复中 --> 空闲状态: 收到 agent_end_reply<br/>恢复唤醒+录音

    %% 异常与中断
    录音中 --> 空闲状态: 异常发生
    等待服务器处理 --> 空闲状态: 超时/异常
    Agent回复中 --> 空闲状态: 异常/手动中断
```

#### SpringBoot 状态机状态图

```mermaid
stateDiagram-v2
    [*] --> Idle

    Idle --> WaitingSTT: 收到 stt_start

    WaitingSTT --> WaitingVL: 收到 stt_end
    WaitingSTT --> WaitingVL: 超时（仅STT）

    WaitingVL --> Syncing: 收到 vl_end
    WaitingVL --> Syncing: 超时（仅VL）

    state Syncing {
        [*] --> Merging
        Merging --> CallLLM: STT和VL都到达 或 超时
    }

    CallLLM --> WaitingLLM: 调用LLM服务

    WaitingLLM --> TextParsing: 收到 LLM 整体输出

    state TextParsing {
        [*] --> Parsing
        Parsing --> Dispatching: 解析完成（生成指令列表）
    }

    Dispatching --> Idle: 批量下发完成（发送 agent_end_reply）

    %% 异常处理
    WaitingSTT --> Idle: 异常/超时
    WaitingVL --> Idle: 异常/超时
    CallLLM --> Idle: 调用失败
    WaitingLLM --> Idle: 超时/LLM异常
    TextParsing --> Idle: 解析失败
    Dispatching --> Idle: 发送失败/连接断开
```

#### RK 下层 Cpp 状态机状态图

```mermaid
stateDiagram-v2
    [*] --> 空闲

    空闲 --> 连接中: 启动/网络恢复
    连接中 --> 空闲: 连接失败
    连接中 --> 已注册: 收到 rk_connect_ack

    已注册 --> 空闲: WebSocket断开

    state 已注册 {
        [*] --> 等待指令
        等待指令 --> 执行指令: 收到 control_command_sb
        等待指令 --> 批量编排中: 收到 instruction_list
        批量编排中 --> 批量编排中: 顺序执行下一项并发送 instruction_result
        批量编排中 --> 等待指令: 本批全部完成
        执行指令 --> 等待指令: 指令完成并发送 command_result
        执行指令 --> 执行指令: 多指令并发（不同硬件）
    }

    已注册 --> 状态上报中: 定时/中断触发
    状态上报中 --> 已注册: 上报完成

    %% 异常与恢复
    执行指令 --> 空闲: 指令超时/硬件错误
    状态上报中 --> 空闲: 发送失败
    已注册 --> 连接中: 心跳超时
```






















