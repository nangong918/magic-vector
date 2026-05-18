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













