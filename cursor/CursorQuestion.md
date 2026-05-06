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



### Live直播推流
我之前跑通过一版本的直播推流，现在需要你迁移到App（Android）中。
首先需要你把推流的功能剥离出来作成SDK，大概功能是RTMP，X264编辑码，FFmpeg编解码。
本次任务先写SDK，然后再在App中验证。

你先看到这个项目：[flutteraar](../demo/flutter/flutteraar)
他的介绍在里面：[README.md](../demo/flutter/flutteraar/README.md)我认为你能看懂，
然后需要你迁移的功能：
* 录制并推流功能：[LiveActivity.kt](../demo/cpp/app/src/main/java/com/demo/cpp/activity/LiveActivity.kt)
    要把这个页面的全部功能迁移到SDK，对了比较困难的是你需要自行考虑哪些属于SDK哪些属于app，其实我区分SDK的本质是为了给Flutter使用，
    因为Flutter使用JNI非常的不方便，然后我就把RTMP，X264编辑码，FFmpeg编解码等这些功能封装成AAR的SDK方便Flutter调用，
    但是我认为啊Camera和画面显示这些功能Android和Flutter（Android、IOS）都能直接实现，没必要去调用原生接口。所以你在那时把我刚刚说的那三个封装成SDK就行了。
* FFmpeg推流功能：C:\Github\FFmpegAndroid-master\app\src\main\java\com\frank\ffmpeg\activity\PushActivity.kt
    这个我没记错的话是基于FFmpeg推流的，然后也是迁移核心到SDK[aarlib](../demo/flutter/flutteraar/aarlib)，其他非核心放在[app](../demo/flutter/flutteraar/app)写一个Demo出来。

补充：你顺便搜一搜flutter是否能直接使用官方库实现live推流和live拉流，并给我一个报告，关于Android，IOS适配性以及和FFmpeg和RTMP，X264编解码的性能对比。
    对了我用FFmpeg和RTMP，X264编解码其实主要是为了以后写系统Android App烧入RK设备。Flutter即便又可替代的Live推拉流也不行，所以这次任务该完成还是要完成，完成之后给我分析Flutter是不是有更好的方法，因为毕竟IOS我没实现Native呢对不对，
    给出一个“Live功能实现报告”，里面包括我刚刚说的，Flutter跨平台实现，Android原生烧录RK实现，以及不同方案的性能比较。



### Live 拉流播放

我现在已经实现了Live推流功能，并且我用VLCPlayer验证过没问题。
我的Live推流代码在[LivePushDemoActivity.java](../demo/flutter/flutteraar/app/src/main/java/com/example/flutteraar/ui/activity/LivePushDemoActivity.java)
功能介绍活动图在：[Live功能活动图.md](../demo/flutter/flutteraar/Live功能活动图.md)
你只需要看RTMP + X264编解码部分，因为我暂时不用ffmpeg去推流。
然后现在你思考一下怎么接收流比较好？就是我希望接流并播放，关于推流的rtmp链接在代码里面有，你也可以看nginx配置[nginx.conf](../demo/springboot/nginx-docker/conf/nginx.conf)
[docker-compose.yml](../demo/springboot/docker/docker-compose.yml)
这些我都是配置好了并且验证通过了的，我现在希望你拉流。

现在你需要做到是：
1. 阅读我原先的推流活动图，代码理解架构和代码原理。
2. 上网查询并思考这种编码格式的推流方式应该怎么拉流，解码，播放。包括播放器怎么选择。
3. 查看C:\Github\FFmpegAndroid-master\app\src\main\java\com\frank\ffmpeg\activity\VideoPreviewActivity.kt这里的代码，
   这是我本地可运行的app，看看这个能满足需求吗？如果满足可以复用，如果不满足只有你来做了。
4. 帮我完成拉流播放代码，如果无法完成告诉我原因，写到[Live功能实现报告.md](../demo/flutter/flutteraar/Live功能实现报告.md)，如果可以完成你也要告诉我怎么完成的。

注意，你写的时候要创建Live Pull拉流播放Demo，写在[flutteraar](../demo/flutter/flutteraar)
关于这个项目怎么写demo你要看：[README.md](../demo/flutter/flutteraar/README.md)


### Live推流与Live拉流播放

