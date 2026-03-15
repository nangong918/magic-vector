**Cursor提问记录**
====



# 启动页，注册登录

开发的时候应该先读取我项目中的文档：[ProjectAndMainRule.md](ProjectAndMainRule.md)包括里面涉及到了的模块的的开发规范和规则。

现在开发Android和SpringBoot的相关功能，Flutter先不同步。
SpringBoot的minio我还没配置好，但是你可以先设计文件传输，SpringBoot那边先不对文件做处理，
当然如果你愿意，你也可以帮我配置一下，我的minio在[minio-starter](springboot/starters/minio-starter)
目前我还没有网关，所以没法反向代理，一般minio返回的是url路径资源，这个资源需要被反向代理，我现在没做，
所以你返回一个错的url也没关系，记得写个todo。
还有根据`ProjectAndMainRule`，你需要写cursorDevelopLog，Android和springboot的都要写，其实不用写多复杂，就写一下自己设计了哪些类，
哪些功能，记录一下就好了，最好是uml的，我比较注重类图，对象图，活动图，状态机图，时序图，通讯图。
关于Android，如果已有功能并且是Compose+MVI直接在上面添加就好了，如果是XML+MVVM就需要新创建文件前缀Compose*xxx，不要删除源文件。因为我在重构还需要审核原先的逻辑。


## Start 页面设计

### 业务描述
- 应用启动后通过 UserManager 检查是否有已登录用户
- 有用户信息就去请求后端验证 access_token 是否有效 （access_token放在请求头，无需设计refreshToken）
- 验证有效则自动跳转到 Main 页面
- 验证无效或没有用户信息则自动跳转到 Login 页面
- 启动页至少停留 1200ms，避免一闪而过

### UI大概设计
- 已实现，就是个居中显示的 Logo

### MVI大概设计 (我给你举例子，以后自己设计)
- dataState: isLoggedIn（登录状态）
- intent: Initialize（触发登录检查）
- effect: GoToMain, GoToLogin（导航用）


## Login 页面设计

### 业务描述
- 用户输入账号密码登录
- 登录成功后后端返回 userId、accessToken 等信息
- 调用 UserManager 保存用户信息到数据库
- 登录成功后跳转到 Main 页面
- 页面底部有个小链接可以跳转到注册页面


### UI大概设计
- 账号输入框
- 密码输入框（要隐藏输入内容）
- 登录按钮（账号密码没填的时候置灰不可点）
- "没有账号？去注册" 这样的文本链接

### MVI 设计
自己设计

## Register 页面设计

### 业务描述
- 用户输入账号密码注册
- 可以选头像，不选也行
- 选头像前要用 PermissionUtils 申请存储权限
- 注册成功后后端返回用户信息，调用 UserManager 保存到数据库
- 注册成功后直接跳转到 Main 页面（不用再登录一次）
- 页面底部有个链接可以跳转到登录页面


### UI大概设计
- 账号输入框
- 密码输入框
- 确认密码输入框（用来校验两次输入是否一致）
- 头像选择区域（圆形头像预览，点击可以选择图片）
- 注册按钮（信息没填完整时置灰）
- "已有账号？去登录" 文本链接

### MVI 设计
自己设计




## 问题补充 + Bug修复
我现在要提一些修改需求，你现在要按照我的需求进行修改，并把我的需求归纳整理到我刚刚跟你说的：
SpringBoot的[developAndRules.md](springboot/docs/developAndRules.md)和Android中[developAndRules.md](app/android/docs/developAndRules.md)

* Android的Formdata之外的数据Post请求需要使用单一请求体类型，请求体内部可以用Entity或者Module进行聚合。
  所以你的/user/login和/user/token/verify接口都要定义请求体以及响应体。响应体不能单独一个Boolean，不可扩展，要封装成xxxResponse内部聚合可拓展。
  User的下面两个接口都要重新设计包括SpringBoot
* 数据结构参考Android详细设计中说的数据结构定义：
    ## Domain
    * 基本的数据结构要放在domain中, dto中存放request和response, entity中存放数据库实体, module中存放业务实体
  其中，几乎每个请求都要定义request和response，他们属于dto，dto可以由module和entity进行聚合，module可以由entity进行聚合。entity不能聚合必须跟表一一对应。
* 我说了mvi的uistate，datastate，intent，effect，event的字段定义都要写注释的，不然后续不好维护。
* 你把cursor开发结束之后需要绘制uml图的事情记录到[ProjectAndMainRule.md](ProjectAndMainRule.md)中，
  `uml的，我比较注重类图，对象图，活动图，状态机图，时序图，通讯图。`现在并且除了这些之外还需要`线程状态图`，你看看`mermaid`有没有实现的办法，线程任务执行时序和各个状态管理这个最重要了。
  所以需要你绘制到cursorDevelopLog中，并把以后开发完成之后要绘制uml图的任务记录到[ProjectAndMainRule.md](ProjectAndMainRule.md)
* SpringBoot的问题，这次我没看到[magic_vector.sql](springboot/db/magic_vector.sql)数据库的改动，我审核了一下代码确实没有需要改的，这次就这样，下次如果涉及到之后你需要修改这个数据库的sql设计。
  并且在cursorDevelopLog中记录修改内容，记录为什么这么设计，以及你现在要把我说的这条记录到SpringBoot的[developAndRules.md](springboot/docs/developAndRules.md)中
* cursorDevelopLog在涉及到数据库的调整时，需要在cursorDevelopLog中记录数据库表设计以及改动，最好有设计图，我不知道`mermaid`能否实现，你看下能否实现。Android的Room和Spring的MySql都需要。
  并且把我这条开发任务记录到SpringBoot的[developAndRules.md](springboot/docs/developAndRules.md)和Android中[developAndRules.md](app/android/docs/developAndRules.md)
* SpringBoot的问题：我看你UserService竟然写获取UserDo的代码，这是不允许的，Service只能进行业务操作和获取业务实体，比如获取Module。或者执行void，boolean等无实际返回函数。
  Do的这种Entity数据库类型应该交给Mapper层处理。Service应该写业务代码所以把这些下沉到Mapper层。
* Android的问题：我记得我跟你说过Activity的逻辑要简明，要把Screen的UI放在`com/magicvector/ui/view/activity`，不然Activity的代码逻辑太大了不好处理。就比如ComposeLoginActivity的LoginScreen拆分出去。
  你写的其他Activity也要，并且这条规则你看看Android中[developAndRules.md](app/android/docs/developAndRules.md)有没有，没有就记录。
* 还有为了方便维护请求和响应体，你可以直接把SpringBoot的dto直接复制到Android项目中做一些微调，比如spring的代码有@data，Android没有，所以Android就都用public。方便维护。
* Android问题，严重！！！：不要创建UserDatabase，Android中只能有一个数据库：Vector数据库，User只是其中一个表，不要专门去设计一个UserDataBase
* ComposeRegisterActivity的获取权限报错，是我说错了，不是使用PermissionUtils，那个是提供给AndroidX的，我现在新封装了ComposePermissionUtils用这个，修复Bug。

### 本次的修改也要更新到cursorDevelopLog，新增规则也要更新到SpringBoot的[developAndRules.md](springboot/docs/developAndRules.md)和Android中[developAndRules.md](app/android/docs/developAndRules.md)
* 注意我新增要求的两个绘图：数据库的和线程的，并且如果你觉得其他比较重要的UML图我没有涉及到你可以绘制出来。并写将其写在：[ProjectAndMainRule.md](ProjectAndMainRule.md)这个规则写在这里是因为我觉得这是整个项目公用的而不是属于SpringBoot或者Android的。
* 你用到了哪些设计模式以及为什么用这些设计模式也可以写道cursorDevelopLog中，并把这条规则记录到[ProjectAndMainRule.md](ProjectAndMainRule.md)
* 我希望学习一些计算机理论, 如果涉及到核心的`操作系统(线程, IO)`, `计算机网络`, `数据结构`, `算法`, `计算机组成原理`, `数据库`的知识你要标注出来. 并把这条规则记录到[ProjectAndMainRule.md](ProjectAndMainRule.md)


