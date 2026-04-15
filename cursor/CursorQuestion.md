**CursorQuestion**
====


### App部分细节调整

#### App数据层架构修改
Android的RemoteApiSource request层级结构要改成返回需要的数据类型比如Model
* 我先给你介绍一下，我的数据结构定义，就用我的user模块来给你介绍吧。首先如果这个业务需要持久化到Android的Room数据库，我就会创建一个Entity比如说：
  [UserEntity.kt](../demo/app/app/src/main/java/com/vectordemo/domain/entity/UserEntity.kt)
  然后OSS也要创建一个Entity存储，我认为这些接口要缓存为Entity：
  /oss/user/bucket/list，/oss/user/bucket/file/id/list，/oss/user/bucket/file/url/list，/oss/user/bucket/file/item/list
  我认为表应该设计成：OssUserBucket（userId（long）对应bucketName列表，一对多）OSSBucketFile（bucketName和fileId（id）fileName，fileUrl一对多关系）
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



















