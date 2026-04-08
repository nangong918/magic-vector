# AgentChat设计



### 消息记录查看页面

取消聊天功能：右滑查看具体聊天记录和指令信息，不能发送消息。
取消二分插入：直接使用Java的Sort工具类。

### Android（App，RK通用）语音聊天初步设计

- 讯飞离线唤醒SDK唤醒
- 开始录音，同时启动2s定时器
- 2s后启动VAD语音活动检测：
  - 若2s内未检测到语音 → 停止录音（认为用户已说完）
  - 若2s内检测到语音 → 进入VAD循环检测，直到检测到说话停止 → 停止录音
- 录音期间并行执行以下操作：
  - 音频流通过WS（Base64/Byte流）发送给SpringBoot → SpringBoot实时转发给STT服务 → STT碎片实时返回 → SpringBoot转发给Android进行UI展示
  - 视频流通过RTMP/UDP推流：
    - UDP：SpringBoot转发给VL模型识别视觉内容，并转发给其他接收端
    - RTMP：推流到Nginx-RTMP服务器 → SpringBoot拉流转发给VL → SpringBoot将RTMP URL转发给其他Android设备，其他设备直接从Nginx拉流
- 停止录音后：
  - SpringBoot等待VL识别结果
  - 同步（STT最终结果 + VL结果）→ 发送给LLM
  - LLM产生整体输出（非流式，包含文本 + MCP事件）
- 文本交给文本过滤分析器，生成 `tts_event` 与 `mcp_event`
- 给Android和RK设置 `Agent开始回复`：关闭唤醒词唤醒功能 + 禁用录音
- 服务端碎片化下发 `tts_event` / `mcp_event`
- 客户端按 `instructionTiming + index` 排序执行：
  - 句子：发送给TTS服务 → 获取音频流 → Android播放
  - MCP指令：区分目标设备（Android/RK）→ 发送执行 → 等待执行完成
- 全部指令执行完成后 → 给Android和RK设置 `Agent结束回复`：恢复唤醒词唤醒功能 + 恢复录音功能
- 异常处理：任何环节发生异常，均恢复到空闲状态（恢复唤醒+录音功能）




### 语音视频聊天 UML图设计


活动图
```mermaid
flowchart TD
    subgraph 语音聊天活动图
        Start([用户语音交互]) --> WakeUp[讯飞唤醒SDK唤醒]
        WakeUp --> StartRecord[开始录音]
        
        %% 录音与流媒体并行
        StartRecord --> ParallelStart{并行执行}
        
        ParallelStart --> AudioStreamProcess["录音与流媒体模块<br/>━━━━━━━━━━━━━━━<br/>• 持续发送音频流WS→STT碎片→Android展示<br/>• 持续发送视频帧RTMP/UDP<br/>• VAD检测循环，条件：说话未停止且未超时"]
        
        ParallelStart --> TimeoutMonitor[2s超时监控]
        
        AudioStreamProcess -->|VAD检测到说话停止或2s超时| StopRecord["停止录音，停止所有流媒体"]
        TimeoutMonitor -->|2s内无语音| StopRecord
        
        StopRecord --> SyncWait["同步等待<br/>━━━━━━━━━━━━━━━<br/>等待VL结果 + STT最终结果<br/>（含超时异常处理）"]
        
        SyncWait -->|两者都到达| SendLLM[发送LLM]
        
        SendLLM --> LLMOutput[LLM整体输出<br/>文本+指令]
        LLMOutput --> ParseFilter[文本过滤分析器<br/>生成 tts_event + mcp_event]
        
        ParseFilter --> SetSpeaking[设置Agent开始回复<br/>关闭唤醒/禁用录音]
        
        SetSpeaking --> ProcessList["处理事件流<br/>━━━━━━━━━━━━━━━<br/>服务端碎片化下发<br/>客户端按 instructionTiming+index 执行"]
        
        ProcessList -->|句子| SendTTS[发送TTS]
        SendTTS --> AudioPlay[Android播放音频流]
        AudioPlay -->|播放完成| ProcessList
        
        ProcessList -->|MCP指令| CheckTarget{目标设备}
        CheckTarget -->|Android指令| SendAndroid[发送Android指令]
        CheckTarget -->|RK指令| SendRK[发送RK指令]
        
        SendAndroid --> WaitAndroid[等待Android执行完成]
        SendRK --> WaitRK["等待RK执行完成<br/>（RK的语音输出也由Android播放）"]
        
        WaitAndroid --> ProcessList
        WaitRK --> ProcessList
        
        ProcessList -->|列表处理完成| SetIdle[设置Agent结束回复<br/>恢复唤醒功能]
        
        SetIdle --> End([结束])
    end
```


