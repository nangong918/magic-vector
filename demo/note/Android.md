**Android**
====


## Android基础问题

* Android 生命周期问题
  * 项目问题：
    1. Fragment中调用PermissionUtils进行注册ActivityLauncher会出现问题，提示我不能重复注册ActivityLauncher，因为Fragment再次创建的时候，Activity并不会被暂停，此时的Activity是START状态。
    2. 确认viewBinding的时机，viewBinding会执行几次。viewModel需要在什么时机进行初始化。
    3. ViewModel和Activity的生命周期，在为什么要用ViewModel保证数据不丢失？
    4. 需要Service保证WebSocket为什么要用Service保证后台任务的生命周期？
    5. 性能检测：Activity跳转到了新的Activity之后，原先的View是重新绘制还是复用？Fragment在ViewPager2切换之后呢？
    6. 项目中AgentEmojiActivity是可以旋转的，换砖会导致什么生命周期变化？数据是由ViewModel进行保障的吗？
    7. Websocket的管理者放在MainApplication中作为全局跟放在Service中有什么区别呢？数据的生命作用域不好控制吗？

* Android Activity嵌套跳转。Activity结束跳转问题。
  Activity的四种启动模式：
  1. standard 默认启动模式：Activity可多次创建压入栈
  2. singleTop 栈顶复用模式：栈顶部的Activity复用不会再次创建，其他Activity创建压入栈
  3. singleTask 栈内复用模式：栈内Activity复用不会再次创建。如果再次创建Activity1，23会被销毁。
  4. singleInstance 全局唯一模式：A1启动A2，A2会存放在Task2。A2启动A1，Task2返回Task1。 
  * 项目问题：项目需要使用的模式：场景：点击用户头像跳转到详情，详情点击发送消息跳转到消息，再次点击头像，是单例的详情Activity。如果是详情打开的消息，返回详情。
  singleInstance模式 (MessageFragment) -> (ChatActivity) -> (UserDetailActivity) -> (ChatActivity) -back-> (UserDetailActivity) -back-> (MessageFragment)

* Android 后台任务模式
  * 项目问题：
    1. 任务活动域分析：
       * websocket/mqtt长连接：MainActivity启动；不同的Agent进行不同的绑定。messageFragment只进行TextChannel绑定；chat和agentEmoji页面需要进行AudioChannel绑定。该任务适合使用Service。并且需要长期保持活跃，可能需要FrontGroundService。
       * 缓冲池音频异步播放：audio播放，在AgentEmoji接收到的音频播放，返回到Chat不希望音频播放结束。由于kotlin协程的生命周期交给的是Activity，需要使用Worker/IntentService解决异步问题。在Chat返回Main的时候需要取消音频播放，因为要启动其他的Agent了，所以需要取消Worker（worker基本取代了IntentService和HandlerThread）。
       * VAD音频活动检测：在Chat的Call和AgentEmoji需要启动。跨越Activity生命周期。需要放入Service中进行绑定，通过AtomicBoolean控制VAD是否启用。
       * YOLOv8目标活动检测：在AgentEmoji中进行检测，仅仅属于单个Activity生命周期。无需放入Service，交给ViewModel进行管理。
       * 项目中没有IPC跨进程通讯需求，无需使用RemoteService。
    2. Service绑定获取资源初始化异步问题：ServiceConnect是异步的，需要连接成功之后进行资源设置。
    3. websocket长连接的心跳机制和重连接机制：BroadcastReceiver监听系统网络变化，监听变化进行重连。
    4. 系统消息：websocket产生的本地消息应该使用EventBus传递而不是使用BroadcastReceiver

* Android 资源
  * 项目问题：
    1. 选择系统照片进行上传：使用 `ContentResolver` 与 `ContentProvider` 交互来访问选定的照片数据，`ContentObserver` 则在监听数据变更时使用。