# 问题3


### KMP
我现在有一个比较重要的任务交给你，就是我现在的App要升级成跨平台，虽然说我本身有跨平台方案flutter，但是我现在也想尝试一下kmp。
我已经创建了一个新的项目：[kmp](../demo/kmp)
我的项目源是[app](../demo/app)，现在你需要把里面的全部功能迁移到kmp中，据我所知KMP的ui还是使用jetpack compose吧，大部分代码你直接迁移就行，
注意我使用的架构以及分层结构和mvi设计模式。需要尽量保持一直，
我大概介绍一下，我的ui放在[ui](../demo/app/app/src/main/java/com/vectordemo/ui)自定义view和主题都放在里面
然后数据源是网络和数据库：[repository](../demo/app/app/src/main/java/com/vectordemo/repository)
我的数据结构是：[domain](../demo/app/app/src/main/java/com/vectordemo/domain)
注意我的repository层不持有model，然后[dataSource](../demo/app/app/src/main/java/com/vectordemo/dataSource)层是用于映射的，
因为我不希望UI层感知entity和request和response，因为我认为这些是跟业务要分离的。后端的接口和数据库的接口是不能因为业务做改变而直接改变接口的，是需要内部做映射的，
映射的管理者都放在：[manager](../demo/app/app/src/main/java/com/vectordemo/manager)
数据类型转换都放在[convertor](../demo/app/app/src/main/java/com/vectordemo/domain/convertor)
然后[service](../demo/app/app/src/main/java/com/vectordemo/service)是Android的四大组件的service，我不知道kmp中有没有，你查一下资料看怎么迁移。
我这个的单例是放在[MainApplication.kt](../demo/app/app/src/main/java/com/vectordemo/MainApplication.kt)管理的，
你看看kmp有没有更高级的单例管理方式比如注入，我了解到的好像Android就有比如dagger。
然后viewmodel是在[viewModel](../demo/app/app/src/main/java/com/vectordemo/viewModel)
其他你自己看吧。反正我希望你能完成功能的迁移，让我的kmp能正常使用。

注意注意你也要先学一下KMP的相关基础知识，
比如说：开发应该在[commonMain](../demo/kmp/shared/src/commonMain)，而[androidMain](../demo/kmp/shared/src/androidMain)和[iosMain](../demo/kmp/shared/src/iosMain)这两个的kmp提供的原生接口，
也就是意味着你的那些KNI接口需要放在androidMain而不是commonMain因为这些是ios和Android公用的代码，明显ios无法实现JNI/KNI所以这些c++的方法放在原生androidMain就行，
然后在应用层要判断如果是ios的话就说明一下暂时不支持此功能。然后你自己思考判断这些功能是ios和Android公用还是Android特有，Android特有就写在androidMain然后commonMain分别处理，
否则就就只把代码迁移到commonMain。还有关于Java代码，你尽量想办法转化成kotlin吧。

最后我需要审核你怎么做的，你要写一个文档向我汇报[KMP重构.md](kmp/KMP重构.md)，需要包括原生接口怎么处理的，通用怎么迁移的，架构是否沿用我的架构，gradle依赖有没有做到ios和Android公用等。



#### 问题补充
[kmp](../demo/kmp)是一个kmp项目，我是将[app](../demo/app)迁移过来的，
现在有些问题需要你帮我改一下：
1. 第一就是我不知道为什么我的app只要一点返回就会直接返回手机主页而不是返回上一页。是不是因为activity的栈模式没配置对？
2. 网络访问权限好像没有开，我去调用聊天demo，但是发现：ERROR 消息发送失败： CLEARTEXT communication to localhost not permitted by network security policy
   看报错也可能是配置的url不对？因为他说的是localhost？还是说http被禁用了？这个需要解除禁用，要允许http访问，不用强制https
3. 我以前是做过flutter的项目的，就比如说jni的这种功能，我就会在/android里面写一个接口，给/lib里面的dart调用，那我也是要迁移cpp代码的啊。
   但是我在这个迁移项目里面是完全没有看到cpp代码的迁移，我在想cpp的代码是不是要迁移到androidMain里面，因为我理解的是commonMain就是跨平台代码相当于lib，
   androidMain相当于/android，里面可以放jni（kni）和cpp吧，但是我没看到这个项目引入，cpp代码，看看能不能帮我实现一下，代码在C:\CodeLearning\magic-vector\demo\app\app\src\main\cpp
