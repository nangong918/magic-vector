# Cursor Question 2




### 梳理Android音频、视频采集逻辑

#### 问题
帮我梳理一下Android音频、视频采集逻辑。
[LivePushDemoActivity.java](../demo/flutter/flutteraar/app/src/main/java/com/example/flutteraar/ui/activity/LivePushDemoActivity.java)
这个Activity，帮我梳理一下是怎么采集视频是音频的。
预览View是怎么实现的？TextureView？SurfeceView？用的哪个为什么这么用？性能怎样？
采集的数据源是什么格式？YUV？通过什么转化成了什么？H.264？为什么要转化？为什么不继续转化为H.265？
传输是怎么传输的？RTMP？为什么用这个协议，内部有封装IBP帧吗？为什么封装或者没有封装（必要性）
RTMP推流用的是Nginx[nginx.conf](../demo/springboot/nginx-docker/conf/nginx.conf)
分析一下流媒体服务器做些什么？
然后接收的在[LivePullDemoActivity.java](../demo/flutter/flutteraar/app/src/main/java/com/example/flutteraar/ui/activity/LivePullDemoActivity.java)
看看是怎么播放的？直接播放H.264还是转码播放了？用什么播放的？内部播放原理是什么？有FFmpeg参与吗？参与的智能是什么？

#### 逻辑梳理与绘图
绘制出整个采集，预览，转码封装，推流，转发，拉流，解码，播放的全流程逻辑的活动图 和 数据的通信图


#### 优化设计
思考现在架构性能如何？如果让你来优化你准备怎样优化。
流媒体最注重的就是性能和内存，你看看是否性能瓶颈点和内存泄漏风险？

#### 技术与理论问题
本次涉及到的流媒体知识有哪些？梳理出来，包括RTMP，H.264，HLS，编解码，FFmpeg等
本地涉及到的计算机考研408知识有哪些？包括数据结构与算法分析，操作系统，计算机网络，计算机组成原理。详细分析；
我梳理几个我想到的点：
- IBP帧这样设计是为了提高传输效率，网络上是怎么传输的，编解码与展示是怎么实现的？（操作系统，计算机组成原理，计算机网络）
- 声道和视频画面源是怎么做到一致的？
- 采样率，码率，编解码是怎么影响到CPU性能的（计算机组成原理）
- RTMP底层是基于什么？TCP还是UDP？协议是在计算机网络哪一层？跟Http传输有什么区别？为什么不直接用UDP快速传输？
- YUV格式为什么不能直接传输需要转码H.264甚至H.265？
- 整个系统是否有性能优化点？瓶颈会在哪里？推流？转码？Nginx转推？多用户同时拉流的带宽？（计算机网络，计算机组成原理）
- 线程池是怎样设计的？是否能让推流预览编解码最优？（操作系统）
等等还有好多问题，你也可以提出很多然后解答，我希望你多提出一些以便我学习408

回复的内容都写在[CursorMedia.md](media/CursorMedia.md)



### 帮我梳理RTMP和RTSP封包组帧逻辑

看到这个代码:
[LivePushDemoActivity.java](../demo/flutter/flutteraar/app/src/main/java/com/example/flutteraar/ui/activity/LivePushDemoActivity.java)
[LiveRtspFilePushDemoActivity.java](../demo/flutter/flutteraar/app/src/main/java/com/example/flutteraar/ui/activity/LiveRtspFilePushDemoActivity.java)
这两个分别是RTMP直播推流和RTSP文件推流,
我现在需要你帮我梳理出来非cpp lib代码中(就是不是RTMP,RTSP,FFmpeg这种官方代码,而是本项目自己开发的代码)中的组帧封包逻辑
是怎么将H.264和AAC封装包组帧发送的, 帮我梳理Cpp代码, 绘制出Mermaid格式的逻辑活动图和数据流通信图
需要写在C:\GoBackPlan\Knowledge\Media\Media.md
这个是我自己整理的流媒体资料, 我已经划分了大致框架, 你大概要卸载我里面规定的指定位置.
如果你有余力就帮我完成#### IBP帧  这个, 首先需要介绍IBP帧是什么?为什么要这么做?解决了什么问题?带来的新的问题是什么?怎么选择?项目中RTMP,RTSP,HLS是怎么运用的?为什么这么运用?



### 剩余流媒体文档梳理
我现在需要你帮我梳理并撰写剩余的流媒体文档内容, 文档在C:\GoBackPlan\Knowledge\Media\Media.md
视频采集: 我已经完成了音频采集, 你参考我音频采集的文档以及代码[LivePushDemoActivity.java](../demo/flutter/flutteraar/app/src/main/java/com/example/flutteraar/ui/activity/LivePushDemoActivity.java)
整理总结我的视频采集. 视频采集里面有一点好像要注意:
  * 我看YUV数据好像做了矩阵转换, 这个是不是非常吃CPU性能的, 做这个是有必要的吗?
  * 我看JNI层对YUV数据做封装的时候判断了YUV的类型, 好像分为YUV和UVY顺序不一致? 
