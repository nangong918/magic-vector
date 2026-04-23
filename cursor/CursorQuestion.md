**CursorQuestion**
====


### App部分细节调整

#### App数据层架构修改
Android的RemoteApiSource request层级结构要改成返回需要的数据类型比如Model
* 我先给你介绍一下，我的数据结构定义，就用我的user模块来给你介绍吧。首先如果这个业务需要持久化到Android的Room数据库，我就会创建一个Entity比如说：
  [UserEntity.kt](../demo/app/app/src/main/java/com/vectordemo/domain/entity/UserEntity.kt)
  然后OSS也要创建一个Entity存储，我认为这些接口要缓存为Entity：
  /oss/user/bucket/list，/oss/user/bucket/file/id/list，/oss/user/bucket/file/url/list，/oss/user/bucket/file/item/list
  我认为表应该设计成：OssUserBucketFile（userId（long）对应bucketName列表，一对多，bucketName和fileId（id）fileName，fileUrl一对多关系）
* 然后需要创建Model和Manager，Model是Entity在程序中的业务参考[UserSessionModel.kt](../demo/app/app/src/main/java/com/vectordemo/domain/model/user/UserSessionModel.kt)
* 然后我的RemoteApiSource期望是返回的是业务层需要的Model，这样业务层就不需要关心Response是怎样的了。
* 然后Model是跟Entity相关的业务模型，如果跟Entity无关就是叫做BO，目前暂时没有，只是给你介绍。
* 然后类型之间的转换你要放在convertor，你可以参考[UserConvertor.kt](../demo/app/app/src/main/java/com/vectordemo/domain/convertor/UserConvertor.kt)

flutter先不用修改，等下App我审核通过再喊你改


* Http传输图片等文件不应该展示具体内容，直接取消SkipMultipartBinaryHttpLogger改为HttpLoggingInterceptor中检查req和resp是不是json类型，不是就不输出。
* 保存下载回调未完成：提示下载成功，包含路径
* Screen、Activity职能分离：我设计Screen目的是相当于旧版Android的xml。这样就能解耦ui和业务。

* Flutter的Api应该放在ApiRequest，基本逻辑实现应该参考之前的ApiRequest。解耦AuthRemoteApiSource和OssRemoteApiSource
* SafeDioLogInterceptor需要检查跟我之前说的【HttpLoggingInterceptor中检查req和resp是不是json类型，不是就不输出】逻辑是否一样。
* 下拉刷新，数据更新UI存在问题
* OssDemoPage需要创建ViewModel，拆分UI数据流业务更新逻辑。


### Flutter修改
* 这个flutter项目是这个Android项目的flutter化，要遵守这个Android项目的架构。
  首先介绍一下，这个flutter项目是迁移这个android项目的功能。
  首先这个flutter的架构应该跟android一样，所有请求都应该走apirequest。
  首先要要定义数据库的实体类型entity，你可以参考Android那边的userEntity。
  然后需要Convertor转化为UserSessionModel，然后
  dataSource是为了对上层屏蔽Response和Entity类型的，内部用Convertor转换，
  然后ViewModel持有的是各个Manager，Manager持有的是dataSource，
  相当于ViewModel不持有数据库数据源和网络数据源，这部分是由Manager持有。
  然后具体业务你都可以分析Android并迁移就行。
* Flutter的Api应该放在ApiRequest，基本逻辑实现应该参考之前的ApiRequest。解耦AuthRemoteApiSource和OssRemoteApiSource
* SafeDioLogInterceptor需要检查跟我之前说的【HttpLoggingInterceptor中检查req和resp是不是json类型，不是就不输出】逻辑是否一样。
* 下拉刷新，数据更新UI存在问题
* OssDemoPage需要创建ViewModel，拆分UI数据流业务更新逻辑。


#### 补充

我审核了一下代码，提出以下问题
1. 你要仿照Android的路径层次结构去完成：
   dataSource: local/remote
   当然顺便说一下，你的把RemoteApiSource拆分成UserRemoteApiSource和OSSRemoteApiSource是对的，请保持，并且拆分Android中的RemoteApiSource
2. 你没有完全参考我的Android的设计理念：
   LocalDataSource不对外暴露Entity，而是内部用Convertor对外提供调用者希望的Model类型，包括他们下发数据也是给Model而不是Entity。
   同理RemoteApiSource也不对外暴露Response，而是提供他们直接需要的Model类型，包括他们下发数据也是给Model而不是Request。
   VM应该不引用任何Entity和Request，Response，这些对他们来说是无感知的，同理Page和Screen也不应该感知。

3. 我发现Flutter跟Android不一样，ApiRequest可以直接生成api_request.g.dart，我看了一下你好像是好好用了的（这条就是检查一下，顺口一提，不是什么任务）
   意思是不是ApiRequestImpl不需要了，我认为是不需要了，我看里面也就是一些demo的方法，你给它放到demoRemoteApiSource吧。




### VoiceAgent问题

我现在启动你写的从flutter迁移到android的，然后打开voiceAgent遇到了问题：
首先flutter的voiceAgent是好的，但是Android这边遇到了问题，大概是这样：
```log
离线唤醒认证成功
就绪
开始唤醒词监听：小卡小卡
异常：离线唤醒异常：loadData失败：18608
异常：离线唤醒异常：启动了录音唤醒失败：18609
会话已结束
```
而且我看代码你好像是申请了权限的，应该不是权限问题；
其实你自己为了排查你可以自己加日志排查原因然后修复。


还是不行，你是在不行可以打印日志我配合你排查，
还有官方文档在这里：https://www.xfyun.cn/doc/asr/AIkit_awaken/Android-SDK.html
但是我很遗憾的告诉你我没找到18608这个错误码，还是要靠你排查。

当然如果你不知道怎么做我还是建议你仔细分析我flutter怎么做的，毕竟我那边都已经成功。
对了你还要好好看看官方文档，没准有什么蛛丝马迹我不知道。