通信图
```mermaid
flowchart LR
  subgraph "Android(AppRK上层)"
    A1[讯飞离线唤醒SDK]
    A2[录音模块]
    A3[音频推流WS]
    A4[视频推流UDP/RTMP]
    A5[播放器]
    A6[UI文本展示]
    A7[事件执行器<br/>按 instructionTiming+index 执行]
    D1[指令解析与路由]
  end

  subgraph RK下层
    R1[事件执行器<br/>按 instructionTiming+index 执行]
    R2[GPIO控制器]
    R3[舵机]
    R4[LCD屏]
  end

  subgraph SpringBoot端
    B1[WS消息路由]
    B2[音频转发]
    B3[视频转发]
    B4[VL拉流处理]
    B5[结果同步器<br/>等待VL+STT]
    B6[文本解析器<br/>生成 tts_event + mcp_event]
    B7[事件下发器<br/>碎片化发送]
  end

  subgraph 外部服务
    C2[STT服务]
    C3[VL服务]
    C4[LLM服务]
    C5[TTS服务]
  end

  subgraph 流媒体
    E1[Nginx-RTMP]
  end

%% 唤醒后直接触发录音（不经过外部服务）
  A1 -->|唤醒事件| A2

%% 录音与流媒体
  A2 -->|原始音频| A3
  A3 -->|Base64/Byte流| B1
  B1 --> B2

  A4 -->|UDP视频帧| B3
  A4 -->|RTMP推流| E1

%% 音频处理
  B2 -->|实时音频流| C2
  C2 -->|STT碎片实时返回| B2
  B2 -.->|碎片实时转发| A6

%% 视频处理
  B3 -->|UDP视频帧| B4
  E1 -->|拉流| B4
  B4 -->|视频帧| C3
  C3 -->|VL识别结果| B5

%% STT最终结果
  B2 -->|STT最终文本| B5

%% 同步等待
  B5 -->|STT+VL同步| C4
  C4 -->|LLM整体输出| B6

%% 解析与事件下发
  B6 -->|句子列表| C5
  B6 -->|MCP事件| B7

%% 服务端碎片化发送
  B7 -->|tts_event / mcp_event| D1

%% TTS音频流
  C5 -->|音频流| A5

%% 指令解析与路由
  D1 -->|Android指令| A7
  D1 -->|RK指令| R1

%% Android逐条执行
  A7 -->|逐条执行句子| A5
  A7 -->|执行完成| A7

%% RK下层逐条执行
  R1 -->|逐条执行| R2
  R2 -->|控制| R3
  R2 -->|控制| R4
  R1 -->|执行完成| R1

%% 全部执行完成通知
  A7 -.->|全部执行完成| D1
  R1 -.->|全部执行完成| D1
  D1 -.->|全部执行完成| B7
```


