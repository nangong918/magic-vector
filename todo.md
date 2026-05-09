# todo

完成全部demo再进行设计app吧，而且完成demo之后直接交给AI完成就行了。
我需要做的事情就是完全跑通demo

## 当前计划

1. 考虑增加WebRTC双端
2. 梳理链路
3. 优化性能, FAAC软编改为MediaCodec硬编
4. 查询YUV为什么要矩阵置换，这不是很消耗CPU资源吗？
5. 学习音视频码率一共有哪些？为什么要YUV_420_888 -> I420 -> H.264
6. 确认音视频采样率
7. 学习ExoPlayer（Media3）播放器的视频流处理方式，是直接播放H.264吗？
8. ExoPlayer是怎么同时支持RTMP，RTSP，HLS的？
9. ExoPlayer怎么播放的？YUV数据好像是先转码成了H.264又加上了RTMP帧头封装了的
10. 学习RTMP帧头内容是什么？为什么这么设计？做到了音视频流媒体同步吗？
11. 音视频流媒体其实都是人定义的，根本没什么要学习的，要从各个视频场景思考为什么要定义这些
    比如说：
    稳定性：RTSP
    画质：采样率选择
    同步：RTMP封装
    实时：RTMP
    带宽与效率：编解码，压缩，IBP
12. FFmpeg做了什么，还能做什么？
    推流RTSP？
    怎么抽帧的？
    怎么HLS转码Mp4的？
13. Android的JNI层的Cpp代码利用FFmpeg，X264，RTMP做了什么事情？这些都是开源库，按理来说不用Android也能推流，从纯Cpp角度分析。
14. MediaCodec是硬编解码吗？跟X264 cpp库软编解码的区别是什么？为什么硬编解码效率高还要存在软编码？软编码不是CPU消耗更高吗？
15. 音频采集的什么内容？PCM 16bit是什么？为什么要在JNI层faac编码成AAC？
16. JNI是跨语言的，Android做流媒体音视频是不是天生比纯Cpp的QT要弱？性能要查，CPU开销要大？
17. Nginx流媒体做什么了？它还支持控制视频分辨率？
18. 拉流的原理是什么？为什么一个视频源能被那么多用户同时拉？没有带宽限制吗？CDN加速是什么？能解决什么问题？
19. TextureView和SurfaceView的详细区别？什么时候选什么？什么性能最好？
20. Android的Camera是获取YUV数据，其他的设备也是吗？比如RK3588芯片外接的USB摄像头？因为我后续需要把APK烧录到RK上，我需要确认，笔记本电脑呢？
21. 音视频流媒体的关键词：采样率，声道，格式，编解码，分辨率，码率都是什么？还有没有其他名词？
22. IBP帧分别是什么？直播为什么不用B帧？什么时候用B帧？
23. H.265什么时候使用？带来的额外开销怎么办？
24. RTMP的url：rtmp://是谁创建的？Nginx吗？不用Nginx的话可以用什么呢？
25. Nginx做了什么事情，还做实时转码多码率（ABR ladder）？还做切片分发（HLS packaging + HTTP serving）？
26. 性能瓶颈如何解决：Java 层 YUV 拷贝与旋转，x264/faac 软编，PacketQueue 未阻塞等待，RTMP 单 TCP 通道
27. 网络突然波动的降级策略？
28. 内存泄漏风险点？
29. 新增日志排查各个步骤的耗时，并优化总体性能
30. ”SIMD/NEON 对 YUV 转换有巨大加速价值“是什么意思？
31. 写新的Cursor问题分析：cursor分析RTSP推流拉流，上传视频FFmpeg抽帧，HLS播放与下载HLS并转码。SpringBoot怎么实现的？
32. 梳理Cpp的实现以及Cpp的书籍笔记记录.
33. 梳理写书并写完再上网查询别人写的笔记完善书籍
34. 优化整体性能
35. 写入麒麟灵境项目然后写入简历

## 待编排的任务
* Android的RemoteApiSource request层级结构要改成返回需要的数据类型比如Model
* Http传输图片等文件不应该展示具体内容，直接取消SkipMultipartBinaryHttpLogger改为HttpLoggingInterceptor中检查req和resp是不是json类型，不是就不输出。
* 保存下载回调未完成
* Screen、Activity职能分离：我设计Screen目的是相当于旧版Android的xml。这样就能解耦ui和业务。

* Flutter的Api应该放在ApiRequest，基本逻辑实现应该参考之前的ApiRequest。解耦AuthRemoteApiSource和OssRemoteApiSource
* SafeDioLogInterceptor需要检查跟我之前说的【HttpLoggingInterceptor中检查req和resp是不是json类型，不是就不输出】逻辑是否一样。
* 下拉刷新，数据更新UI存在问题
* OssDemoPage需要创建ViewModel，拆分UI数据流业务更新逻辑。

## 日计划及实际行动

### Demo
[计划.md](plan/计划.md)

## 月计划及实际行动




