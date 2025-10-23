**Bug 记录**
====

## 待优化

全流式碎片发送导致文本可视性差, 改为使用整句发送接收
检查聊天记录顺序, 聊天记录顺序可能存在问题

Agent的设定改变: 新增Agent详情页面, 可以编辑Agent的设定

(*)minio存储文件异常 -> 完成minio文件存储以及资源反向代理 -> url展示
(*)Android View展示: CallDialog数据不全

## 待解决

语音的延迟调用: 1.语音真实效果, 2.批量转换 3. 触发API请求频繁
messageId主键插入异常: 重复主键Id
Android端异常断开, Spring端会抛出异常
SpringAlibaba AI长时间不说话会连接超时, 需要检查WebSocket Client解决超时问题:
```text
2025-10-23T21:52:34.938+08:00 ERROR 30184 --- [open-api] [ctor-http-nio-3] o.s.ai.chat.model.MessageAggregator      : Aggregation Error

org.springframework.web.reactive.function.client.WebClientRequestException: Connection reset
	at org.springframework.web.reactive.function.client.ExchangeFunctions$DefaultExchangeFunction.lambda$wrapException$9(ExchangeFunctions.java:137) ~[spring-webflux-6.2.0.jar:6.2.0]
	Suppressed: reactor.core.publisher.FluxOnAssembly$OnAssemblyException: 
Error has been observed at the following site(s):
	*__checkpoint ⇢ Request to POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation [DefaultWebClient]
Original Stack Trace:
		at org.springframework.web.reactive.function.client.ExchangeFunctions$DefaultExchangeFunction.lambda$wrapException$9(ExchangeFunctions.java:137) ~[spring-webflux-6.2.0.jar:6.2.0]
		at reactor.core.publisher.MonoErrorSupplied.subscribe(MonoErrorSupplied.java:55) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.Mono.subscribe(Mono.java:4576) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxOnErrorResume$ResumeSubscriber.onError(FluxOnErrorResume.java:103) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxPeek$PeekSubscriber.onError(FluxPeek.java:222) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxPeek$PeekSubscriber.onError(FluxPeek.java:222) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxPeek$PeekSubscriber.onError(FluxPeek.java:222) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.MonoNext$NextSubscriber.onError(MonoNext.java:93) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.MonoFlatMapMany$FlatMapManyMain.onError(MonoFlatMapMany.java:205) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.SerializedSubscriber.onError(SerializedSubscriber.java:124) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxRetryWhen$RetryWhenMainSubscriber.whenError(FluxRetryWhen.java:229) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxRetryWhen$RetryWhenOtherSubscriber.onError(FluxRetryWhen.java:279) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onError(FluxContextWrite.java:121) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxConcatMapNoPrefetch$FluxConcatMapNoPrefetchSubscriber.maybeOnError(FluxConcatMapNoPrefetch.java:327) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxConcatMapNoPrefetch$FluxConcatMapNoPrefetchSubscriber.onNext(FluxConcatMapNoPrefetch.java:212) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.onNext(FluxContextWrite.java:107) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.SinkManyEmitterProcessor.drain(SinkManyEmitterProcessor.java:476) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.SinkManyEmitterProcessor$EmitterInner.drainParent(SinkManyEmitterProcessor.java:620) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxPublish$PubSubInner.request(FluxPublish.java:874) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.request(FluxContextWrite.java:136) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxConcatMapNoPrefetch$FluxConcatMapNoPrefetchSubscriber.request(FluxConcatMapNoPrefetch.java:337) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxContextWrite$ContextWriteSubscriber.request(FluxContextWrite.java:136) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.Operators$DeferredSubscription.request(Operators.java:1743) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.FluxRetryWhen$RetryWhenMainSubscriber.onError(FluxRetryWhen.java:196) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.core.publisher.MonoCreate$DefaultMonoSink.error(MonoCreate.java:205) ~[reactor-core-3.7.0.jar:3.7.0]
		at reactor.netty.http.client.HttpClientConnect$HttpObserver.onUncaughtException(HttpClientConnect.java:393) ~[reactor-netty-http-1.2.0.jar:1.2.0]
		at reactor.netty.ReactorNetty$CompositeConnectionObserver.onUncaughtException(ReactorNetty.java:709) ~[reactor-netty-core-1.2.0.jar:1.2.0]
		at reactor.netty.resources.DefaultPooledConnectionProvider$DisposableAcquire.onUncaughtException(DefaultPooledConnectionProvider.java:225) ~[reactor-netty-core-1.2.0.jar:1.2.0]
		at reactor.netty.resources.DefaultPooledConnectionProvider$PooledConnection.onUncaughtException(DefaultPooledConnectionProvider.java:478) ~[reactor-netty-core-1.2.0.jar:1.2.0]
		at reactor.netty.channel.FluxReceive.drainReceiver(FluxReceive.java:245) ~[reactor-netty-core-1.2.0.jar:1.2.0]
		at reactor.netty.channel.FluxReceive.onInboundError(FluxReceive.java:466) ~[reactor-netty-core-1.2.0.jar:1.2.0]
		at reactor.netty.channel.ChannelOperations.onInboundError(ChannelOperations.java:526) ~[reactor-netty-core-1.2.0.jar:1.2.0]
		at reactor.netty.channel.ChannelOperationsHandler.exceptionCaught(ChannelOperationsHandler.java:153) ~[reactor-netty-core-1.2.0.jar:1.2.0]
		at io.netty.channel.AbstractChannelHandlerContext.invokeExceptionCaught(AbstractChannelHandlerContext.java:346) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeExceptionCaught(AbstractChannelHandlerContext.java:325) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.AbstractChannelHandlerContext.fireExceptionCaught(AbstractChannelHandlerContext.java:317) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.CombinedChannelDuplexHandler$DelegatingChannelHandlerContext.fireExceptionCaught(CombinedChannelDuplexHandler.java:424) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.ChannelHandlerAdapter.exceptionCaught(ChannelHandlerAdapter.java:92) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.CombinedChannelDuplexHandler$1.fireExceptionCaught(CombinedChannelDuplexHandler.java:145) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.ChannelInboundHandlerAdapter.exceptionCaught(ChannelInboundHandlerAdapter.java:143) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.CombinedChannelDuplexHandler.exceptionCaught(CombinedChannelDuplexHandler.java:231) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeExceptionCaught(AbstractChannelHandlerContext.java:346) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeExceptionCaught(AbstractChannelHandlerContext.java:325) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.AbstractChannelHandlerContext.fireExceptionCaught(AbstractChannelHandlerContext.java:317) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.handler.ssl.SslHandler.exceptionCaught(SslHandler.java:1210) ~[netty-handler-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeExceptionCaught(AbstractChannelHandlerContext.java:346) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeExceptionCaught(AbstractChannelHandlerContext.java:325) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.AbstractChannelHandlerContext.fireExceptionCaught(AbstractChannelHandlerContext.java:317) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.DefaultChannelPipeline$HeadContext.exceptionCaught(DefaultChannelPipeline.java:1324) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeExceptionCaught(AbstractChannelHandlerContext.java:346) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.AbstractChannelHandlerContext.invokeExceptionCaught(AbstractChannelHandlerContext.java:325) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.DefaultChannelPipeline.fireExceptionCaught(DefaultChannelPipeline.java:856) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.handleReadException(AbstractNioByteChannel.java:125) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:177) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:788) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:724) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:650) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:562) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997) ~[netty-common-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74) ~[netty-common-4.1.115.Final.jar:4.1.115.Final]
		at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30) ~[netty-common-4.1.115.Final.jar:4.1.115.Final]
		at java.base/java.lang.Thread.run(Thread.java:1583) ~[na:na]
Caused by: java.net.SocketException: Connection reset
	at java.base/sun.nio.ch.SocketChannelImpl.throwConnectionReset(SocketChannelImpl.java:401) ~[na:na]
	at java.base/sun.nio.ch.SocketChannelImpl.read(SocketChannelImpl.java:434) ~[na:na]
	at io.netty.buffer.PooledByteBuf.setBytes(PooledByteBuf.java:255) ~[netty-buffer-4.1.115.Final.jar:4.1.115.Final]
	at io.netty.buffer.AbstractByteBuf.writeBytes(AbstractByteBuf.java:1132) ~[netty-buffer-4.1.115.Final.jar:4.1.115.Final]
	at io.netty.channel.socket.nio.NioSocketChannel.doReadBytes(NioSocketChannel.java:356) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
	at io.netty.channel.nio.AbstractNioByteChannel$NioByteUnsafe.read(AbstractNioByteChannel.java:151) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
	at io.netty.channel.nio.NioEventLoop.processSelectedKey(NioEventLoop.java:788) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
	at io.netty.channel.nio.NioEventLoop.processSelectedKeysOptimized(NioEventLoop.java:724) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
	at io.netty.channel.nio.NioEventLoop.processSelectedKeys(NioEventLoop.java:650) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
	at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:562) ~[netty-transport-4.1.115.Final.jar:4.1.115.Final]
	at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997) ~[netty-common-4.1.115.Final.jar:4.1.115.Final]
	at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74) ~[netty-common-4.1.115.Final.jar:4.1.115.Final]
	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30) ~[netty-common-4.1.115.Final.jar:4.1.115.Final]
	at java.base/java.lang.Thread.run(Thread.java:1583) ~[na:na]
```

## 已解决