### 继续调整
* 之前说绘制各个线程的状态图是我说错了，应该是各个线程的甘特图，我已经添加规则到主规则，现在稍微修改一下图：多线程设计要画出线程的甘特图，表示在不同时间各个线程的执行顺序、线程状态以及线程锁。
* 我审核代码发现你把数据库的表id竟然使用string，这明显会降低数据库的排序性能。你要做修改改为long，并在[ProjectAndMainRule.md](ProjectAndMainRule.md)
  中加入此规则，并稍微说明原因，用`数据库`的理论原理来说明。并且记录每个表的主键必须叫做id。所以引申出Android的UserEntity必须改.
* `AccessToken`的验证逻辑我觉得有问题，我认为验证accessToken应该跟userId（后端分配的）强相关，所以重新设计，而且现在我要求你用uml图展示accessToken内部原理，
  用`通讯图`和`活动图`来绘制




# AI自动化开发流水线设计

介绍：

## 规则集
* 自动读取规则集：总规则集，模块规则集
* 根据我的需求自动修改补全规则集
主要规则集在[ProjectAndMainRule.md](ProjectAndMainRule.md)
内部包含了整个项目cursor开发的时候需要注意的规则集。


## 设计图与开发日志
* 自动读取设计图设计文档
* 自动续写设计图设计文档
现在新增一个`MainDesignDocument.md`，内部包含了整个项目的各个模块的设计文档，
我的初步构思是这样的，我只维护整个设计文档和代码审核，不参与任何的代码开发。

然后你现在需要把cursorDevelopLog的以实现的功能迁移到对应的设计文档中。相当于就是已经开发实现的功能了。

## 知识库
* 相关知识自动记录到Knowledge知识库

我现在不仅在设计整个项目，我还在积攒我的计算机知识相关的知识库，
如果有什么重要的知识我希望后续的cursor能记录到[学习笔记.md](学习笔记.md)
不是现在让你记录而是让你写一条规则在[ProjectAndMainRule.md](ProjectAndMainRule.md)
能让后续的cursor把我说的重要知识记录到[学习笔记.md](学习笔记.md)，规则要写我让记录再记录，不让记录的时候不要记录。


所以你现在要做的事情是：1.整理我上述说的到规则集，2.把你之前写的cursorDevelopLog日志迁移到[MainDesignDocument.md](MainDesignDocument.md)
内部对应的模块。3.在规则集中取消使用cursorDevelopLog，替换的是读取，设计，写入[MainDesignDocument.md](MainDesignDocument.md)




### 修改设计

SpringBootDesignDocument存在问题：

首先一般的SpringBoot项目只有大的四层：1.Controller 2.Service 3.Mapper 4.Domain
一般**鉴权子系统**是放在SpringCloudGateway中的，但是我这个项目在那时不希望加上。
你就写一个拦截器吧，然后写一个配置类，可以配置哪些路由需要鉴权，而且这个项目暂时不用Redis，
JWT无状态校验又不能踢人，所以设计就按照现在简单的Map。

第二，你现在要在规则中写入，包括主规则、Android、SpringBoot。Domain之间的转换需要使用Converter，
如果是Android就用接口实现，如果是SpringBoot就用MapStruct。
参考C:\CodeLearning\magic-vector\springboot\open-api\src\main\java\com\openapi\converter
现在需要你修改你之前做的功能的类型转换，不是你做的先不用改。

第三是参数校验校验, 我以前用过Spring 基于 `jakarta.validation-api`（原 `javax.validation-api`）
你现在加上，并把注解校验这个规则写道spring开发规则。
异常处理会在全局异常拦截处理。这条也写在开发规则，并说明异常类型写在com/openapi/domain/constant/error


第一，formdata的不能这样校验，写入规则集，然后回滚/user/register
第二，修改SpringBoot还得同步Android的请求对不对你可以读AndroidDesignDocument并修改这个文档和代码。



### 新功能开发、修改

大纲是这样的：
#### Agent
Agent
* 创建Agent
* 查看，修改，删除Agent
  AgentList
* 选择Agent
* 接收Agent消息


其实大部分我的功能已经完成，
你现在看如果已经完成是否分别符合[ProjectAndMainRule.md](ProjectAndMainRule.md)，
[developAndRules.md](springboot/docs/developAndRules.md)
和[developAndRules.md](app/android/docs/developAndRules.md)

现在我详细说明要开发什么：
用户交互的main界面会展示三个navigation，分别是Agent（聊天Agent相关），Control（设备状态操作监控），Mine（我的）
详情你想了解可以看[MainDesignDocument.md](MainDesignDocument.md)可以只是了解，因为本次我不会让你全部开发。

##### 创建 Agent
- 如果一个 Agent 都没有，页面中间显示一个大大的创建按钮, 有Agent之后按钮不显示, 变为agent列表
- 点进去要填：头像、名称、设定（提示词）
- 填好后请求后端保存，头像文件用 MinIO 存（我没实现SpringBoot的minio配置，可以写todo，然后创建可以不传递头像也可以请求）
- 保存成功后通过 AgentManager 更新本地缓存，列表里就能看到了
其实我再想要不要不用创建Agent的Activity了因为现在是Jetpack Compose开发很方便，
而是创建Agent的Compose组合函数，放在fragment碎片(不是真的fragment，现在我用Jetpack都使用组合函数了
参考app/android/app/src/main/java/com/magicvector/fragment)下面有自己的vm
我希望点击创建按钮还是在Main页面就不用跳转新的activity了，然后组合函数UI弹性放大占满屏幕。内容我已经写好了你可以参考
[ComposeCreateAgentActivity.kt](app/android/app/src/main/java/com/magicvector/activity/ComposeCreateAgentActivity.kt)
然后点击创建收到响应或者用户点击返回要弹性缩小。这样还能免除使用activity返回值，而是使用全局事件比如eventbus直接在agentList展示创建好的agent。
当然我觉得eventbus在jetpack compose是兜底方案，你还是不要这么实现，写到设计文档吧，分析一下在compose的框架下用ViewModel 中用 SharedFlow/StateFlow是不是更合适，并实现。

##### 查看，修改，删除Agent
- 跟创建Agent的组合函数Page基本一致，只不过能编辑，底下能删除，要弹出确认是否删除等。
- 其实创建就是增加，Agent详情页面就是查看，修改，删除。
- 打开和关闭的逻辑和UI跳转方式跟创建 Agent一致，都是用组合函数动态弹性打开和动态弹性关闭。
- 这里也要注意用jetpack compose的ViewModel，用SharedFlow/StateFlow去控制AgentList上的UI变化。

##### 选择Agent与接收Agent消息
其实我基本已经实现了，你参考一下跳转[ComposeChatActivity.kt](app/android/app/src/main/java/com/magicvector/activity/ComposeChatActivity.kt)的逻辑
目前只要做一些小的修改。
我觉得选择聊天的逻辑相对复杂，所以选择Agent就是点击AgentList上的item然后跳转到ChatActivity，此处需要创建Activity。
跳转到ChatActivity之后不要忘记接收其他Agent的消息，在背后线程也要更新UI消息，特别注意线程管理避免直接new而是使用协程或者线程池。
ChatManager需要设计一下，这个是用来管理跟所有Agent的绘话与聊天的。当你在ChatActivity收到其他的消息，虽然Main页面的AgentList不可见
但是你要用SharedFlow/StateFlow或者最次使用eventBug去更新数据。然后展示出来。
ChatActivity的本次不需要开发，直接用我的ComposeChatActivity

对了还需要做缓存，就是在没有网络的情况下，能看到之前的消息，所以你需要把所有的Agent消息存储在Room，
关于头像，我认为直接使用Glide缓存就好。
所以你需要设计Room表存储AgentList中的全部消息。至少包括Agent表，ChatMessage表，映射每条消息是属于哪个Agent的。
由于ChatActivity要有上拉加载聊天记录的功能（本次可以先不开发）所以你需要设计根据根据某个时间节点往前或者往后找n条跟某个agent的消息的功能。
这个dao我觉得稍微难设计一点，关于设计sql我觉得你还是要写在设计文档。当然SpringBoot的Mapper也要完成跟Android的Dao一样的接口功能。
要特别注意聊天消息的索引设计，能让我快速实现上述功能。设计文档中绘制ER图，然后看看SQL函数的UML图怎么绘制合适？