我现在已经完成了Live推流和Live拉流播放的Android JavaDemo，源码在：
* 推流：[LivePushDemoActivity.java](../demo/flutter/flutteraar/app/src/main/java/com/example/flutteraar/ui/activity/LivePushDemoActivity.java)
* 拉流：[LivePullDemoActivity.java](../demo/flutter/flutteraar/app/src/main/java/com/example/flutteraar/ui/activity/LivePullDemoActivity.java)
大概介绍一下，推流是基于Cpp源码的RTMP + X264编码推流，传输其实是基于Nginx[nginx.conf](../demo/springboot/nginx-docker/conf/nginx.conf)的RTMP传输，拉流是ExoPlayer拉流.
然后我这个[flutteraar](../demo/flutter/flutteraar)项目其实就是一个生成aar并测试的项目，具体介绍你可以看[README.md](../demo/flutter/flutteraar/README.md)
然后上述功能我已经验证过了。
现在需要你：
1. 把[flutteraar](../demo/flutter/flutteraar)的生成的新的aar分别拷贝到[app](../demo/app)和[flutternew](../demo/flutter/flutternew)
   1. 其中[app](../demo/app)就是我的基于Kotlin Jetpack Compose 以MVI为设计模式的Android App这么做的目的是后续可能转为KMP跨平台。
   2. 第二是[flutternew](../demo/flutter/flutternew)这是我的Flutter项目，这个是直接可以跨平台的项目，你直接按照原来的Demo方式实现迁移就行了。
2. 分别在[app](../demo/app)和[flutternew](../demo/flutter/flutternew)实现直播推流和直播拉流的demo
3. 关于Flutter的直播推流和直播拉流虽然这已经实现了。但是你要在demo后面标注(Android)因为这其实是调用Android原生实现的，接下来你西药与上网查资料并分析怎么用dart以及依赖库实现并把可执行方案写在[Live功能实现报告.md](../demo/flutter/flutteraar/Live功能实现报告.md)
4. 如果flutter的dart原生能实现IOS和Android双平台的推流并播放，那么你就再实现两个Demo，这次后面备注是(跨平台)

#### 补充

flutter的live推拉流的（Android）不对把，我的期望是跟Android那边是打开就是相同的功能，而不是再点击按钮跳转，而且你这个跳转点击就报错闪退啊。
修改成我期望的调用原生android方法实现。


### FFmepge + RTSP 文件推流

我现在想要实现RTSP及逆行文件推流，我之前已经实现了RTMP直播推流，我的Nginx配置在：[nginx.conf](../demo/springboot/nginx-docker/conf/nginx.conf)[mime.types](../demo/springboot/nginx-docker/conf/mime.types)
现在我希望也实现RTSP直播文件推流，首先我认为应该配置nginx实现RTSP支持对不对？
然后你其实可以参考我的RTMP代码：（原来的RTMP的demo已经实现完成咯，我已经测过没问题了，你不要动。你写文件推流要写心得Demo）
推流：[LivePushDemoActivity.java](../demo/flutter/flutteraar/app/src/main/java/com/example/flutteraar/ui/activity/LivePushDemoActivity.java)
拉流：[LivePullDemoActivity.java](../demo/flutter/flutteraar/app/src/main/java/com/example/flutteraar/ui/activity/LivePullDemoActivity.java)
我不知道你能不能完成RTSP推流，因为其实你也看得到我的底层是有JNI支持的，我是上网去下载的RTMP的。所以我猜测RTSP也需要下载Cpp依赖库导入项目，
如果需要你可以直接不用完成这个任务了，直接跟我是哦为什么，以及我去哪里下载对应的RTSP资源，写在[Live功能实现报告.md](../demo/flutter/flutteraar/Live功能实现报告.md)
如果不需要下载，你就直接完成，当然怎么完成的你也直接写在报告里面。
对了如果RTSP没cpp依赖库你无法完成，你就看下这个：C:\Github\FFmpegAndroid-master\app\src\main\java\com\frank\ffmpeg\activity\PushActivity.kt
这个是我从github上面拉下来的FFmpeg推流，你看看能不能推流文件？或者说实现RTSP推流，如果可以或者不可以，都写到文档[Live功能实现报告.md](../demo/flutter/flutteraar/Live功能实现报告.md)
当然如果可以那么你就直接实现一个demo呗：[flutteraar](../demo/flutter/flutteraar)
实现规则你可以看[README.md](../demo/flutter/flutteraar/README.md)
如果FFmpeg和RTSP是两个不同的架构的话，那你就要写两个Demo咯，
顺便一说，我的Nginx是部署在Docker镜像中的，也就是Linux环境。


#### 补充
我有问题：
1. 这个demo有没有点击选择本地mp4进行推流的功能，就是选择本地媒体进行推流
2. 这个demo是否需要Nginx修改配置？还是现在的配置就可以直接推RTSP流了？
3. 需不需要新加一个拉取RTSP的Demo？还是原先的拉取RTMP的Demo就可以复用Media3播放？

#### 完成
那你现在能帮我完成任务吗？
1. 因为我觉得实时摄像头这种直播推流是RTMP合适，RTSP是推流文件，所以你的demo改为选择本地的mp4进行推流u。
2. 你看看能否想办法帮我搭建一个能支持rtsp推流的环境，就是我现在开启两个手机的同一个app，第一个App打开推流demo，推出RTSP文件流，然后第二个手机也是这个App的拉流demo然后播放拉流地址的文件流。
3. 你看看怎么帮我实现拉RTSP流？复用或者是开新demo都可以。但是不要让之前的RTMP代码失效。
