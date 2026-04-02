# AgentChat设计


## 描述

有一点你理解错了，我现现在这样吧。我来教你绘制Agent调用流程图：



1.纯文本聊天：
Android发送Text消息->SpringBoot
SpringBoot交给LLM模型
LLM产生StreamText
Android直接展示Text流

2.Android（App，RK通用）语音聊天：
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





### 纯文本聊天

活动图

```mermaid
flowchart TD
    subgraph 纯文本聊天活动图
        Start([用户输入文本]) --> Send[Android发送文本到SpringBoot]
        Send --> Forward[SpringBoot转发给LLM]
        Forward --> Stream[LLM产生StreamText流式输出]
        Stream --> Push[SpringBoot推送文本流]
        Push --> Display[Android实时展示文本]
        Display --> End([结束])
    end
```

通信图
```mermaid
flowchart LR
    subgraph Android端
        A1[用户输入]
        A2[UI展示]
    end

    subgraph SpringBoot端
        B1[消息路由]
        B2[LLM客户端]
    end

    subgraph 外部服务
        C1[LLM服务]
    end

    A1 -->|文本消息| B1
    B1 -->|转发| B2
    B2 -->|请求| C1
    C1 -->|StreamText流| B2
    B2 -->|流式推送| A2
```

时序图
```mermaid
sequenceDiagram
    participant User as 用户
    participant Android as Android
    participant SB as SpringBoot
    participant LLM as LLM服务

    User->>Android: 输入文本
    Android->>SB: chat_message_send
    SB->>LLM: 转发文本请求
    loop 流式输出
        LLM-->>SB: StreamText碎片
        SB-->>Android: 推送文本碎片
        Android-->>User: 实时显示
    end
    LLM-->>SB: llm_end
    SB-->>Android: 推送结束标识
```

甘特图
```mermaid
gantt
    title 纯文本聊天时序图
    dateFormat HH:mm:ss.SSS
    axisFormat %H:%M:%S
    
    section Android
    发送文本 :a1, 00:00:00.000, 50ms
    
    section SpringBoot
    转发LLM :b1, 00:00:00.050, 20ms
    
    section LLM服务
    LLM处理 :c1, 00:00:00.070, 2000ms
    
    section 流式推送
    文本碎片1 :d1, 00:00:00.500, 10ms
    文本碎片2 :d2, 00:00:01.000, 10ms
    文本碎片3 :d3, 00:00:01.500, 10ms
    文本碎片4 :d4, 00:00:02.000, 10ms
```

状态图
```mermaid
stateDiagram-v2
    [*] --> 空闲
    
    空闲 --> 等待输入: 用户打开聊天界面
    等待输入 --> 发送中: 用户输入并发送
    
    发送中 --> 流式接收中: 文本已发送
    
    state 流式接收中 {
        [*] --> 接收碎片
        接收碎片 --> 追加显示
        追加显示 --> 接收碎片: 继续接收
        接收碎片 --> [*]: 接收结束
    }
    
    流式接收中 --> 等待输入: 显示完成
    
    等待输入 --> 等待输入: 继续等待下一轮输入
```

### 语音视频聊天


活动图
```mermaid
flowchart TD
    subgraph 语音聊天活动图
        Start([用户语音交互]) --> WakeUp[讯飞唤醒SDK唤醒]
        WakeUp --> StartRecord[开始录音]
        StartRecord --> StartTimer[启动2s定时器]
        
        StartTimer --> CheckVoice{2s内检测到语音?}
        CheckVoice -->|否| StopRecord1[停止录音<br/>结束流程]
        CheckVoice -->|是| VadStart[启动VAD检测]
        
        StartRecord --> PushAudio[音频流WS发送]
        PushAudio --> STTProcess[STT实时识别]
        STTProcess --> STTResult[返回识别碎片]
        STTResult --> DisplayText[Android展示文本]
        
        VadStart --> VadDetect{VAD检测中}
        VadDetect -->|正在说话| PushVideo[RTMP/UDP推流]
        PushVideo --> VadDetect
        VadDetect -->|说话停止| StopRecord2[停止录音]
        
        StopRecord2 --> WaitVL[等待VL结果]
        WaitVL --> GetFinalSTT[获取STT最终结果]
        GetFinalSTT --> SyncResult[同步STT+VL结果]
        SyncResult --> SendLLM[发送LLM]
        
        SendLLM --> LLMOutput[LLM整体输出<br/>文本+指令]
        LLMOutput --> ParseFilter[文本过滤分析器<br/>解析句子+MCP指令List]
        
        ParseFilter --> SetSpeaking[设置Agent开始回复<br/>关闭唤醒/禁用录音]
        
        SetSpeaking --> ProcessList{遍历List}
        
        ProcessList -->|句子| SendTTS[发送TTS]
        SendTTS --> AudioPlay[Android播放音频流]
        AudioPlay --> ProcessList
        
        ProcessList -->|MCP指令| CheckTarget{目标}
        CheckTarget -->|Android| SendAndroid[发送Android指令]
        CheckTarget -->|RK| SendRK[发送RK指令]
        SendAndroid --> ProcessList
        SendRK --> ProcessList
        
        ProcessList -->|结束| SetIdle[设置Agent结束回复<br/>恢复唤醒功能]
        
        StopRecord1 --> End([结束])
        SetIdle --> End
    end
```