然后要做网络请求缓存，我已经设计了一部分，就是首次打开[MessageListPage.kt](app/android/app/src/main/java/com/magicvector/fragment/MessageListPage.kt)
这个页面的时候才去网络请求。由于我是使用了ws所以本次需要监控ws的状态，用Android的系统级别广播监听网络状态，
设计一个`NetworkManger`把，专门监听和管理，绘制类图，状态图，活动图，通信图，甘特图。
如果之前是连接现在断开，然后再次连接的话，就再次主动get请求，否则MessageListPage只在初始化请求一次，其他的数据都又ws更新，
在没有数据的时候都由SQLite展示。
制作`ChatController`吧，管理各种数据源和所有Agent消息的组合，数据源稍微有点多：
首次和断开从连的数据来自于Http，
离线的时候来自于SQLite（Room）
在线时候来自于ws
我的建议是安全线程Map管理<agentId, `ChatManager`>；
chatManager是管理单个Agent的消息，内部有一个顺序排列的List，还有消息插入方法，
消息插入要支持单个和批量的二分插入，因为在有序list的情况下二分插入是最快的，这一点你需要写道设计文档。
`ChatController`和`ChatManager`要绘制类图，对象图，状态图，活动图，通信图，甘特图（首次、断开从连、离线、在线全流程）。
还要绘制整个Main页面的线程甘特图，内部线程设计要合理，包括UI线程，数据库IO线程，网络请求IO线程以及其他管理的线程等。
其他图要根据DesignDocument.md之前的图进行绘制，

#### 任务

你完成任务的顺序：先写设计文档，把新增的功能或者已经开发的功能（需要审核是否规范）写入设计文档：
[MainDesignDocument.md](MainDesignDocument.md)（如果有，这里大部分是架构设计，应该没什么要写的）
[SpringBootDesignDocument.md](springboot/docs/SpringBootDesignDocument.md)（这里是SpringBoot设计）
[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)（这是Android的设计文档）

写完方案之后开发SpringBoot和Android。


### 重新审核

看看AndroidDesignDocument和SpringBootDesignDocument
你不应该在最底下加入本次开发的新的内容，而是把本次开的涉及到的内容插入到对应模块中，就比如说：
AndroidDesignDocument中：
1. `### 架构类图`需要更新。
2. 两个E-R图是不是应该合并？就算不合并是不是应该放在`本地数据库设计（Room）`中？
3. ChatController + ChatManager的设计是不是应该放在`Manager管理类设计`中？是不是要分别像之前的设计一样设计静态UML图(类图)和动态UML图？本次稍微复杂, 还需要对象图, 对象图是ChatController对ChatManager的管理.
顺便一提, 之前的`通信图`应该属于的是动态UML图, 调整一下.
4. 本次修改新增这么多UI, 你应该在`## 页面模块设计`中新增啊, 包括activity级别的和组合函数级别的(姑且称为fragment, 自定义view不是fragment. 能聚合别的自定义组合函数并且拥有自己的vm甚至包含业务逻辑的view我称之为fragment)各种图(MVI 类图,MVI 通信图,活动图,时序图,功能甘特图)是不是也需要跟之前一样?大概UI设计和交互逻辑是不是要写?
5. 网络接口是不是要合并? `## 网络接口契约（Auth API）`的每个接口最好写上大致功能
6. 其他的DAO和Domain是不是也要合并? 不要以本次开发单独领出来, 而是要你根据之前的设计文档在其应该的位置插入新功能.

SpringBootDesignDocument也要跟上述的一样合并, 缺少的就补上.

我说的这些你需要总结到对应的developAndRules中,让你下次开发完写文档的时候能记住怎么写
总之本次的意图就是让你的设计文档要目录化,结构化,戒掉日志化的习惯


### 代码审核

#### SpringBoot
* `/chat/getByAnchor`这个方法需要修改
设计的接口不传递文件就不使用FormData类型数据，应该传递xxxRequest。内部使用校验。你难道没看`SpringBoot developAndRules`[developAndRules.md](springboot/docs/developAndRules.md)吗？
而且不要使用比对字符串`direction`，要使用布尔值。
#### Android
* NetworkManager我认为是全局需要，不仅仅是MainActivity，所以应该放在Application中。
* 如果你认为你MainEffect.LaunchCreateAgent完成的很好了，就是不用再使用新的创建Activity之后应该删除这个`effect`
* 根据Android的设计规则[developAndRules.md](app/android/docs/developAndRules.md)
  Activity页面不做复杂的UI设计，UI要拆分到 `com/magicvector/ui/view/activity`，Activity仅保留编排逻辑（导航、effect监听、权限触发等）。
  所以很明显你忘记拆分Activity的UI了，不要把UI耦合在Activity，太重了。
* 我看你的NetWorkManager中只监听了网络变化，但是我忘了告诉你了Ws的变化也要属于网络变化，你就写Websocket变化吧。是我没说完整，现在Websocket变化跟网络变化的逻辑基本一致，
  需要写到Android设计文档以及在代码中实现。
* （超级重点）我写的ChatMapController，ChatController，ChatManager可能不完善存在问题，你根据设计文档看看是否完善，逻辑是否周密严密，设计是否合理符合Mvi？
  是否高效高性能是否存在内存泄漏的风险。这些都需要你审核和优化。这个可能是本项目最难的地方。毕竟那么多数据源。包括二分插入方法是否可靠？都要好好思考，
  不可以的化就优化。



### 重构长连接逻辑

梳理一下我的Android和SpringBoot的Agent聊天的长连接方式，
好像是打开ChatActivity才调用RealTimeChatController。梳理逻辑，最好画出当前的`通信图`，我看看当前的通信状况是真没样子。
我现在希望知道是什么时候创建长连接，好像现在是打开chat之后用agentId建立？这样建立我感觉不对，
应该改成登陆成功就建立user和server的长连接，然后agentId作为channelId去聊天获取数据。
现在需要你重构：
1. 思考整体最优设计，你有权利可以考虑重写整个代码。
2. 改为登录成功之后就尝试进行去ws长连接，连接建立是用userId而不是agentId。agentId作为路由入参，相当于channel，需要改Android和SpringBoot。
3. 优化后的逻辑要能够跟整体业务兼容。就比如说在打开agent1的chatActivity的时候，RealTimeChatController更新了数据，SharedFlow/StateFlow去更新MainActivity中Fragment的UI。
4. 讲你新设计的逻辑功能归类添加到Android[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)和
   SpringBoot[SpringBootDesignDocument.md](springboot/docs/SpringBootDesignDocument.md)的文档中。
   要绘制出：静态UML：类图，对象图；动态UML：活动图，时序图，功能线程甘特图，通信图。
   要写你用了哪些设计模式比如工厂模式等。
5. ws要添加SpringBoot和Android的心跳连接以及60秒未心跳的断连判断以及NetworkManager中的ws断开重连机制。


### 补充
* 心跳请求是自己写吗？我记得SpringBoot和Android都有直接支持的啊？如果没有就算了，你去查一下资料，
  我这里面Android和SpringBoot使用的ws对应的框架是否直接自己直接配置心跳而不用自己写。如果存在就改为框架的心跳，如果不存在就算了，就这样吧。
* WS长连接的网络状态图也要绘制，当然你可以更新到已有的网络状态图中。在Android[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)和
  SpringBoot[SpringBootDesignDocument.md](springboot/docs/SpringBootDesignDocument.md)的文档中。
* 另外Android的RealTimeChatController我当时设计的时候内部含管理其他的东西，比如AudioRecord，VadDetection，还有各种Controller和Callback要画在类图中。
  我看你只补充了之前的打的类图，并没有绘制RealTimeChatController内部的类图，对象图，状态图，活动图，时序图，功能线程甘特图，通信图。
  你需要补充一下。