时序图
```mermaid
sequenceDiagram
  participant User as 用户
  participant Android as Android
  participant RK as RK设备
  participant SB as SpringBoot
  participant STT as STT服务
  participant VL as VL服务
  participant LLM as LLM服务
  participant TTS as TTS服务

  Note over Android: 讯飞离线唤醒SDK唤醒
  Android->>Android: 开始录音，启动2s定时器+VAD检测

  par 并行处理（录音+视频+音频识别碎片）
    Android->>SB: RTMP/UDP推流视频
    SB->>VL: 转发视频帧
  and
    Android->>SB: WS音频流推送
    SB->>STT: 实时音频流
    loop 实时识别
      STT-->>SB: STT识别碎片
      SB-->>Android: 转发碎片
      Android->>User: UI实时展示识别文本
    end
  end

  Note over Android: VAD检测到说话停止 或 2s超时
  Android->>Android: 停止录音，停止推流

  SB->>STT: 请求STT最终结果
  STT-->>SB: STT最终文本

  VL-->>SB: VL识别结果

  SB->>SB: 同步等待STT+VL（含超时异常处理）
  SB->>LLM: 发送STT+VL完整上下文
  LLM-->>SB: LLM整体输出（文本 + MCP事件）

  SB->>SB: 文本过滤解析器（生成 tts_event / mcp_event）

  SB-->>Android: 设置Agent开始回复（关闭唤醒/禁用录音）
  SB-->>RK: 设置Agent开始回复（关闭唤醒）

  Note over SB: 碎片化下发 tts_event / mcp_event

  loop 逐条执行指令（前一条完成后执行下一条）
    alt 句子指令
      SB->>TTS: 发送句子文本
      TTS-->>SB: TTS音频流
      SB-->>Android: 转发TTS音频流
      Android->>User: 播放音频
    else MCP指令
      alt 目标为Android
        SB-->>Android: 发送MCP指令
        Android->>Android: 执行Android指令
      else 目标为RK
        SB-->>RK: 发送MCP指令
        RK->>RK: 逐条执行指令<br/>（GPIO/舵机/LCD屏）
      end
    end
  end

  Note over Android,RK: 全部指令执行完成

  SB-->>Android: 设置Agent结束回复（恢复唤醒）
  SB-->>RK: 设置Agent结束回复（恢复唤醒）

  Android->>User: 等待下次唤醒
```

甘特图
```mermaid
gantt
  title 语音聊天时序图
  dateFormat HH:mm:ss.SSS
  axisFormat %H:%M:%S

  section Android端（主线程-协程）
    唤醒检测(Default协程) :a1, 00:00:00.000, 500ms
    开始录音 :a2, after a1, 10ms
    设置Agent开始回复(关闭唤醒) :a3, 00:00:08.200, 50ms
    设置Agent结束回复(恢复唤醒) :a4, after a17, 50ms

  section Android端（录音线程池）
    启动VAD检测(2s后拉起) :a5, 00:00:02.000, 1ms
    录音推流音频 :a6, after a2, 4000ms
    RTMP/UDP推流视频 :a7, after a2, 4000ms
    UI展示STT碎片 :a8, 00:00:00.600, 3400ms

  section Android端（播放+指令线程池）
    TTS句子1播放 :a9, 00:00:08.600, 2000ms
    MCP A指令执行 :a10, 00:00:08.600, 100ms
    等待句子1+A完成 :a11, after a9, 0ms

    TTS句子2播放 :a12, after a11, 2000ms
    MCP B指令执行 :a13, after a11, 2600ms
    等待句子2+B完成 :a14, after a13, 0ms

    TTS句子3播放 :a15, after a14, 2200ms
    MCP C指令执行 :a16, after a14, 400ms
    等待句子3+C完成 :a17, after a15, 0ms

  section SpringBoot
    音频转发STT :b1, 00:00:00.510, 3500ms
    视频转发VL :b2, 00:00:00.510, 3500ms
    停止录音触发(VAD结束或2s超时) :b3, 00:00:04.000, 1ms
    同步等待VL+STT :b4, 00:00:04.000, 1310ms
    LLM请求 :b5, after b4, 50ms
    文本解析 :b6, after c3, 50ms

    TTS请求句子1 :b7, after b6, 10ms
    TTS流式接收句子1 :b8, after b7, 1500ms
    下发MCP A :b9, after b7, 10ms

    TTS请求句子2 :b10, after b8, 10ms
    TTS流式接收句子2 :b11, after b10, 1000ms
    下发MCP B :b12, after b10, 10ms

    TTS请求句子3 :b13, after b11, 10ms
    TTS流式接收句子3 :b14, after b13, 1200ms
    下发MCP C :b15, after b13, 10ms

  section 外部服务
    STT识别 :c1, 00:00:00.510, 4500ms
    VL理解 :c2, 00:00:00.510, 4800ms
    LLM生成 :c3, after b5, 2500ms
    TTS合成句子1 :c4, after b7, 1500ms
    TTS合成句子2 :c5, after b10, 1000ms
    TTS合成句子3 :c6, after b13, 1200ms
```


