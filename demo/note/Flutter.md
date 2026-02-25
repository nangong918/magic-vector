**Flutter**
====

flutter的版本总是一个问题，因为各种适配原因，flutter版本总是不能使用最新的，
所以维护一套flutterDemo和flutterNew，两个的内容基本一致
主要区别就是flutter新旧版本的区别。

### flutter环境问题


`flutter pub get`的时候遇到flutter锁问题

```shell
Waiting for another flutter command to release the startup lock...
```

JDK版本问题：
```shell
Android Gradle plugin requires Java 17 to run. You are currently using Java 11.
```
解决方法：
```shell
flutter config --jdk-dir="C:\Users\clt\.jdks\dragonwell-17.0.18"

```


## flutter 编译


### 打包


[flutter三端打包.md](flutter/flutter三端打包/flutter三端打包.md)

### flutter调用平台native

[flutter调用平台native.md](flutter/flutter调用平台native/flutter调用平台native.md)


### flutter集成AAR


[flutter集成aar.md](flutter/flutter集成SDK/flutter集成aar.md)


## Android 过度 Flutter


[android过度flutter.md](flutter/android过度flutter/android过度flutter.md)



### UI组件

[flutterUI.md](flutter/UI/flutterUI.md)



#### Flutter国际化

[flutter国际化.md](flutter/flutter国际化/flutter国际化.md)



### flutter语法


[flutter语法.md](flutter/flutter语法/flutter语法.md)