* 绘制完成之后你要分析RealTimeChatController内部设计是否合理和耦合，如果不合理直接写在设计文档的Manager下面的RealTimeChatController模块下写todo，
  写为什么不合理，要怎么重构。因为我之前是面向agent，现在是面向user肯定会有改动的。



### 重构AgentChat功能

#### Chat
* 视图：
  * 文本Chat视图
  * Call唤醒视图 + emoji表情视图
* 当前的前置摄像头状况

现在要重构整个ComposeChatActivity。

##### UI设计
现在UI改成：
一个Activity，顶部有两个小圆点，可以左滑右滑切换Fragment（Compose中的组合Fragment函数，放在[fragment](app/android/app/src/main/java/com/magicvector/fragment)）
左边的组合函数fragment是一个纯黑的页面，中间两个白色的眼睛，逻辑几乎可以参考：[ComposeAgentEmojiActivity.kt](app/android/app/src/main/java/com/magicvector/activity/ComposeAgentEmojiActivity.kt)
只不过现在横屏改为了竖屏，逻辑几乎不变。
然后底部有一个小圆圈，在未连接和断开等状态是灰色的，异常是红色的，用户语音还清之后是绿色的，
用户正在说话是蓝色的，Agent正在回复的紫色的。
这个小圆圈要在唤醒的时候弹性变大，然后Agent回复完毕之后弹性还原。
逻辑大概可以参考[voice_agent_page.dart](demo/flutter/flutternew/lib/page/voice_agent_page.dart)
我在flutter中实现过demo。可以不用实现，但是你得把设计稿和设计图给我：[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)
反正就是flutter的这个也页面逻辑voice_agent_page和ComposeAgentEmojiActivity的基本逻辑组合一下作为第一个Fragment：
这两个fragment都需要vm，都放在[fragment](app/android/app/src/main/java/com/magicvector/viewModel/fragment)
都需要mvi设计模式的，你可以参考之前的设计。
AgentEmojiFragment放在[fragment](app/android/app/src/main/java/com/magicvector/fragment)

第二个fragment是原先的[ComposeChatActivity.kt](app/android/app/src/main/java/com/magicvector/activity/ComposeChatActivity.kt)
这个逻辑，两个fragment都在同一个ComposeChatActivity，这样把，为了让你参考原先的ComposeChatActivity逻辑
你新写的Activity要叫做`ComposeAgentChatActivity`
要看设计文档，思考如何利用已有的ReatimeChatController，ChatController，ChatManager，ChatCacheController做好ws消息插入、持久化等。还要画好ChatActivity的各种UML图。

我其实原先的逻辑基本设计的差不多了，你要合并并写在设计图中，
SpringBoot的要写在[SpringBootDesignDocument.md](springboot/docs/SpringBootDesignDocument.md)
Android的写在[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)
这里面的规则都要遵守
都要按照规则[MainDesignDocument.md](MainDesignDocument.md)[developAndRules.md](springboot/docs/developAndRules.md)[developAndRules.md](app/android/docs/developAndRules.md)

其实这里的逻辑是本项目最难的，你需要绘制很多设计图：
Android：
`ChatService`存放ws的，
  * 类图，对象图，状态图，活动图，时序图，功能线程甘特图，通信图
  * 设计模式
* `AudioController`，上述相同
* `UdpVisionManager`，像后端以UDP发送视频流的方法，上述相同。不必特别详细，因为我后续回改为RTMP
* Agent表情Manager：
  * `VisionManager`，上述相同
  * `EyesMoveManager`, 上述相同
  * `TargetActivityDetectionManager`, 上述相同

如果你觉得比较难，可以先画出设计图，然后再按照你的设计图设计代码。


### 补充

我怎么看你就写了一些Android的逻辑，你有检查SpringBoot[open-api](springboot/open-api)的逻辑吗？
而且我让你要绘制Android和SpringBoot的设计图以及设计文档你怎么没绘制。
继续完成我刚刚说的任务：完成的内容都要绘制UML图，而且按照我刚刚跟你说的进行绘制。
SpringBoot的这块逻辑主要在[RealtimeChatServiceImpl.java](springboot/open-api/src/main/java/com/openapi/service/impl/RealtimeChatServiceImpl.java)
你根据上下文设计类图，对象图，状态图，活动图，时序图，功能线程甘特图，通信图，设计模式。
如果你忘了刚刚的任务我再说一遍：
```text
现在UI改成：
一个Activity，顶部有两个小圆点，可以左滑右滑切换Fragment（Compose中的组合Fragment函数，放在[fragment](app/android/app/src/main/java/com/magicvector/fragment)）
左边的组合函数fragment是一个纯黑的页面，中间两个白色的眼睛，逻辑几乎可以参考：[ComposeAgentEmojiActivity.kt](app/android/app/src/main/java/com/magicvector/activity/ComposeAgentEmojiActivity.kt)
只不过现在横屏改为了竖屏，逻辑几乎不变。
然后底部有一个小圆圈，在未连接和断开等状态是灰色的，异常是红色的，用户语音还清之后是绿色的，
用户正在说话是蓝色的，Agent正在回复的紫色的。
这个小圆圈要在唤醒的时候弹性变大，然后Agent回复完毕之后弹性还原。
逻辑大概可以参考[voice_agent_page.dart](demo/flutter/flutternew/lib/page/voice_agent_page.dart)
我在flutter中实现过demo。可以不用实现，但是你得把设计稿和设计图给我：[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)
反正就是flutter的这个也页面逻辑voice_agent_page和ComposeAgentEmojiActivity的基本逻辑组合一下作为第一个Fragment：
这两个fragment都需要vm，都放在[fragment](app/android/app/src/main/java/com/magicvector/viewModel/fragment)
都需要mvi设计模式的，你可以参考之前的设计。
AgentEmojiFragment放在[fragment](app/android/app/src/main/java/com/magicvector/fragment)

第二个fragment是原先的[ComposeChatActivity.kt](app/android/app/src/main/java/com/magicvector/activity/ComposeChatActivity.kt)
这个逻辑，两个fragment都在同一个ComposeChatActivity，这样把，为了让你参考原先的ComposeChatActivity逻辑
你新写的Activity要叫做`ComposeAgentChatActivity`
要看设计文档，思考如何利用已有的ReatimeChatController，ChatController，ChatManager，ChatCacheController做好ws消息插入、持久化等。还要画好ChatActivity的各种UML图。

我其实原先的逻辑基本设计的差不多了，你要合并并写在设计图中，
SpringBoot的要写在[SpringBootDesignDocument.md](springboot/docs/SpringBootDesignDocument.md)
Android的写在[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)
这里面的规则都要遵守
都要按照规则[MainDesignDocument.md](MainDesignDocument.md)[developAndRules.md](springboot/docs/developAndRules.md)[developAndRules.md](app/android/docs/developAndRules.md)

其实这里的逻辑是本项目最难的，你需要绘制很多设计图：
Android：
`ChatService`存放ws的，
  * 类图，对象图，状态图，活动图，时序图，功能线程甘特图，通信图
  * 设计模式
* `AudioController`，上述相同
* `UdpVisionManager`，像后端以UDP发送视频流的方法，上述相同。不必特别详细，因为我后续回改为RTMP
* Agent表情Manager：
  * `VisionManager`，上述相同
  * `EyesMoveManager`, 上述相同
  * `TargetActivityDetectionManager`, 上述相同
```


### 控制台开发

#### Control
* 设备状态操作监控
  * RK与App连接状态（蓝牙，Wifi）
  * RK与SpringBoot连接状态
  * App与SpringBoot连接状态
  * RK的Agent选用状态
* 云操控平台(Live)
  * 向SpringBoot发送请求指令
  * 接收Nginx的Live推流
    * 另一台设备的Camera信道（Nginx）测试
    * 视频录制保存本地
* 离线蓝牙、Wifi操控
  * BLE蓝牙连接并发送指令
  * 连接RK创建的WIFI发送指令
  * Agent指令控制台输出

#### Mine
* Setting（本次不实现，写todo）
  * 修改密码
  * 登出
