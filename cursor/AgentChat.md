# AgentChat设计



### 消息记录查看页面

取消聊天功能：右滑查看具体聊天记录和指令信息，不能发送消息。
取消二分插入：直接使用Java的Sort工具类。

### Android（App，RK通用）语音聊天初步设计

* 讯飞唤醒SDK唤醒->
* 定时任务，2s之后启动VAD语音活动检测（Android2s没检测到语音则认为2s内说完了，停止录音。否则开始VAD检测，直到检测从说话变为停止，则结束录音）->
* Android2s到录音结束期间会RTMP推流或者UDP发送视频帧，
    * UDP：SpringBoot转发给VL模型识别视觉内容，并转发给其他接收的Android设备。
    * RTMP：SpringBoot从Nginx流媒体服务器拉流，转发给VL。SpringBoot将流媒体rtmp的url转发其他Android设备其他Android设备直接从Nginx拉流。
* Android开始录音并将音频流通过WS将Base64或者Byte流（两种方式：音频流Base64放在JSON中，Byte音频流头部尾部加上协议帧）发送给服务器->
* SpringBoot将Base64转为Byte流或者将Android的Byte流拆帧去尾实时发送给远端STT模型->
* 远端STT模型恢复识别碎片给SpringBoot，SpringBoot存储一份在本地，其他碎片全部转发Android（Android UI展示）->
* Android结束录音 ->
* SpringBoot等待远端VL视觉理解结果 ->
* 同步(STT的最终结果 + VL的结果) -> 一同发送给远端LLM -> LLM产生整体输出不流式（因为TTS需要整段话而不是碎片流）（内部包含文本和指令）->
* 文本交给text过滤分析器（句子 + MCP指令 + 句子 + MCP指令的List格式） ->
* 给Android和RK设置为`Agent开始回复`，此时关闭唤醒词唤醒功能；禁用录音
* A：句子交给远端TTS模型 ->
* B：获得的音频流交给Android播放
* C：遇到MCP指令 -> 区分发送给Android/RK -> 检测是否结束
* 递归回调ABC直到结束 -> 
* 给Android和RK设置为`Agent结束回复`，恢复唤醒词唤醒功能。



根据我写的绘制：活动图，通信图，时序图，甘特图，状态图



### 语音视频聊天 UML图设计


活动图
```mermaid
flowchart TD
    subgraph 语音聊天活动图
        Start([用户语音交互]) --> WakeUp[讯飞唤醒SDK唤醒]
        WakeUp --> StartRecord[开始录音]
        
        %% 录音与流媒体并行
        StartRecord --> ParallelStart{并行执行}
        
        ParallelStart --> AudioStreamProcess[录音与流媒体模块<br/>━━━━━━━━━━━━━━━<br/>• 持续发送音频流WS→STT碎片→Android展示<br/>• 持续发送视频帧RTMP/UDP<br/>• VAD检测循环，条件：说话未停止且未超时]
        
        ParallelStart --> TimeoutMonitor[2s超时监控]
        
        AudioStreamProcess -->|VAD检测到说话停止或2s超时| StopRecord[停止录音，停止所有流媒体]
        TimeoutMonitor -->|2s内无语音| StopRecord
        
        StopRecord --> SyncWait[同步等待<br/>━━━━━━━━━━━━━━━<br/>等待VL结果 + STT最终结果<br/>（含超时异常处理）]
        
        SyncWait -->|两者都到达| SendLLM[发送LLM]
        
        SendLLM --> LLMOutput[LLM整体输出<br/>文本+指令]
        LLMOutput --> ParseFilter[文本过滤分析器<br/>解析句子+MCP指令List]
        
        ParseFilter --> SetSpeaking[设置Agent开始回复<br/>关闭唤醒/禁用录音]
        
        SetSpeaking --> ProcessList[处理指令列表<br/>━━━━━━━━━━━━━━━<br/>通过消息队列接收<br/>逐条执行，等待前一条完成]
        
        ProcessList -->|句子| SendTTS[发送TTS]
        SendTTS --> AudioPlay[Android播放音频流]
        AudioPlay -->|播放完成| ProcessList
        
        ProcessList -->|MCP指令| CheckTarget{目标设备}
        CheckTarget -->|Android指令| SendAndroid[发送Android指令]
        CheckTarget -->|RK指令| SendRK[发送RK指令]
        
        SendAndroid --> WaitAndroid[等待Android执行完成]
        SendRK --> WaitRK[等待RK执行完成<br/>（RK的语音输出也由Android播放）]
        
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
    A7[指令队列执行器<br/>逐条执行List]
    D1[指令解析与路由]
  end

  subgraph RK下层
    R1[指令队列执行器<br/>逐条执行List]
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
    B6[文本解析器<br/>句子+MCP指令List]
    B7[批量指令下发器<br/>一次性发送完整List]
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

%% 解析与批量下发
  B6 -->|句子列表| C5
  B6 -->|MCP指令List| B7

%% 批量发送完整List
  B7 -->|完整List批量下发| D1

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
  LLM-->>SB: LLM整体输出（文本 + MCP指令List）

  SB->>SB: 文本过滤解析器（解析句子+MCP指令List）

  SB-->>Android: 设置Agent开始回复（关闭唤醒/禁用录音）
  SB-->>RK: 设置Agent开始回复（关闭唤醒）

  Note over SB: 批量下发完整指令List

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
    
    录音中 --> 等待VL: VAD检测说话停止
    
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
        [*] --> 禁用唤醒
        禁用唤醒 --> 禁用录音
        
        state 播放TTS {
            [*] --> 句子播放
            句子播放 --> 指令执行
            指令执行 --> 句子播放
        }
        
        禁用录音 --> 播放TTS
        播放TTS --> 检测结束
        检测结束 --> 播放TTS: 还有内容
        检测结束 --> [*]: 内容结束
    }
    
    Agent回复中 --> 空闲状态: 恢复唤醒/录音功能
```