你顺便在文档中讲一讲原始数据YUV, 我想知道是Android摄像头采集的数据是YUV还是其他的嵌入式设备采集都是YUV,这是一个通用格式?
YUV格式矩阵转换的目的是什么? 很吃CPU性能吗? 能用DSP硬编加速吗? 为什么会出现YUV和UVY顺序不一致? 区别是什么?

Android Camera预览: 视频采集之后会获取到YUV数据, 这个数据如何预览的? TextureView和SurfaceView的区别是什么?
哪个性能更好? 项目中为什么用性能稍微差一些的TextureView? 能改成SurfaceView吗?(你不要改代码, 只写文档)
往文档中整理TextureView和SurfaceView的差异, 适用场景, 代码片段.

MediaCodec: 虽然我的项目没有用MediaCodec, 但是我为了提高性能准备使用DSP硬编, 
现在你从代码上介绍一下MediaCodec, Android封装好的API是怎样的, 各个参数是怎样的, 就像我之前音频分析AudioRecord一样分析.
然后现在我需要你绘制整个完整的计算机组成原理体系架构图在C:\GoBackPlan\Knowledge\408\计算机组成原理.md
对, 没错,是完整的计算机组成原理体系架构图. 你先去网上查一下 计算机组成原理这本书, 怎么介绍计算机内部体系架构的 绘制出来.
我让你绘制这个主要是为了全面的掌握计算机理论相关知识, 然后更好的理解DSP 然后理解 MediaCodec 然后理解硬编
然后再在**流媒体文档**中引用[计算机组成原理.md](../../../GoBackPlan/Knowledge/408/计算机组成原理.md)
然后再在文档里面绘制新的粗略图, 重点绘制arm架构下的CPU和DSP的关系, 然后再讲解MediaCodec是怎么实现硬编的, 以及硬编到底能比x264和Faac的软编快多少, 少吃多少CPU性能.
然后绘制两个通信图, 用于描述采集的数据分别走DSP MediaCodec硬编 和 CPU的x264和faac软编数据流程是怎样的, 资源占用和耗时是怎样的.

流媒体服务器:
流媒体服务器分为Nginx的RTMP流媒体服务器和MediaMtx的RTSP的流媒体服务器
nginx的代码再[nginx.conf](../demo/springboot/docker/nginx/nginx.conf)
现在你要分析nginx为RTMP做了什么, 怎么实现rtsp://的?
而且我听说nginx能配置RTMP性能和分辨率???这是真的吗?给我讲讲Nginx原理以及配置的代码.
MediaMtx我是通过docker下载实现的:[docker-compose.yml](../demo/springboot/docker/docker-compose.yml)
你上网查一下并结合我的代码分析一下, 我的MediaMtx做了什么? RTSP协议中的作用是什么?
然后绘制数据流通信图, 重点绘制数据流到了Nginx和MediaMtx这一环做了什么?

拉流:
现在我需要你整理Android是怎么拉流的? 虽然就是简单的输入rtmp, rtsp, hls的流 url,
但是我想知道服务器和Android具体做了什么? 服务器是怎么做到一对多的? 就是一个服务器的流为什么可以被多个用户甚至十万百万个用户拉流?
极限瓶颈是什么? CPU? 网络带宽? 以及跟我讲讲怎么计算某个性能CPU网络带宽的服务器的最大被拉流量是多少? 计算公式是怎样的?
如果遇到大量拉流怎么用降级策略?
CDN是什么? 是用来加速的吗? 在流媒体中的作用是什么? 代码上实现还是配置吗? 我这个项目有吗? 怎么添加CDN?

HLS: 
HLS是RTMP和RTSP之外的第三种格式, 我记得还有DASH, 分别适用于什么场景? 
为什么要HLS?不用DASH? 适用于什么场景? 我记得HLS适合的是在线视频播放, 因为他会吧视频切片让用户不必一次性加载全部HLS对吗?
我记得项目中生成是在后端, 你看看生成是怎么生成的, 生成逻辑是怎样的?
要么是在docker里面吧mp4转为hls, 要么就是在后端[demo](../demo/springboot/demo), 你梳理一下
如果一个视频过大的话在Android用ExoPlayer加载播放完会出现内存OOM吗? 是怎么避免的? HLS能解决这个问题吗?
你梳理一下Android的HLS播放逻辑:[LocalHlsPlayerActivity.java](../demo/flutter/flutteraar/app/src/main/java/com/example/flutteraar/ui/activity/LocalHlsPlayerActivity.java)
HLS的IBP帧是怎样的? 跟RTPM和RTSP的区别是什么? 

播放:
看看是RTMP, RTSP, HLS? 怎么处理IBP帧的? 怎么播放的, 底层原理是什么, 我知道是ExoPlayer, 这个player的底层播放原理是什么?

