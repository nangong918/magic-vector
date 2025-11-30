**Android**
====


## 基础问题

### Android 生命周期问题

#### 项目问题：
1. Fragment中调用PermissionUtils进行注册注册ActivityResultLauncher会出现问题，提示我不能重复注册注册ActivityResultLauncher，因为Fragment再次创建的时候，Activity并不会被暂停，此时的Activity是START状态。
   * Activity切换Fragment使用Navigation而不是FragmentManager (√)
2. 确认viewBinding的时机，viewBinding会执行几次。viewModel需要在什么时机进行初始化。
3. ViewModel和Activity的生命周期，在为什么要用ViewModel保证数据不丢失？
4. 需要Service保证WebSocket为什么要用Service保证后台任务的生命周期？
5. 性能检测：Activity跳转到了新的Activity之后，原先的View是重新绘制还是复用？Fragment在ViewPager2切换之后呢？
6. 项目中AgentEmojiActivity是可以旋转的，换砖会导致什么生命周期变化？数据是由ViewModel进行保障的吗？
7. Websocket的管理者放在MainApplication中作为全局跟放在Service中有什么区别呢？数据的生命作用域不好控制吗？
8. 尝试使用Dagger管理项目对象的依赖注入生命周期
9. `单一Activity阻塞同步的http请求` 直接在ViewModel上传就可以，`跨Activity非阻塞异步Http请求` 改译为使用Worker

### Android Activity嵌套跳转，Activity结束跳转问题

#### 项目问题：
项目需要使用的模式：场景：点击用户头像跳转到详情，详情点击发送消息跳转到消息，再次点击头像，是单例的详情Activity。如果是详情打开的消息，返回详情。
singleInstance模式 (MessageFragment) -> (ChatActivity) -> (UserDetailActivity) -> (ChatActivity) -back-> (UserDetailActivity) -back-> (MessageFragment)

### Android 异步任务、后台任务
#### 项目问题：
1. 任务活动域分析：
   * websocket/mqtt长连接：MainActivity启动；不同的Agent进行不同的绑定。messageFragment只进行TextChannel绑定；chat和agentEmoji页面需要进行AudioChannel绑定。该任务适合使用Service。并且需要长期保持活跃，可能需要FrontGroundService。
   * 缓冲池音频异步播放：audio播放，在AgentEmoji接收到的音频播放，返回到Chat不希望音频播放结束。由于kotlin协程的生命周期交给的是Activity，需要使用Worker/IntentService解决异步问题。在Chat返回Main的时候需要取消音频播放，因为要启动其他的Agent了，所以需要取消Worker（worker基本取代了IntentService和HandlerThread）。
   * VAD音频活动检测：在Chat的Call和AgentEmoji需要启动。跨越Activity生命周期。需要放入Service中进行绑定，通过AtomicBoolean控制VAD是否启用。
   * YOLOv8目标活动检测：在AgentEmoji中进行检测，仅仅属于单个Activity生命周期。无需放入Service，交给ViewModel进行管理。
   * 项目中没有IPC跨进程通讯需求，无需使用RemoteService。
2. Service绑定获取资源初始化异步问题：ServiceConnect是异步的，需要连接成功之后进行资源设置。
3. websocket长连接的心跳机制和重连接机制：BroadcastReceiver监听系统网络变化，监听变化进行重连。
4. 系统消息：websocket产生的本地消息应该使用EventBus传递而不是使用BroadcastReceiver
5. Base64编解码字节流数据考虑在后台执行。
6. kotlin协程：原先RxJava的Disposable手动取消任务改为Job取消；其余的异步使用协程lifecycleScope进行管理。
7. YOLOv8中CameraX预览销毁的时机应。

### Android 设计模式
#### 项目问题：
1. ViewBinding和DataBinding在现代Android开发并不合理，XML的数据绑定到ViewModel中的livedata上是不合理的操作，新推出的Jetpack Compose具有根号的DataBinding生命周期，应该弃用XML开发选用Compose将UI全部重构。
2. MVVM设计模式中数据observer中更新数据会导致循环观察异常。
3. 项目中的ViewModel没有发挥实际作用：1.网络请求的生命周期需要使用ViewModelScope实现。2.Activity旋转重新创建的ViewModel需要从`ViewModelProvider`获取，因为其示例是缓存在`ViewModelStore`。ViewModel需要在XML中进行DataBinding（此条暂时不实现，因为可以使用Compose进行绘制）。生命周期的管理也未实现，viewModel的生命周期应该由ViewModelStoreOwner实现。

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


# Demo App
* github 上获取Compose Demo
* RecyclerView -> Compose LazyList
* 参考JetChat重构App的View
* 引入Dagger注入
* Websocket的心跳请求demo
* 广播监听网络状态变化Demo
* 聊天记录的多数据源合并Demo
* Room本地存储Demo
* MKKV, SharedPreferences, LruCache缓存Demo
* 线程Demo: Kotlin协程(生命周期执行), RxJava, Handler(Handler, Looper, MessageQueue, HandlerThread, 线程池)
* Worker处理异步任务
* ViewPager2, Navigation切换Fragment
* CameraX预览 + 获取流 + MediaCore Demo
* JNI编写库Demo (Cpp的STL库)
* JNI 调用 FFmpeg编辑的Demo -> Github获取.
* View绘制与事件分发机制Demo
* Jetpack compose实现传统的一些view (Jetpack compose在github上的的viewDemo App)
* Activity, Fragment通讯以及生命周期
* 性能测试, 内存泄露检测demo