通信图
```mermaid
flowchart LR
    subgraph Android端
        A1[讯飞唤醒SDK]
        A2[录音模块]
        A3[音频推流WS]
        A4[视频推流UDP/RTMP]
        A5[播放器]
        A6[UI文本展示]
    end
    
    subgraph SpringBoot端
        B1[WS消息路由]
        B2[音频转发]
        B3[视频转发]
        B4[VL拉流处理]
        B5[结果同步器]
        B6[文本解析器]
        B7[指令分发器]
    end
    
    subgraph 外部服务
        C1[讯飞唤醒]
        C2[STT服务]
        C3[VL服务]
        C4[LLM服务]
        C5[TTS服务]
    end
    
    subgraph RK端
        D1[指令执行器]
    end
    
    subgraph 流媒体
        E1[Nginx-RTMP]
    end

    A1 -->|唤醒| C1
    A2 -->|音频流| A3
    A3 -->|Base64/Byte流| B1
    B1 --> B2
    B2 -->|音频| C2
    C2 -->|碎片| B2
    B2 -->|碎片| A6
    
    A4 -->|UDP| B3
    A4 -->|RTMP| E1
    B3 -->|UDP视频| B4
    E1 -->|拉流| B4
    B4 -->|视频帧| C3
    C3 -->|VL结果| B5
    
    B2 -->|最终文本| B5
    B5 -->|STT+VL| C4
    C4 -->|整体输出| B6
    B6 -->|句子| C5
    B6 -->|MCP指令| B7
    
    C5 -->|音频| A5
    B7 -->|Android指令| A6
    B7 -->|RK指令| D1
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

    Note over Android: 讯飞唤醒SDK唤醒
    Android->>Android: 启动2s定时器 + VAD检测
    
    par 并行处理
        Android->>SB: RTMP/UDP推流视频
        SB->>VL: 转发视频帧
    and
        Android->>SB: WS音频流
        SB->>STT: 实时音频
        STT-->>SB: 识别碎片
        SB-->>Android: 转发碎片
        Android->>User: UI展示文本
    end
    
    Android->>Android: 说话停止，结束录音
    SB->>STT: 获取最终结果
    STT-->>SB: 最终文本
    
    VL-->>SB: VL结果
    SB->>SB: 同步STT+VL结果
    SB->>LLM: 发送完整上下文
    LLM-->>SB: 整体输出(文本+指令)
    
    SB->>SB: 文本过滤解析
    SB->>Android: Agent开始回复(关闭唤醒)
    SB->>RK: Agent开始回复(关闭唤醒)
    
    loop 遍历句子+MCP指令
        alt 句子
            SB->>TTS: 发送句子
            TTS-->>SB: 音频流
            SB-->>Android: 音频流
            Android->>User: 播放音频
        else MCP指令
            alt 目标Android
                SB-->>Android: 发送指令
            else 目标RK
                SB-->>RK: 发送指令
                RK->>RK: 执行指令
            end
        end
    end
    
    SB->>Android: Agent结束回复(恢复唤醒)
    SB->>RK: Agent结束回复(恢复唤醒)
```

甘特图
```mermaid
gantt
    title 语音聊天时序图
    dateFormat HH:mm:ss.SSS
    axisFormat %H:%M:%S
    
    section Android端
    唤醒检测 :a1, 00:00:00.000, 500ms
    2s定时器等待 :a2, after a1, 2000ms
    VAD检测(说话中) :a3, after a1, 3000ms
    录音推流音频 :a4, after a1, 3500ms
    UI展示STT碎片 :a5, after a1, 100ms
    RTMP/UDP推流视频 :a6, after a1, 3500ms
    
    播放TTS音频 :a7, 00:00:08.000, 3000ms
    执行指令 :a8, 00:00:11.000, 500ms
    
    section SpringBoot
    音频转发STT :b1, 00:00:00.500, 3500ms
    视频转发VL :b2, 00:00:00.500, 3500ms
    等待VL结果 :b3, after b2, 1500ms
    同步STT+VL :b4, 00:00:04.000, 100ms
    LLM处理 :b5, after b4, 2000ms
    文本解析 :b6, after b5, 100ms
    TTS合成 :b7, after b6, 1500ms
    指令分发 :b8, after b7, 500ms
    
    section 外部服务
    STT识别 :c1, 00:00:00.500, 3500ms
    VL理解 :c2, 00:00:00.500, 4000ms
    LLM生成 :c3, 00:00:04.100, 2000ms
    TTS合成 :c4, 00:00:06.200, 1500ms
    
    section RK端
    执行指令 :d1, 00:00:11.000, 500ms
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