4. 还有一些功能其实是在aar里面的，我看这个项目也没有，aar是专门给Android的，同样ios不支持，aar的libs在C:\CodeLearning\magic-vector\demo\app\aarlib，也需要你复制然后迁移并实现功能的调用。
5. 我现在明明是用Android测试，但是大部分功能却检测我是IOS设备，跟我说功能不支持。首先你要上网查一下KMP怎么判断设备是Android还是IOS设备，然后根据不同类型去判断做逻辑。帮我完成我的需求

你解决问题的方案需要写在[KMP重构.md](kmp/KMP重构.md)


### KMP
1. 集成ViewModel
2. 修复语音唤醒Demo
3. 修复KNI原生调用
4. 整理KMP
5. 新增UI demo，要同时同步给Android原生，Flutter
6. 修复旋转之后生命周期丢失（旋转屏幕就得重新代开app了）


你查询一下KMP是否支持ViewModel，然后集成，[kmp](../demo/kmp)这个项目其实是对[app](../demo/app)的迁移，
关于ViewModel的使用方式你可以参考[app](../demo/app)[viewModel](../demo/app/app/src/main/java/com/vectordemo/viewModel)内部的viewmodel是怎么做的
然后如果KMP是支持Viewmodel的，我希望你能将其集成进我的项目。
你看看这个是否可以：org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-ktx
我希望你在commonMain公用，而不是只给androidMain



### WeChat UI Demo
我在学习KMP和Flutter，我现在对这两个UI不是很熟悉，但是我对Android的XML的UI开发非常熟悉，现在我想要你帮我写Demo，就叫做WeChat UI Demo
首先我认为应该这样分类：
最重要的就是原理：WMS，WindowsManagerService
看看KMP和Flutter的WMS原理是怎样的？是怎么进行View点击时间拦截与下发的，是怎么View叠加的，是怎么适配不同手机的像素的。
然后就是：
GroupView：这些一般常用的只有两个：LinearLayout（行、列）ConstraintLayout
跳过基本控件，太简单了没啥用
然后是自定义RecyclerView列表，里面是要放一个自定义ItemView的
然后是viewPager就是底部一个Naviagtion然后点击跳转不同的ViewPager然后左右滑动也可以，然后底部的Navigation的icon也要跟随滑动变化。

最后就是Compose特有的动画view

其实我觉得最简单的学习view的办法就是做一个类似微信的demo
这个demo的数据是写死在代码的，完全不用什么数据库。
UI学习设计点：
这个demo点开之后是三个page：消息，通讯录。（1. Navigation和ViewPager，LinearLayout，ScollerView）
消息页面的消息Item要跟微信一样，包括头像，名称，消息预览，右侧要有消息时间。消息属于自定义view，要单独创建文件或者组合函数叫做Contact Message（2. 自定义view和RecyclerView）
消息页面要能下拉刷新（3. 下拉刷新）这个下拉刷新是假的，根本不用去读取数据，直接延迟两秒toast弹出刷新成功就行。
点击消息页面的消息会进入消息页面可以发送消息，这个进入聊天页面是消息item动态变大进入页面，然后点击右上角的返回也是动态变小返回消息list（4. view动画）
聊天页面要能下拉刷新（5. list感知下拉到底下拉加载更多）因为聊天页面其实最开始只能展示一个屏幕的消息，但是总的历史消息不止这么多，要下拉从历史的list中加载，也就是listview要能感知到下拉到底了，然后从list中加载历史聊天记录
消息要分为对方发的和我发的，我也要能真实的发消息（6. List中根据itemType展示两种不同的view）
点击对方或者自己的头像要进入用户详情页面，这个过程也是头像动态变大然后在详情顶部填充满的正方形（7. view动效）
这个其实每个人的头像都是2张以上，所进入详情页面之后用户的头像是又可以跟viewPager一样右滑和左滑切换的（8. viewPager）
详情里面是可以下拉看这个人的朋友圈的，朋友圈是九宫格照片和文字的组合，还有日期，可以点赞和评论并更新view（9. viewModel、changeNotifier实时更新view）
然后刚刚说了聊天页面往上拉会加载历史记录，然后只要list判断现在不是在最下方就会在发送框的上方弹出一个小气泡“回到最新消息”
然后可以跟用户进行语音通话，其实是假的，主要是绘制view，这个view要显示对方头像，然后要显示通话时间以及静音和挂断按钮（10. 这个页面明显不能使用LinearLayout，要使用ConstraintLayout）
然后回到主页面，主页面的通讯录显示的是用户列表，顶部有所搜view，可以搜索筛选联系人，点击联系人按钮也是跳转到用户详情页面（11. 页面复用AMS中重复的Page不应该重复创建而是从栈中取出，因为这时候用户可以循环点击头像进入用户详情，再点击发送消息进入聊天页面，这样循环点击如果不做栈处理就会循环创建导致内存OOM，应该采用页面复用）

