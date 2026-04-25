# todo

完成全部demo再进行设计app吧，而且完成demo之后直接交给AI完成就行了。
我需要做的事情就是完全跑通demo

## 当前计划

1. 编写SpringBoot启动脚本，总是忘记启动Nginx和Minio（可以直接使用docker编排文件）

## 待编排的任务
* Android的RemoteApiSource request层级结构要改成返回需要的数据类型比如Model
* Http传输图片等文件不应该展示具体内容，直接取消SkipMultipartBinaryHttpLogger改为HttpLoggingInterceptor中检查req和resp是不是json类型，不是就不输出。
* 保存下载回调未完成
* Screen、Activity职能分离：我设计Screen目的是相当于旧版Android的xml。这样就能解耦ui和业务。

* Flutter的Api应该放在ApiRequest，基本逻辑实现应该参考之前的ApiRequest。解耦AuthRemoteApiSource和OssRemoteApiSource
* SafeDioLogInterceptor需要检查跟我之前说的【HttpLoggingInterceptor中检查req和resp是不是json类型，不是就不输出】逻辑是否一样。
* 下拉刷新，数据更新UI存在问题
* OssDemoPage需要创建ViewModel，拆分UI数据流业务更新逻辑。

## 日计划及实际行动

### Demo
[计划.md](plan/计划.md)

## 月计划及实际行动




