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