我现在需要你实现三套代码，三个代码的UI和逻辑要求一模一样，分别是：
Jetpack Compose的原生Android：[app](../demo/app)
KMP，也是Jetpack Compose，UI几乎一样，只是框架与原生略有区别：[kmp](../demo/kmp)
Flutter：[flutternew](../demo/flutter/flutternew)
你要遵循这三个项目的原先已有架构开发这个UI demo

然后实现完成这个demo我希望你把UI相关的知识写道[UI.md](UI/UI.md)


#### 简化
我在学习KMP和Flutter，我现在对这两个UI不是很熟悉，但是我对Android的XML的UI开发非常熟悉，现在我想要你帮我写Demo，就叫做WeChat UI Demo

其实我觉得最简单的学习view的办法就是做一个类似微信的demo
这个demo的数据是写死在代码的，完全不用什么数据库。
UI学习设计点：
这个demo点开之后是三个page：消息，通讯录。（1. Navigation和ViewPager，LinearLayout，ScollerView）
消息页面的消息Item要跟微信一样，包括头像，名称，消息预览，右侧要有消息时间。消息属于自定义view，要单独创建文件或者组合函数叫做Contact Message（2. 自定义view和RecyclerView）
消息页面要能下拉刷新（3. 下拉刷新）这个下拉刷新是假的，根本不用去读取数据，直接延迟两秒toast弹出刷新成功就行。
点击消息页面的消息会进入消息页面可以发送消息，这个进入聊天页面是消息item动态变大进入页面，然后点击右上角的返回也是动态变小返回消息list（4. view动画）
聊天页面要能下拉刷新（5. list感知下拉到底下拉加载更多）因为聊天页面其实最开始只能展示一个屏幕的消息，但是总的历史消息不止这么多，要下拉从历史的list中加载，也就是listview要能感知到下拉到底了，然后从list中加载历史聊天记录
消息要分为对方发的和我发的，我也要能真实的发消息（6. List中根据itemType展示两种不同的view）
点击对方或者自己的头像要进入用户详情页面，这个过程也是头像动态变大然后在详情顶部填充满的正方形（7. view动效）
这个其实每个人的头像都是2张以上，所进入详情页面之后用户的头像是又可以跟viewPager一样右滑和左滑切换的（8. viewPager）
详情里面是可以下拉看这个人的朋友圈的，朋友圈是九宫格照片和文字的组合，还有日期，可以点赞和评论并更新view（9. viewModel、changeNotifier实时更新view）
然后刚刚说了聊天页面往上拉会加载历史记录，然后只要list判断现在不是在最下方就会在发送框的上方弹出一个小气泡“回到最新消息”
然后可以跟用户进行语音通话，其实是假的，主要是绘制view，这个view要显示对方头像，然后要显示通话时间以及静音和挂断按钮（10. 这个页面明显不能使用LinearLayout，要使用ConstraintLayout）
然后回到主页面，主页面的通讯录显示的是用户列表，顶部有所搜view，可以搜索筛选联系人，点击联系人按钮也是跳转到用户详情页面（11. 页面复用AMS中重复的Page不应该重复创建而是从栈中取出，因为这时候用户可以循环点击头像进入用户详情，再点击发送消息进入聊天页面，这样循环点击如果不做栈处理就会循环创建导致内存OOM，应该采用页面复用）

我现在需要你实现三套代码，三个代码的UI和逻辑要求一模一样，分别是：
KMP，Jetpack Compose，只是框架与原生略有区别：[kmp](../demo/kmp)
也是Jetpack Compose，UI几乎一样，原生的Android：[app](../demo/app)
Flutter：[flutternew](../demo/flutter/flutternew)
你要遵循这三个项目的原先已有架构开发这个UI demo

然后现在我们一步一步来，我现在只要求你开发第一个模块：[kmp](../demo/kmp)