* 视频
  * 云上录播记录播放（本次不实现，写todo）
  * 本地视频播放（本次实现）
  * 本地视频上传云端（本次不实现，写todo）

现在我要实现SpringBoot,Android,RK三端互通，
因为暂时没有RK代码，跟RK相关的你都可以写todo.


##### UI 设计
MainActivity的第二个Fragment页面。

* 设备连接状态
顶部显示设备的连接状态，最好要有看上去还不错的UI：
参考我刚刚说的`* 设备状态操作监控`
下面就是显示几个按钮：
（云操控平台）至少需要SpringBoot和Android连接。
（离线操控平台）至少需要RK和Android连接。

* 云操控平台
首先是一个视频view用来展示从RTMP拉的流，你可以选择SurfaceView或者什么，反正希望性能好些。
视频源：可以选择通过（RK\Android + RTMP + FFmpeg + Nginx不走SpringBoot推拉流）和
（Android或者RK传输裸UDP帧给SpringBoot，SpringBoot用Netty接收转发给另一个App）这两个选项。
低下是一些指令按钮，你就暂时先设计两个手柄的拖拽，左边是用于操控方向，右边是操控移动。你可以参考switch游戏手柄。
你要设计通信的数据结构，这些是通过ws传递给SpringBoot。当然SpringBoot也要设计将这些指令通过ws或者mqtt交给RK，可以先写todo。
关于RTMP拉流，这个我自己来实现吧，你写todo。
关于推流，因为暂时没有RK代码，所以你还是写todo吧。
要设计合理的状态显示UI与重连显示UI。

** 还需要一个个App推拉流的测试功能，点击测试之后UI页面变为选择推流或者拉流。
推流是：输入RTMP Url + 推流按钮 + 视频View
拉流是：输入RTMP Url + 拉流按钮 + 视频View

* 离线操控平台
Android可以通过连接RK创造的WIFI进行视频流传输和指令传输。
UI基本跟云操控平台一致。
视频view直接展示UDP流，然后操控跟云平台基本一致，甚至你可以封装自定义view然后复用。
如果选用BLE蓝牙连接的话，就不能看实时视频，但是遥感操控和命令按钮基本一致。
要设计合理的状态显示UI与重连显示UI。

** 视频录制保存本地
在云操控平台或者离线操控平台都要有的按钮，点击UI就变成正在录制，显示录制时长。
不采用系统录屏，而是想办法把接收的流变为Mp4，在线的话接收的是H264，离线的话接收的是UDP裸流。

** Mine中的视频播放本地视频

总体设计要能跑通，
我需要你看目前已有的设计
[MainDesignDocument.md](MainDesignDocument.md)
[SpringBootDesignDocument.md](springboot/docs/SpringBootDesignDocument.md)
[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)
并把新增的设计设计出来再去写代码。
你还需要遵守开发规范：
[developAndRules.md](app/android/docs/developAndRules.md)
[developAndRules.md](springboot/docs/developAndRules.md)
上述的开发设计要写道设计文档，插入到应该属于的地方，要参考之前怎么写的

要设计合适的Page并设计Mvi设计模式的viewmodel，并绘制跟参考文档中其他page一样的UML图。
要设计合理的Mangaer和Controller，并绘制：类图，对象图，状态图，活动图，时序图，功能线程甘特图，通信图
要设计合理的ws和http接口，并写大概功能
需要设置合理的缓存机制，比如说离线的时候能狗播放本地视频。
包括一些可选方案比如推流是使用RTMP还是UDP，也要写在设计文档。

然后再写代码，我认为首先要设计好，才能写代码。
本次任务较难的大部分都是音视频开发，springboot只是下发指令和长连接，给RK设置Agent等简单的接口。当然如果用UDP推流还有部分逻辑。
总体难度在Android端。总之好好设计，然后写代码。




### Mine
接着完成整个App最后一个功能Mine

#### Mine
* Setting
  * 修改密码
  * 登出
* 视频
  * 云上录播记录播放
  * 本地视频播放
  * 本地视频上传云端

就跟正常的app一样，显示头像下面是大的UserAccount，
有Setting按钮和视频按钮。
Setting的业务逻辑暂时就那两个很简单，我都懒得绘制任何功能相关的uml了，随便设计设计。
重点在下面，视频模块。
首先SpringBoot要提供从minio获取视频源然后转为m3u8的方法，这部分可能要使用什么sdk或者依赖，先写todo，
然后Android这边可就应播放器来播放这些视频。

第二个比较简单，就是直接播放本地的MP4，用什么sdk就可以。

第三个是本地视频上传云端，要支持上传和下载，服务端使用，minio。要支持断点续传。

其实这一部分代码的业务逻辑很少，主要是让你给出可行方案以及设计Android的Manager和Android的Controller并给出方案和UML图。

这次开发的需要一样
总体设计要能跑通，
我需要你看目前已有的设计
[MainDesignDocument.md](MainDesignDocument.md)
[SpringBootDesignDocument.md](springboot/docs/SpringBootDesignDocument.md)
[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)
并把新增的设计设计出来再去写代码。
你还需要遵守开发规范：
[developAndRules.md](app/android/docs/developAndRules.md)
[developAndRules.md](springboot/docs/developAndRules.md)
上述的开发设计要写道设计文档，插入到应该属于的地方，要参考之前怎么写的

要设计合适的Page并设计Mvi设计模式的viewmodel，并绘制跟参考文档中其他page一样的UML图。
要设计合理的Mangaer和Controller，并绘制：类图，对象图，状态图，活动图，时序图，功能线程甘特图，通信图

#### 补充

上次的`Agent指令控制台输出`我是不是忘了说实现了，就是在云操控平台需要在下面创建一个Agent输出日志view，
然后后端返回的AgentJson指令要显示出来。SpringBoot也要配合设计。给出UML的设计图，种类跟之前要求一样。
对了最好还需要日志，这个日志是能够存储Android的Room的以及存储SpringBoot的Mysql的，要有（id, user_id, agent_id, log_time, log_content）
所以要设计在线的查询http接口以及离线的查询dao接口。

上次的离线操控平台我不记得我跟你说了没有，RK要创建wifi给Android发送摄像头的udp帧，Android这边要显示在视频view。我不记得实现没有。检查一下。

#### 关于可行方案
这部分加上我上次给你的操控平台的解决方案写在设计文档中。

这个项目目前还有较多的为实现，因为涉及各种sdk所以我不要求你实现，但是我希望你去查资料，给出可行方案：

我已经创建了[RKDesignDocument.md](rk/docs/RKDesignDocument.md)
我的RK需要烧入系统级别App（我还没写）用于跟我现在的App和SpringBoot进行通讯。
关于RK，我还没选好芯片，RK3566?RK3588?我的需求就是部署Android系统APP进行操控，以及能通过JNI操控cpp然后操控GPIO引脚操控sg90舵机运动，
要有摄像头，并传输摄像头数据，我已经实现Android系统App的silero VAD（TensorflowLite）和YOLOv8，我希望能成功部署RK，我不要求有多高的性能其实跟手机差不多就行。
帮我选用芯片并写入设计文档。

下面的可行性方案分析要求：方案给出可行性分析以及方案大概设计，最好能给出可行方案的网上的文档链接。
要求设计给出`大概的类图`，`大概通信图`，`甘特图`，`活动图`，`状态机图`（大概是因为还不确定，但是我觉得甘特，活动，状态机图可以直接确定）

方案我希望你使用FFmpeg，RTMP，OpenGL甚至OpenCV等技术实现我提出的音视频方案。

##### Control
* 设备状态操作监控
  * RK与App连接状态（给出连接状态匹配方案，RK和Android进行wifi连接，BLE蓝牙连接的）
  * RK与SpringBoot连接状态（给出和SpringBoot进行Ws，Mqtt连接的方案，以及http请求）
* 云操控平台(Live)
  * 接收Nginx的Live推流
    （我已经实现demo，基本上就是用nginx[nginx-rtmp-win32-dev](nginx-rtmp-win32-dev)）
     我目前推流设计基本已经完成，并且我测试通过[LiveActivity.kt](demo/cpp/app/src/main/java/com/demo/cpp/activity/LiveActivity.kt)
     大概是用jni + RTMP实现的，你可以参考。拉流我没实现。我是用Windows的VL player验证成功的。
    * 另一台设备的Camera信道（Nginx）测试（参考上述我说的给出方案就行）
    * 视频录制保存本地（给出方案）
