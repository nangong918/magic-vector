# 项目介绍


FlutterAAR此项目是用于专门集成Demo功能给Android和Flutter App用的，
是一个SDK项目。


## 项目结构介绍

### 打包的AAR需要引用外部AAR

会出现一种情况：我们这个项目的[aarlib](aarlib)aarlibs是给Android和flutter的aarlibs，但是可能还需要其他外部aar作为依赖。
但是Android又禁止aar内部打包aar，所以改为将外部的aar统一放在[LocalRepo](LocalRepo)然后在gradle引用他们以及他们的依赖。

### AARLibs
[aarlib](aarlib)这里面就是我自己写的AAR，相当于SDK；总体功能需要用Manager来管理，对外接口需要用Bridge来提供。当然你也可以把Bridge写为接口，然后业务用Impl。
关于使用kotlin还是Java都根据当时业务决定，都无所谓的。

### Android测试
写完SDK需要在本地用Android App[app](app)编写demo并运行app进行测试。

### 其他项目使用SDK
首先要将aarlib进行编译（Compile aarlib）打包生成aar在build的outputs路径下，然后拷贝到app/libs下。
使用SDK需要复制AARLibs编译产物和[LocalRepo](LocalRepo)全部aar。













