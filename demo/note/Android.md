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

* Android Activity嵌套跳转。Activity结束跳转问题。
  Activity的四种启动模式：
  1. standard 默认启动模式：Activity可多次创建压入栈
  2. singleTop 栈顶复用模式：栈顶部的Activity复用不会再次创建，其他Activity创建压入栈
  3. singleTask 栈内复用模式：栈内Activity复用不会再次创建。如果再次创建Activity1，23会被销毁。
  4. singleInstance 全局唯一模式：A1启动A2，A2会存放在Task2。A2启动A1，Task2返回Task1。 
  * 项目问题：项目需要使用的模式：场景：点击用户头像跳转到详情，详情点击发送消息跳转到消息，再次点击头像，是单例的详情Activity。如果是详情打开的消息，返回详情。
  singleInstance模式 (MessageFragment) -> (ChatActivity) -> (UserDetailActivity) -> (ChatActivity) -back-> (UserDetailActivity) -back-> (MessageFragment)