集成FFmpeg: 
项目是怎么集成FFmpeg的? 
SpringBoot我是用Docker: [Dockerfile](../demo/springboot/docker/Dockerfile)
Android的话看看是怎么集成的? [flutteraar](../demo/flutter/flutteraar)
梳理一下

项目中的FFmpeg:
梳理一下一下功能中FFmpeg参与的职能, 最好附上代码片段:
* RTSP推流
* 上传Mp4抽帧作为封面
* Mp4转为HLS推送
* HLS的m3u8离线转为Mp4

FFmpeg的基本功能:
上网查询并梳理FFmpeg在流媒体中常用的基本职能有哪些? Android和SpringBoot怎么调用?
梳理功能并给出部分使用代码片段

其他问题: 
最然我的项目没有使用WebRTC, 但是我将来会用.你简单介绍一下WebRTC, 对比一下跟RTMP, RTSP, HLS的区别
给出集成方案: 服务器如何集成, Android如何集成?
给出部分代码片段, 包括服务器(如果有或者配置)和Android的(发和收)

生产问题排查思路: 如果遇到直播, 推流文件, 在线视频花屏, 卡顿, 音视频不同步, 闪屏, 黑屏等问题怎么排查, 排查思路是怎样的? wireShark抓包吗? 怎么抓包? 分析RTMP,RTSP,HLS帧是否乱序?简单当前帧状态是否使用了UDP?
我希望你整理计算机网络的相关知识, 包括计算机七层, 计算机网络的各种协议, 写在[计算机网络.md](../../../GoBackPlan/Knowledge/408/计算机网络.md), 将你刚刚写的这些抓包, RTMP协议等网络知识基础整理进去. wireshark抓包的数据帧结构是怎样的? 从物理层到数据链路层一直到引用层抓出来的包是什么样子的.
然后在流媒体文档引用这个文档.
长时间播放视频出现越播越卡你准备怎么排查? 内存泄漏? 编解码? 如果一个很大的在线视频电影大概4K完整有10G怎么加载到ExoPlayer播放? 手机只有4G内存哦, 怎么避免内存溢出? Exoplayer内部的内存调度机制吗?
怎么排查服务器中推流是否发生内存泄漏? 怎么排查Android流媒体是否出现内存泄漏?
音视频编解码如果用软编和硬编应该分别怎样使用线程池? 线程池应该怎么配置? 线程池队列? 核心线程数量, 拒绝策略? 
Android是不是应该启动Service后台持续编解码? 还是只启动一次性的Worker或者IntentService?
你需要上网查询Java和C++的线程池分别怎么配置, kotlin的协程怎么使用, 以及线程池的原理? 最好全面一点 这些写到[操作系统.md](../../../GoBackPlan/Knowledge/408/操作系统.md)
我希望你写操作系统知识的时候最好结合操作系统书籍来写, 然后整理操作系统的知识和体系.其中有一个章节是进程, 进程里面有线程和协程, 你先梳理理论(从操作系统角度), 然后再写Java, C++, Kotlin的代码, 再写配置策略.
然后在流媒体文档引用这个文档.
最后你再看看流媒体有没有数据结构及算法分析相关的知识,如果有就整理到: [数据结构与算法分析.md](../../../GoBackPlan/Knowledge/408/数据结构与算法分析.md)

我的任务比较多,我希望你一个一个完成我全部的问题, 你一个一个完成再往我的文本里面写, 不要漏问题.



#### WebRTC
你看这个代码[flutteraar](../demo/flutter/flutteraar)
这个项目的介绍在[README.md](../demo/flutter/flutteraar/README.md)
我现在已经实现了RTMP、RTSP推拉流，HLS在线播放。现在想要再实现一个新的demo：
WebRTC视频通话，点开Demo之后会弹窗要求先绑定本机Id，如果不绑定就返回。
绑定成功之后显示本机ID，然后输入对方Id，然后可以点击Call。
Call之后如果另一个手机已经绑定了Id就会收到打给字节的视频通话。接听之后双方跳转新的Activity。
简单一点就上面展示自己的摄像头，下方展示对方的摄像头。
然后下方有开关静音，开关摄像头，挂断电话三个按钮。
现在我并不知道是否能实现，现在需要你先去查询方案，就比如Android端的依赖怎么选择，数据流怎么流转，是否需要后端以及后端怎么搭建。
对了，后端再[springboot](../demo/springboot)，docker在[docker-compose.yml](../demo/springboot/docker/docker-compose.yml)[Dockerfile](../demo/springboot/docker/Dockerfile)
nginx配置在：[nginx.conf](../demo/springboot/docker/nginx/nginx.conf)
后端代码在[demo](../demo/springboot/demo)（可能不用？还是要让对方收到Call消息需要用？我不清楚你来设计）
然后实现方案要写在[Live功能实现报告.md](../demo/flutter/flutteraar/Live功能实现报告.md)
我以前是没有接触过WebRTC的，我希望你先去上网查询可行方案，然后独立实现。