* 离线蓝牙、Wifi操控
  * BLE蓝牙连接并发送指令（给出方案包括连接，连接状态，指令发送与接收，RK下发GPIO操控移动）
  * 连接RK创建的WIFI发送指令（给出方案包括）
  * WIFI下Android监控当前RK的Camera（给出方案，包括RK创建WIFI，UDP发送以及Android展示）

##### Mine
* 视频
  * 云上录播记录播放（给出SpringBoot把视频文件拆分为m3u8的方案，以及给出url让Android拉流的方案）
    （给出Android把已有的m3u8数据源整合下载成.m3u8文件或者mp4的方案）
  * 本地视频播放（给出Android本地播放mp4方案）
  * 本地视频上传云端（给出将mp4上传，断点续穿给SpringBoot的以及下载MP4的方案）

文档全部写完才允许写代码，我认为首先要设计好，才能写代码。（RK不用写代码暂时）



### Fix

你修复的bug要写入到[CursorBug日志.md](CursorBug日志.md)

#### 1. 打开页面无法创建Agent
你看[MainDesignDocument.md](MainDesignDocument.md)里面包含Android设计文档，
看到Android设计文档的##### Agent 列表页面（MessageListScreen）
**布局结构**：
- 空状态：中心提示 + 创建按钮
- 非空状态：Agent 列表 + 创建 FAB
我看好像并没有实现，我没有看到提示`创建Agent`的按钮，显示是`当前暂无消息`，
我觉得的状态机错了，我觉得需要两个状态值来管理：是否有Agent和是否有消息。这两个值共同来管理UI界面。重构一下这部分的设计文档和代码（添加状态机UML图，更新其他相关UML图）。
当前逻辑在[MainActivity.kt](demo/app/app/src/main/java/com/magicvector/demo/activity/MainActivity.kt)
你认真看设计文档并做修改，如果修改改动了设计架构要写在设计文档中。

当然我还能提供你的信息：
```shell
                                                                                                    <---- Response (27.184716ms)
2026-03-14 10:43:11.986  7947-7980  com.core.b...nterceptor com.magicvector                      D  Response URL: http://192.168.1.2:48888/agent/getLastAgentChatList?userId=2032466744930009088
2026-03-14 10:43:11.986  7947-7980  com.core.b...nterceptor com.magicvector                      D  Status Code: 200
2026-03-14 10:43:11.986  7947-7980  com.core.b...nterceptor com.magicvector                      D  Response Headers: Vary: Origin
                                                                                                    Vary: Access-Control-Request-Method
                                                                                                    Vary: Access-Control-Request-Headers
                                                                                                    Content-Type: application/json;charset=UTF-8
                                                                                                    Content-Length: 52
                                                                                                    Date: Sat, 14 Mar 2026 02:43:11 GMT
                                                                                                    Keep-Alive: timeout=60
                                                                                                    Connection: keep-alive
2026-03-14 10:43:11.993  7947-7980  com.core.b...nterceptor com.magicvector                      I  
                                                                                                    
                                                                                                    <---- ResponseBody: 
                                                                                                    {
                                                                                                      "code": "C_10001",
                                                                                                      "message": "参数错误、不全"
                                                                                                    }
```
去其实觉得Android和SpringBoot的接口对接是错误的。你检察一下

#### 2. Mine页面布局错误
你去看Android开发文档的### Mine 模块（Setting + 视频）
我看你页面有问题，是不是我描述的有问题：
```markdown
功能职责
* 顶部显示头像与大号 `UserAccount`，下方提供 `Setting` 与 `视频` 两个一级入口。
* **Setting**：提供修改密码、登出（业务简单，轻量实现）。
* **视频**：
  * 云上录播记录播放（服务端 MinIO 视频源转 m3u8，Android 播放）
  * 本地视频播放（MP4）
  * 本地视频上传云端（支持断点续传、下载）
```
这个页面的头像和用户信息下面其实就是三个按钮啊，
这三个按钮分别是：设置，视频，测试
你现在是写在同一个页面了，这是不对的，这三个按钮是应该跳转三个不同的ComposeActivity的。
现在需要你重新写设计文档并实现功能。

#### 3.Control页面错误
你找到Android设计文档这一部分：### Control 模块（Main 第二个 Tab）
首先`App-Spring`连接状态就是App登录之后通过userId和SpringBoot进行长连接
`Control WS`我没喊你设计吧？你看设计文档没有吧，现在改为RTMP拉流状态。
还有你要结合看一下Android和SpringBoot的设计文档。
好像长连接经常不稳定，你检查一下：
```shell
2026-03-14 10:52:53.918  7947-8079  ControlConsoleManager   com.magicvector                      E  control ws onFailure
                                                                                                    java.net.SocketException: Socket closed
                                                                                                    	at java.net.SocketInputStream.socketRead0(Native Method)
                                                                                                    	at java.net.SocketInputStream.socketRead(SocketInputStream.java:119)
                                                                                                    	at java.net.SocketInputStream.read(SocketInputStream.java:176)
                                                                                                    	at java.net.SocketInputStream.read(SocketInputStream.java:144)
```
而且我每次跳转到Control页面的时候：Control WS，App Spring连接状态，网络连接状态都会闪绿一下然后闪红。
我怀疑你是MVI的生命周期处理错了。是不是先处理Intent改变UI状态然后onResume初始化覆盖掉了？我不确定你排查一下，
而且有很多类似的闪烁问题：比如云操控平台点击离线BLE就会闪烁出提示然后马上消失。
点击开始录制会闪烁出时间然后马上闪烁出未录制，页面就结束了。修复问题。

#### 4. Control页面操控RK错误
我只要一拖拽遥感，或者是点击前进或者急停按钮就会闪退：
```shell
2026-03-14 11:30:36.649  8119-8119  AndroidRuntime          com.magicvector                      D  Shutting down VM
2026-03-14 11:30:36.652  8119-8119  AndroidRuntime          com.magicvector                      E  FATAL EXCEPTION: main
                                                                                                    Process: com.magicvector, PID: 8119
                                                                                                    java.lang.NullPointerException: Attempt to invoke virtual method 'long java.lang.Long.longValue()' on a null object reference
                                                                                                    	at com.magicvector.manager.control.ControlCommandController.baseRequest(ControlCommandController.kt:64)
                                                                                                    	at com.magicvector.manager.control.ControlCommandController.buildButtonCommand(ControlCommandController.kt:43)
                                                                                                    	at com.magicvector.viewModel.fragment.ControlVm.sendQuickButtonCommand(ControlVm.kt:186)
                                                                                                    	at com.magicvector.viewModel.fragment.ControlVm.processIntent(ControlVm.kt:62)
                                                                                                    	at com.magicvector.fragment.ControlPageKt.ControlScreen$lambda$18$lambda$17(ControlPage.kt:86)
                                                                                                    	at com.magicvector.fragment.ControlPageKt.$r8$lambda$iq98z32ATgeu04qb-YyKghjRkXw(Unknown Source:0)
                                                                                                    	at com.magicvector.fragment.ControlPageKt$$ExternalSyntheticLambda9.invoke(D8$$SyntheticClass:0)
                                                                                                    	at androidx.compose.foundation.ClickableNode.onPointerEvent-H0pRuoY(Clickable.kt:1009)
                                                                                                    	at androidx.compose.ui.input.pointer.Node.dispatchMainEventPass(HitPathTracker.kt:436)
                                                                                                    	at androidx.compose.ui.input.pointer.Node.dispatchMainEventPass(HitPathTracker.kt:422)
                                                                                                    	at androidx.compose.ui.input.pointer.Node.dispatchMainEventPass(HitPathTracker.kt:422)
                                                                                                    	at androidx.compose.ui.input.pointer.Node.dispatchMainEventPass(HitPathTracker.kt:422)
                                                                                                    	at androidx.compose.ui.input.pointer.NodeParent.dispatchMainEventPass(HitPathTracker.kt:275)
                                                                                                    	at androidx.compose.ui.input.pointer.HitPathTracker.dispatchChanges(HitPathTracker.kt:171)
                                                                                                    	at androidx.compose.ui.input.pointer.PointerInputEventProcessor.process-BIzXfog(PointerInputEventProcessor.kt:118)
                                                                                                    	at androidx.compose.ui.platform.AndroidComposeView.sendMotionEvent-8iAsVTc(AndroidComposeView.android.kt:2428)
                                                                                                    	at androidx.compose.ui.platform.AndroidComposeView.handleMotionEvent-8iAsVTc(AndroidComposeView.android.kt:2378)
                                                                                                    	at androidx.compose.ui.platform.AndroidComposeView.dispatchTouchEvent(AndroidComposeView.android.kt:2249)
                                                                                                    	at android.view.ViewGroup.dispatchTransformedTouchEvent(ViewGroup.java:3062)
                                                                                                    	at android.view.ViewGroup.dispatchTouchEvent(ViewGroup.java:2751)
                                                                                                    	at android.view.ViewGroup.dispatchTransformedTouchEvent(ViewGroup.java:3062)
                                                                                                    	at android.view.ViewGroup.dispatchTouchEvent(ViewGroup.java:2751)
                                                                                                    	at android.view.ViewGroup.dispatchTransformedTouchEvent(ViewGroup.java:3062)
                                                                                                    	at android.view.ViewGroup.dispatchTouchEvent(ViewGroup.java:2751)
                                                                                                    	at android.view.ViewGroup.dispatchTransformedTouchEvent(ViewGroup.java:3062)
                                                                                                    	at android.view.ViewGroup.dispatchTouchEvent(ViewGroup.java:2751)
```




