**Android**
====


## Android基础问题

* Android Activity嵌套跳转。Activity结束跳转问题。
  Activity的四种启动模式：
  1. standard 默认启动模式：Activity可多次创建压入栈
  2. singleTop 栈顶复用模式：栈顶部的Activity复用不会再次创建，其他Activity创建压入栈
  3. singleTask 栈内复用模式：栈内Activity复用不会再次创建。如果再次创建Activity1，23会被销毁。
  4. singleInstance 全局唯一模式：A1启动A2，A2会存放在Task2。A2启动A1，Task2返回Task1。 
  * 项目需要使用的模式：场景：点击用户头像跳转到详情，详情点击发送消息跳转到消息，再次点击头像，是单例的详情Activity。如果是详情打开的消息，返回详情。
  singleInstance模式 (MessageFragment) -> (ChatActivity) -> (UserDetailActivity) -> (ChatActivity) -back-> (UserDetailActivity) -back-> (MessageFragment)