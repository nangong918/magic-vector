**Android**
====


## 基础问题

### Android 生命周期问题

#### Activity的生命周期
Activity生命周期
1. onCreate()：创建的时候
   * 绑定View：binding = Binding.inflate(layoutInflater)
   * 注册ActivityResultLauncher
   * 注册viewModel，注册观察者。
   * 初始化CameraX（但是不绑定生命周期）
   * Service绑定
   * Button监听器绑定
2. onStart()：不可见，准备前台可见
   * 广播接收器
   * EventBus注册
   * 初始化网络请求（可能需要Service绑定后的资源，一般在Service绑定成功的回调中执行。）
   * CameraX绑定生命周期
3. onResume()：与用户交互阶段
   * 开启CameraX
   * 开启YOLOv8检测
   * 开启AudioRecord
   * 开启AudioTrack
   * 动画绘制
4. onPause()：暂停交互 (不要在此处解绑生命周期和释放资源，因为系统可能只是短暂失去焦点（如弹出对话框）)
   * 暂停音频/视频播放
   * 提交未保存的更改（例如草稿）
5. onStop()：清理可见相关资源
   * 停止YOLOv8检测
   * 停止AudioRecord
   * 停止AudioTrack
   * CameraX解绑生命周期
6. onDestroy()：最终清理
   * 解绑Service
   * 释放资源


Activity的行为导致生命周期的变化：
* 横屏反转、横竖屏反转
   ```text
   原Activity: onPause() -> onStop() -> onDestroy()
   新Activity: onCreate() -> onStart() -> onResume()
   ```
* Activity1 startActivity(intent) 跳转 Activity2 -> Activity2 finish销毁回到 Activity1
   ```text
   Activity1: onPause() -> onStop()
   Activity2: onCreate() -> onStart() -> onResume()
   --- 用户在Activity2中按返回或调用finish() ---
   Activity2: onPause()
   Activity1: onRestart() -> onStart() -> onResume()
   Activity2: onStop() -> onDestroy()
   ```
* Activity1 ActivityResultLauncher(intent) 跳转 Activity2 -> Activity2 finish销毁回到 Activity1
   * 流程同上，但是在Activity2中调用setResult()后调用finish()。Activity1在onResume()之后，会通过ActivityResultLauncher的回调接收到结果
* Android Home键回到桌面 -> 从桌面返回App时的生命周期
   ```text
   按下Home键: onPause() -> onStop()
   从桌面返回: onRestart() -> onStart() -> onResume()
   ```

#### Fragment的生命周期
Fragment生命周期类似Activity：
```text
onAttach() → onCreate() → onCreateView() → onViewCreated() → onStart() → onResume()
→ onPause() → onStop() → onDestroyView() → onDestroy() → onDetach()
```
不同点：
`onAttach()`: Fragment与Activity关联时调用
  * 此时的Fragment的 `isAdded()` 方法是true
`onCreateView()`: 创建Fragment的UI布局
  * 进行binding填充
`onViewCreated()`: View已创建完成，最适合初始化UI相关操作
  * 初始化View数据, liveData观察数据
`onDestroyView()`: View被销毁，但Fragment实例仍然存在

关于`isAdded()`：在`onAttach()`之后是true，在 `onDetach()` 之后是false。

关于什么时候能获取到Fragment的ViewModel？
* 场景1：获取Fragment自己的ViewModel
```kotlin
// 在任何生命周期方法中都可以，但推荐在onCreate或onViewCreated中
class MyFragment : Fragment() {
    // ✅ 方式1：在onCreate中获取（最早的位置）
    private lateinit var viewModel: MyViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MyViewModel::class.java)
    }
    
    // ✅ 方式2：使用property delegate（推荐）
    private val viewModel: MyViewModel by viewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 这里可以安全使用viewModel
        viewModel.data.observe(viewLifecycleOwner) { data ->
            binding.textView.text = data
        }
    }
}
```
* 场景2：获取共享的Activity ViewModel（Fragment间通信）
```kotlin
class MyFragment : Fragment() {
    // 获取所在Activity的ViewModel
    private val sharedViewModel: SharedViewModel by activityViewModels()
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel.sharedData.observe(viewLifecycleOwner) { data ->
            // 多个Fragment可以观察同一个数据
        }
    }
}
```
Fragment自己的ViewModel: 从onCreate()开始就可以获取

Activity的共享ViewModel: 从onAttach()之后就可以获取

#### ViewModel的生命周期
ViewModel的生命周期比Activity长，能在Activity不是真正销毁的时候保留数据。
Activity的 finish() 和 屏幕旋转 都会导致 onDestroy() ，但是两者的销毁步兵不同。
* 屏幕旋转：清理viewModel的viewModelScope
* finish()：清理viewModel的viewModelScope

创建ViewModel：
由于viewModel的生命周期比Activity长，所以创建的时候需要借助ViewModelProvider进行获取和管理viewModel。首次获取则创建，非首次则获取。
```kotlin
class MyActivity : AppCompatActivity() {
    // 最简单的方式
    private val viewModel: MyViewModel by viewModels()
    
    // 带Factory的方式
    private val viewModelWithFactory: MyViewModel by viewModels { 
        MyViewModelFactory("参数") 
    }
}

class MyActivity : AppCompatActivity() {
   private lateinit var viewModel: MyViewModel

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      // 传统方式
      viewModel = ViewModelProvider(this).get(MyViewModel::class.java)

      // 带Factory的传统方式
      val factory = MyViewModelFactory("参数")
      viewModel = ViewModelProvider(this, factory).get(MyViewModel::class.java)
   }
}
```
销毁viewModel：
系统调用onDestroy()之后，viewModel的onCleared()会被调用。
```kotlin
class MyViewModel : ViewModel() {
    private val networkRequest: Job? = null
    private val databaseConnection: Closeable? = null
    
    override fun onCleared() {
        super.onCleared()
        // 清理持有的资源
        networkRequest?.cancel()
        databaseConnection?.close()
        Log.d("ViewModel", "清理所有资源")
    }
}
```

#### Service和Application的生命周期
Service的生命周期：
* 创建：onCreate()
* 启动：onStart()
* 停止：onStop()
* 销毁：onDestroy()
* 绑定：onBind()
* 解绑：onUnbind()

Application的生命周期：
* 创建：onCreate()
* 销毁：onTerminate()

Application可以放置任何全局变量。
Service应该放置跨Activity任务。

#### Worker和IntentService
都是处理`跨Activity非阻塞异步`任务，IntentService出现在API 4，Worker出现在Android Jetpack (API 14+)。IntentService已经废弃。

### Android Activity嵌套跳转，Activity结束跳转问题
#### Activity的四种启动模式：
1. standard 默认启动模式：Activity可多次创建压入栈
2. singleTop 栈顶复用模式：栈顶部的Activity复用不会再次创建，其他Activity创建压入栈
3. singleTask 栈内复用模式：栈内Activity复用不会再次创建。如果再次创建Activity1，23会被销毁。
4. singleInstance 全局唯一模式：A1启动A2，A2会存放在Task2。A2启动A1，Task2返回Task1。 (Twitter一直点击用户胡转发的帖子)

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