状态图



```mermaid
stateDiagram-v2
  [*] --> 空闲状态

  空闲状态 --> 唤醒中: 讯飞唤醒SDK触发
  唤醒中 --> 录音等待: 唤醒成功
  录音等待 --> 录音中: 2s内检测到语音
  录音等待 --> 空闲状态: 2s无语音超时

  state 录音中 {
    [*] --> 音频推流
    音频推流 --> 视频推流
    视频推流 --> STT碎片展示
    STT碎片展示 --> 音频推流
  }

  录音中 --> 禁用唤醒和录音: VAD检测说话停止

  禁用唤醒和录音 --> 等待VL: 已禁用

  state 等待VL {
    [*] --> 等待STT最终结果
    等待STT最终结果 --> 等待VL结果
    等待VL结果 --> 结果同步
  }

  等待VL --> LLM处理中: STT+VL同步完成

  state LLM处理中 {
    [*] --> 文本解析
    文本解析 --> 遍历队列
  }

  LLM处理中 --> Agent回复中: 开始回复

  state Agent回复中 {
    [*] --> 播放TTS

    state 播放TTS {
      [*] --> 句子播放
      句子播放 --> 指令执行
      指令执行 --> 句子播放
    }

    播放TTS --> 检测结束
    检测结束 --> 播放TTS: 还有内容
    检测结束 --> [*]: 内容结束
  }

  Agent回复中 --> 空闲状态: 恢复唤醒+录音功能

%% 异常处理
  禁用唤醒和录音 --> 空闲状态: 异常发生
  等待VL --> 空闲状态: 异常发生
  LLM处理中 --> 空闲状态: 异常发生
  Agent回复中 --> 空闲状态: 异常发生
```

### Instruction 事件流


提示词会要求Agent回复是一个list，里面包含MCP和TTS的指令。
```json
[
  {
    "chatSentence": "你好，我先执行第一阶段。",
    "instructionTiming": 0
  },
  {
    "chatSentence": "向左转。",
    "instructionTiming": 1,
    "eventList": [
      {
        "eventType": "motion",
        "event": {
          "type": "左转",
          "value": "90"
        }
      }
    ]
  }
]
```


服务端直接碎片化下发：
- `tts_event`（`tts` 通道）
- `mcp_event`（`control` 通道）

客户端（Android / RK）本地按 `instructionTiming` + `index` 排序执行。
其中 `tts_event.text` 为该音频分片对应的文本碎片（用于字幕/落库对齐）。

#### 服务端下发示例

```json
{
  "channel": "tts",
  "event": "tts_event",
  "data": {
    "requestId": "req_20260408_001",
    "agentId": "10001",
    "type": "tts",
    "index": 0,
    "instructionTiming": 0,
    "text": "你好，我先执行第一阶段。",
    "seq": "0",
    "isLast": false,
    "timestamp": 1710000000000,
    "base64AudioStream": "ejkcviifri23kwrn42njbj242524df..."
  }
}
```

```json
{
  "channel": "control",
  "event": "mcp_event",
  "data": {
    "requestId": "req_20260408_001",
    "type": "mcp",
    "index": 1,
    "instructionTiming": 0,
    "target": "rk",
    "deviceId": "rk-001",
    "commandId": "cmd_gpio_001",
    "command": "GPIO_SET",
    "params": { "pin": "1", "value": "1" }
  }
}
```

#### 客户端执行规则

- 同一 `requestId` 内：先按 `instructionTiming` 升序，再按 `index` 升序。
- 同一 `instructionTiming` 阶段内：TTS 和 MCP 可并行。
- 阶段 `N` 全部完成后，才执行阶段 `N+1`。

#### 回执（单条结果）

```json
{
  "channel": "system",
  "event": "instruction_event_result",
  "data": {
    "requestId": "req_20260408_001",
    "index": 1,
    "type": "mcp",
    "instructionTiming": 0,
    "status": "success",
    "code": 200,
    "message": "GPIO_SET done"
  }
}
```