### 问题：已有账号信息未跳转

首先你看一下设计文档[MainDesignDocument.md](MainDesignDocument.md)
[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)
[SpringBootDesignDocument.md](springboot/docs/SpringBootDesignDocument.md)

#### 日志
按照设计文档中写的，StartActivity中应该查询Android本地的数据库看看是否有已经登录的账号，如果有，找到token验证，看看是否生效。
但是现在的问题我每次打开App都让我重新登录。
所以我希望你修复这个bug，在Android中加入Log日志：在StartActivity输出数据库中的数据。以及在SpringBoot中配置总的DebugConfig，
然后从配置文件可以配置是否输出log日志。然后再在debug的情况下输出日志。本次要加的日志是token验证的那个post接口。
添加这些日志方便我排查为什么start页面登录成功之后再次登录还是没登陆上。

#### 新增功能
对了现在需要修改设计。登录的时候不仅可以输入，还可以下拉选择数据库中已有的账号，
再新增加账号的密码存储功能，也就是说你现在想需要修改Android的数据库设计，
数据库要新增密码字段。这个密码只有登录成功才存储。下拉选择任何账号的时候如果这个账号存储了密码，那么自动填充。

修复bug的记录要存储在[CursorBug日志.md](CursorBug日志.md)



#### 补充
我发现你没有理解我设计文档中dataState的用途，我给你解释明白之后记得去记录到[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)
dataState就是数据缓存，除了uiState之外的业务数据缓存。

首先你看到ComposeLoginVm的这一行代码：password = state.password
我跟你讲这一行编译器报错了，是编译不通过的。
因为state未定义，但是我跟你讲这个password应该从哪里拿呢？就是uiState的password。

首先会加载数据量，数据库的数据会缓存到dataState；
dataState的设计初衷就是存储跟uiState之外的业务数据，比如从数据库获取的List<UserEntity>就应该放在LoginState
dataState内是允许放一些Entity，Module聚合一个整体的DataState的，但是不允许放DTO，DTO的数据应该单独拆分字段存储在dataState。
很明显数据库的数据是用户不能直接看到的，而且这个数据也不会通过用户的输入改变因为是从数据库获取的，所以不应该存储在uiState中，应该存储在dataState中。

很明显此处userManager.saveCurrentUser的业务逻辑是登录成功存储。
首先初始化加载之后，uiState.password应该选用dataState中的userEntities的第一个，
如果用户输入，dataState的值也不应该被改变，而是改变uiState的password。因为用户可能再次下拉选择，这时候应该把dataState的值再次填入uiState中。
所以userManager.saveCurrentUser的password = uiState.password，
而这个值来自于输入或者下拉选择dataState中的值。
还有就是accessToken是不可见的，不应该写在uiState中，应该存储在dataState中。并且，登录成功会获取userId，这个值也不应该存储在uiState中，应该存储在dataState中。
还有就是，我记得userManager应该存储当前登录用户是哪个，而不是每次都去查询数据库的last，
所以应该在userMapper中缓存一个变量用来存储当前的currentUserSession，如果未null或者isEmpty就再用你写的dao接口查询。



把我跟你说的dataState规则写入[developAndRules.md](app/android/docs/developAndRules.md)
把我说的东西写进设计文档[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)



### 重构App

看到这个[StartActivity.kt](app/android/app/src/main/java/com/magicvector/activity/StartActivity.kt)
现在我希望你封装一个BaseComponentActivity在[activity](app/android/app/src/main/java/com/magicvector/utils/activity)
大概就是把setupFullScreen()的逻辑封装在BaseComponentActivity中，并且onResume()默认直接调用这个方法，
然后把其他的composeActivity切换为继承这个BaseComponentActivity，就实现了代码复用
不准删我任何注释！！！



### 重构handler以及写

现在需要你修改一下android的设计文档[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)
我决定修改一下架构：
[domain](app/android/app/src/main/java/com/magicvector/domain)
domain中存放数据结构，
[dto](app/android/app/src/main/java/com/magicvector/domain/dto)
内部放传输按数据类型。
[entity](app/android/app/src/main/java/com/magicvector/domain/entity)
内部存放数据库类型
[model](app/android/app/src/main/java/com/magicvector/domain/model)
内部存放业务类型
[convertor](app/android/app/src/main/java/com/magicvector/domain/convertor)
这里面是数据类型转换工具
dto，entity，model之间的数据转换需要用convertor


[repository](app/android/app/src/main/java/com/magicvector/repository)
这个存放数据源接口：
[api](app/android/app/src/main/java/com/magicvector/repository/api)
这里面是网络层接口：
其中[ApiRequest.kt](app/android/app/src/main/java/com/magicvector/repository/api/ApiRequest.kt)
这个Retrofit接口
[dao](app/android/app/src/main/java/com/magicvector/repository/dao)
这里面放的是数据库的接口

[dataSource](app/android/app/src/main/java/com/magicvector/dataSource)
这是数据源层，跟repository的区别是里面会包含业务逻辑
我打个比方，看到StartVm的这个代码：
```kotlin
    private suspend fun verifyAccessToken(accessToken: String): Boolean {
  // suspend的定义，线程可以不用等待，可以先去运行其他代码
  // 挂起点1：调用 suspend 函数 getCurrentUser()
  // 协程挂起，直到数据库返回结果，线程不阻塞
  val localUser = userManager.getCurrentUser()

  // 只有挂起点1完成，才会执行到这里
  // suspendCoroutine 是一个 suspend 函数，调用它的瞬间，当前协程就会主动挂起，需要continuation.resume()恢复
  return suspendCoroutine { continuation ->
    if (localUser == null || localUser.userId <= 0L) {
      continuation.resume(false)
      return@suspendCoroutine
    }
    val request = UserTokenVerifyRequest().apply {
      this.userId = localUser.userId
      this.accessToken = accessToken
    }
    api.verifyAccessToken(
      request = request,
      onSuccessCallback = object : OnSuccessCallback<BaseResponse<UserTokenVerifyResponse>> {
        override fun onResponse(response: BaseResponse<UserTokenVerifyResponse>?) {
          val isSuccessCode = response?.code == BaseConstant.NetworkCode.SUCCESS_CODE
          val isValid = response?.data?.valid == true
          // 网络请求发出后，suspendCoroutine 代码块执行完毕，但协程仍处于挂起状态
          // 直到回调里调用 continuation.resume()，协程才恢复
          continuation.resume(isSuccessCode && isValid)
        }
      },
      throwableCallback = object : OnThrowableCallback {
        override fun callback(throwable: Throwable?) {
          // 网络请求发出后，suspendCoroutine 代码块执行完毕，但协程仍处于挂起状态
          // 直到回调里调用 continuation.resume()，协程才恢复
          continuation.resume(false)
        }
      }
    )
  }
}
```
我现在的计划是：
在dataSource创建object，
然后里面有个方法就是verifyAccessToken，入参是accessToken: String和处理方法，叫做handleVerifyAccessToken
这个方法应该从vm中获取，因为对数据结构的处理应该在vm。
然后比如
val localUser = userManager.getCurrentUser()数据获取
参数校验
request组成，都放在这里面，然后各种异常回调，相应处理，以及协程处理都交给handleVerifyAccessToken
相当于是我抽象出来请求，因为请求大部分数据是一样的，各个vm只是做不同相应而已，所以没必要重复在多个vm中写请求。
并且这样做我还能取消[ApiRequestImpl.kt](app/android/app/src/main/java/com/magicvector/repository/api/ApiRequestImpl.kt)
这是一个无意义的类，所以你现在需要实现我的设想。并将你能修改的请求都改成我希望的样子。
放在[remote](app/android/app/src/main/java/com/magicvector/dataSource/remote)
类名就叫做RemoteApiSource

