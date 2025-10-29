**Bug 记录**
====

## 待优化

(待优化A1)长时间未连接之后再次连接会发生Connect Reset目前采用的是递归的方式重试，可能造成堆栈溢出。考虑改为循环调用的方式
Agent的设定改变: 新增Agent详情页面, 可以编辑Agent的设定
YOLOv8实现视频流目标活动检测调参 + 物品阈值增加，person阈值下降 + YOLOv8物品优先级List + 眼睛移动配合YOLOv8优先级List

## 待解决


## 已解决

语音的延迟调用: 1.语音真实效果, 2.批量转换 3. 触发API请求频繁
SpringAlibaba AI长时间不说话会连接超时, 需要检查WebSocket Client解决超时问题: （Bug仍然存在，只是添加了flux重试机制）
```text
2025-10-23T21:52:34.938+08:00 ERROR 30184 --- [open-api] [ctor-http-nio-3] o.s.ai.chat.model.MessageAggregator      : Aggregation Error

org.springframework.web.reactive.function.client.WebClientRequestException: Connection reset
	at org.springframework.web.reactive.function.client.ExchangeFunctions$DefaultExchangeFunction.lambda$wrapException$9(ExchangeFunctions.java:137) ~[spring-webflux-6.2.0.jar:6.2.0]
	Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: 
Error has been observed at the following site(s):
	*__checkpoint ⇢ Request to POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation [DefaultWebClient]
Original Stack Trace:
Caused by: java.net.SocketException: Connection reset
```
Android端异常断开, Spring端会抛出异常: (本质上是无害影响，但是还是要注意回收资源)
```text
java.io.EOFException: null
```


## 暂时未再次复现bug
messageId主键插入异常: 重复主键Id
Android 端存在问题: 后端发送EOF表示发送完成，并不代表前端播放完成。
Android端回显存在问题：转换成功之后将消息发给前端的消息是上一条内容，内容是错误的
检查聊天记录顺序, 聊天记录顺序可能存在问题 (Android端的排序问题)

Agent设定存在问题，Agent会错误的把系统级别设定认为是用户的对话内容
(!!)长时间未连接之后再次连接会发生Connect Reset，这个唤醒判断时间太长了，高达19s，需要学习SpringAI ChatClient的源码，将其超时时间改为3~5s，然后快速重试。: (通过设置timeout并捕获重新调用解决)
```java
        Flux<String> responseFlux = chatClient.prompt()
                .user(sentence)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatContextManager.agentId))
                .stream()
                .content()
                // 3500ms未响应则判定超时，进行重连尝试
                .timeout(Duration.ofMillis(ModelConstant.LLM_CONNECT_TIMEOUT_MILLIS));
```

## 待定

全流式碎片发送导致文本可视性差, 改为使用整句发送接收
(*)minio存储文件异常 -> 完成minio文件存储以及资源反向代理 -> url展示
(*)Android View展示: CallDialog数据不全

tts的文本长度限制未(0, 600]: 
```text
Exception in thread "AudioChat-3" com.alibaba.dashscope.exception.ApiException: 
{"statusCode":400,"message":"<400> InternalError.Algo.InvalidParameter: 
Range of input length should be [0, 600]","code":"InvalidParameter","isJson":true,
"requestId":"c3779a6d-0936-4ba7-9710-a1aa1007d7d2"}; 
status body:{"statusCode":400,"message":"<400> InternalError.Algo.InvalidParameter: Range of input length should be [0, 600]","code":"InvalidParameter",
"isJson":true,"requestId":"c3779a6d-0936-4ba7-9710-a1aa1007d7d2"}
```


