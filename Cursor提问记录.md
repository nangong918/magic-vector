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







