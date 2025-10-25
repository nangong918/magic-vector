**Bug 记录**
====

## 待优化

(!!)长时间未连接之后再次连接会发生Connect Reset，这个唤醒判断时间太长了，高达19s，需要学习SpringAI ChatClient的源码，将其超时时间改为3~5s，然后快速重试。

Agent的设定改变: 新增Agent详情页面, 可以编辑Agent的设定
Agent设定存在问题，Agent会错误的把系统级别设定认为是用户的对话内容


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

## 待定

全流式碎片发送导致文本可视性差, 改为使用整句发送接收
(*)minio存储文件异常 -> 完成minio文件存储以及资源反向代理 -> url展示
(*)Android View展示: CallDialog数据不全


