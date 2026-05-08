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











