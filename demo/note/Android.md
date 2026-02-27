**Android**
====


## 基础问题

### Android 生命周期问题
[Android生命周期.md](android/Android生命周期.md)


### Android Activity嵌套跳转，Activity结束跳转问题
#### Activity的四种启动模式：
1. standard 默认启动模式：Activity可多次创建压入栈
2. singleTop 栈顶复用模式：栈顶部的Activity复用不会再次创建，其他Activity创建压入栈
3. singleTask 栈内复用模式：栈内Activity复用不会再次创建。如果再次创建Activity1，23会被销毁。
4. singleInstance 全局唯一模式：A1启动A2，A2会存放在Task2。A2启动A1，Task2返回Task1。 (Twitter一直点击用户胡转发的帖子)

### Android 异步任务、后台任务

#### Worker和IntentService
都是处理`跨Activity非阻塞异步`任务，IntentService出现在API 4，Worker出现在Android Jetpack (API 14+)。IntentService已经废弃。

### Android 设计模式
#### 项目问题：
1. ViewBinding和DataBinding在现代Android开发并不合理，XML的数据绑定到ViewModel中的livedata上是不合理的操作，新推出的Jetpack Compose具有根号的DataBinding生命周期，应该弃用XML开发选用Compose将UI全部重构。
2. MVVM设计模式中数据observer中更新数据会导致循环观察异常。
3. 项目中的ViewModel没有发挥实际作用：1.网络请求的生命周期需要使用ViewModelScope实现。2.Activity旋转重新创建的ViewModel需要从`ViewModelProvider`获取，因为其示例是缓存在`ViewModelStore`。ViewModel需要在XML中进行DataBinding（此条暂时不实现，因为可以使用Compose进行绘制）。生命周期的管理也未实现，viewModel的生命周期应该由ViewModelStoreOwner实现。

todo：设计模式文档存在问题，待优化。
[Android设计模式.md](android/Android设计模式.md)


### Android View
#### 项目问题：
1. 聊天记录中的ScrollView中的RecyclerView下拉触摸焦点丢失。需要重构TouchEvent的事件分发机制。
2. AgentEmoji的动效与绘制：需要使用帧动画进行绘制。
3. 重构事件分发机制，修复EditText输入完成之后点击空白处键盘仍然显示的问题。

### 数据持久化与缓存
#### 项目问题：
1. 打开App之后要在未连接互联网的情况下显示ChatList和账号的全部数据：LruCache，SQLite，Room，SharedPreferences，网络缓存（Okhttp，Retrofit内置网络缓存），MKVV存储。

### Android 资源
#### 项目问题：
1. 选择系统照片进行上传：使用 `ContentResolver` 与 `ContentProvider` 交互来访问选定的照片数据，`ContentObserver` 则在监听数据变更时使用。

### Android序列化
#### 项目问题：
1. Intent传递的时候Serializable方法过时，需要使用Parcelable

## 进阶问题

### Android网络
#### 项目问题：
1. websocket / netty / mqtt长连接更加稳定，增加心跳连接机制。
2. 配置可以选择视频流传输方式：纯UDP，WebRTC，RTMP/RTSP

### Android JNI
#### 项目需求：
1. 使用JNI调用webRTC的VAD功能
2. 使用JNI调用RTMP协议进行向后端推拉流
3. 使用JNI调用FFmpeg 将Bitmap转为视频流

### Binder，AIDL
正在考虑，目前项目暂时不需要使用Binder和AIDL

### Android 音视频
#### 项目问题：
1. 音频获取数据过大：AudioRecord的数据过大，音频输出频率过快。
2. 音频播放：AudioTrack：1. 码率需要跟后端对齐 2. 播放的任务需要放在后台跑
3. YOLOv8目标检测 + VL视觉模型理解：记录的总是Bitmap而不是视频流，看看能不能用 MediaRecorder + MediaCodec + CameraX 解决这个问题
4. 上传的Bitmap需要进行压缩，节省AgentToken
5. 使用SurfaceView进行CameraX预览 + YOLOv8目标识别检测实时绘制。

### Android 性能
#### 项目问题：
1. 可能出现的内存问题：
   * Android端用户一直执行录音行为，导致内存不断上升，最终导致OOM。并且Android会一直往后端传输UDP的视频流数据，导致后端也出现Out of Memory。
   * 内存泄漏：静态引用会导致内存泄漏，需要额外住哟任何静态引用：final static 和 val, 适当改为WeakReference<T>
   * 音频，视频资源的频繁创建和释放资源可能导致内存抖动。
   * 检查全部对Context引用的地方，检查是否需要使用强引用，如果不需要就要使用`WeakReference<Context>`弱引用。尤其是网络请求和Handler异步处理，因为在处理的时候可能Activity就已经销毁了，但是由于Handler的引用导致GC无法销毁而造成内存泄漏。
   * RxJava替换为Kotlin协程。Android生命周期无法自动管理RxJava的异步任务，需要手动处理，而协程kotlin能直接被ViewModelScope或者Activity生命周期管理。
2. 可能出现的ANR卡顿：
   * 检查在Main线程执行的任何耗时任务，需要把全部的Main线程任务剥离到Worker线程中去，因为如果UI线程处理任务超过5s没有响应就会触发ANR。
   * 检查不合理的布局和绘制：频繁调用 invalidate() 或 requestLayout()，可能触发多次布局和绘制，导致主线程卡顿。
3. View全重绘问题：
   * RecyclerView更新/插入一个Item不能重新绘制整个RecyclerView，需要使用DiffUtil

### 信息安全：混淆，反编译，抓包
#### 项目问题
1. 混淆会出现很多问题，需要仔细研究配置混淆文件。
2. JADX反编译检查回校结果
3. Wireshark进行网络请求数据抓包，检查数据加密。

### Android 依赖
#### 项目问题：
1. 取消非官方的任何依赖库：FastJson -> Gson; PermissionX -> PermissionUtils;

### Android硬件相关
#### adb

连接硬件设备：
* 数据线连接：`adb connect <设备IP>` 然后执行 `adb root` 提供root权限
* 网络连接：网络调节助手：NetAsset，连接设备固定IP，发送命令：`adb on`
* 然后Android Studio应该就可以看到设备的信息。