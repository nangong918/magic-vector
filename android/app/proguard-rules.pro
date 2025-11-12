# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 保留所有的 Kotlin 数据类和反射相关的类
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keepclassmembers class * {
    public <init>(...);
}



# 保留 Android 组件（Activity、Service、Application 等）的原始类名和 onCreate 等方法
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.app.Application
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# 保留组件的生命周期方法（避免混淆 onCreate、onStart 等）
-keepclassmembers public class * extends android.app.Activity {
    void onCreate(android.os.Bundle);
    void onStart();
    void onStop();
    void onDestroy();
}
-keepclassmembers public class * extends android.app.Application {
    void onCreate();
}

# 保留 ApiRequestProvider 类及其伴生对象（Kotlin 伴生对象需特殊处理）
-keep class com.core.appcore.api.ApiRequestProvider { *; }
-keep class com.core.appcore.api.ApiRequestProvider$Companion { *; }

# 保留 ApiRequest 相关类（假设你的网络请求接口/实例是 ApiRequest）
# 若 ApiRequest 是 Retrofit 接口，需保留其方法签名；若为普通类，保留所有成员
-keep class com.core.appcore.api.ApiRequestProvider { *; }
-keep class com.magicvector.MainApplication { *; }
-keep class com.core.appcore.api.ApiRequest { *; }
# 若 ApiRequest 是接口，需额外保留接口方法（避免方法名被混淆）
-keep interface com.core.appcore.api.ApiRequest {
    *;
}

# 保留 Retrofit 接口及其方法（关键！避免方法签名被混淆）
-keep public interface com.core.appcore.api.ApiRequest {
    *;
}
# 保留 Retrofit 相关类的泛型信息（避免类型擦除导致的转换异常）
-keepattributes Signature
-keepattributes Exceptions

# 保留 OkHttp 相关类（若使用 OkHttp 作为网络客户端）
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
# 保留 Retrofit 动态代理生成的类（避免代理类被优化）
-keep class * extends retrofit2.Retrofit
-keep class * implements retrofit2.Converter

# 保留 Kotlin 协程相关类（避免 suspend 方法被混淆）
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }
# 保留 Kotlin Flow 相关类（避免泛型信息丢失）
-keep class kotlinx.coroutines.flow.** { *; }
-keep interface kotlinx.coroutines.flow.** { *; }

# 保留 Kotlin 类的成员（避免属性/方法被混淆）
-keepclassmembers class kotlin.** { *; }
# 保留 Kotlin 伴生对象的静态方法
-keepclassmembers class *$Companion {
    *;
}

# EventBus
# ProGuard 和它的继承者 R8 都提供了压缩、优化、混淆和预校验四大功能。压缩和优化会移除未使用的类/方法/字段，混淆会使用无意义的简短名称重命名类/方法/字段。
# @Subscribe 订阅方法是通过反射调用的，在编译时没有直接调用，如果不增加反混淆规则的话，在运行时会出现找不到方法名的情况。因此，EventBus需要配置以下混淆规则：
-keepattributes *Annotation*
# keep住所有被Subscribe注解标注的方法
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# 如果使用了AsyncExecutor，还需要配置混淆规则：
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}