然后就是数据库的重复业务封装，
比如说`UserManager`你看到这个方法：
```kotlin
    suspend fun saveCurrentUser(session: UserSessionModel) {
        val loginAt = System.currentTimeMillis()
        userDao.clearCurrentFlag()
        userDao.upsert(
            UserEntity(
                userId = session.userId,
                account = session.account,
                name = session.name,
                avatarUrl = session.avatarUrl,
                accessToken = session.accessToken,
                password = session.password,
                isCurrent = true,
                lastLoginAt = loginAt
            )
        )
        currentUserSessionCache = session.copy(
            isCurrent = true,
            lastLoginAt = loginAt
        )
    }
```
很明显，第一UserSessionModel转为UserEntity需要按照我说的规则用convertor去实现，
第二saveCurrentUser这个方法应该封装到
[local](app/android/app/src/main/java/com/magicvector/dataSource/local)
中的object类总，然后提供数据结果回调，传入一个方法，这个方法的入参是数据库操作结果（如果是void就传入null）以及需要的数据
比如这里需要的就是currentUserSessionCache的更新，
那么就应该给回调UserEntity，这个userEntity的数据应该copy：
```kotlin
  currentUserSessionCache = session.copy(
      isCurrent = true,
      lastLoginAt = loginAt
  )
```
这个copy属于具体的manager业务所以定义应该定义在manager中。
再比如说：
```kotlin
    suspend fun getCurrentUser(): UserSessionModel? {
        val cached = currentUserSessionCache
        if (cached != null && cached.accessToken.isNotBlank()) {
            return cached
        }
        val current = userDao.getCurrent()?.toSession()
        currentUserSessionCache = current
        return current
    }
```
这个因为manager需要的是UserSessionModel，所以应该在local的UserLocalSource
然后获取userEntity的方法应该在里面完成，然后转为UserSessionModel是用convertor也在里面完成，
然后回调方法，回调`UserSessionModel`，然后再在manager中使用业务逻辑
比如先判断当前是否为空，不为空就return cached，为空才调用UserLocalSource然后回调currentUserSessionCache = current

总之就是Manager不持有Dao，Vm也不持有api，现在修改整个架构的文档，
文档修改完成之后理清楚思路之后再去修改代码。


#### 补充修改

第一，convertor应该方法名包含什么to什么比如你写的
```kotlin
    fun toEntity(session: UserSessionModel): UserEntity
    fun toModel(entity: UserEntity): UserSessionModel
```
改为
```kotlin
    fun model2Entity(session: UserSessionModel): UserEntity
    fun entity2Model(entity: UserEntity): UserSessionModel
```
其他的也要按照我说的修改，第二不要写那么复杂，什么又是UserDomainConvertor又是UserDomainConvertorImpl
都写成object类，参考我的MessageConvertor

然后更新设计文档


### RemoteApiSource调用suspend化

我希望RemoteApiSource的调用都能suspend化，因为这样就可以可以取消各种嵌套回调。
你现在要做的是：
1. 学习我写的RemoteApiSource.verifyAccessToken的这个完整链路，这个链路我是人审过代码的。
2. 浏览RemoteApiSource然后大概思考要怎么改并不做修改，让你有个印象。
3. 把我的这个总结到开规则[developAndRules.md](app/android/docs/developAndRules.md)和详细设计
  [AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)
  并说明为什么要suspend化，解决了什么问题
4. 梳理代码，并实现涉及到RemoteApiSource相关的整个app请求链路suspend化。


#### 补充

第一，关于代码的问题，你并没有理解我的意思。
我认为你RemoteApiSource改的还不错，但是上层没有理解我的意思，
你先再看一边我写的`verifyAccessToken``resolveStartTargetEffect()`怎么调用的。
因为我看了一下我之前经常写doxxx，handlexxx这种请求回调模式，但是这样就会带来大量的回调处理非常麻烦，
而且违背了mvi设计模式中的effect事件原则。
现在我希望你做的修改是：取消调用doxxx，handlexxx这种回调模式；
然后，不同的功能直接返回，这才符合suspend，我现在打个比方：
RemoteApiSource的getLastChat这个方法，我看了一下吗还是使用把response交给handleGetChatHistory去处理，
这里明显还有回调callback: SyncRequestCallback，而且还竟然持有context: Context，
全都干掉，doGetLastChat应该是一个suspend函数，要取消handlexxx直接业务逻辑放在函数内部，
在内部直接用effect执行之前SyncRequestCallback设计的回调。反正我现在希望你取消叫全部的回调，用suspend。
而且我这个方法以前入参包含context，以前我是mvvm是合理的，现在是mvi，是不合理的，我看你都没有质疑我这样写的合理性。
你不要怕改activity的内容，你都可改的。



第二，关于设计文档的问题：
@app/android/docs/AndroidDesignDocument.md 
这里面`## RemoteApiSource suspend 化改造` 是另一个AI写的，它比较笨，把这个当设计日志了。
你现在要把这部分内容拆分到RemoteApiSource以及设计文档应有的位置，我这个设计文档是由目录层次结构的，不能这么写。
```以 `verifyAccessToken` 为标准链路：```这种例子都得删掉。与之替换的是应该添加上
[remote](app/android/app/src/main/java/com/magicvector/dataSource/remote)中的[RemoteApiSource.kt](app/android/app/src/main/java/com/magicvector/dataSource/remote/RemoteApiSource.kt)
[local](app/android/app/src/main/java/com/magicvector/dataSource/local)中的 **LocalSource
[repository](app/android/app/src/main/java/com/magicvector/repository)
以及调用他们的vm和manager的这样规划的架构。
并写上remote，local的设计模式，绘制UML图：整体类图，内部函数架构的通用甘特图（就是里面的函数基本相同统一绘制一个就行），
内部函数的通用状态图。以及补充上这样设计的计算机理论基础，包括为什么使用suspend取消回调。




### 代码审核完成与笔记录入
我现在审核完成代码了。
我发现了一些问题：
1. datasource的操作很明显都是耗时操作，所以其suspend函数需要指定为IO（这部分代码可能你还需要改）[dataSource](app/android/app/src/main/java/com/magicvector/dataSource)
   并将规则记录到[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)
2. 给登录和注册的甘特图进行更新，因为现在甘特图不仅要写功能的线程甘特图了，还需要写不同操作具体在kotlin的哪个协程中执行比如IO或者Main
   然后查询kotlin中协程有多少种协程状态，比如IO，Main等，然后结合计算机理论：《操作系统》种的线程状态去分析各种操作应该放在哪种协程状态种。然后写入规则中。[AndroidDesignDocument.md](app/android/docs/AndroidDesignDocument.md)









