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

# 保留 ActivityLaunchUtils 类及其内部类 (不写的话会出现混淆问题：R8会自动删除其觉得不适用的类)
-keep class com.core.baseutil.ActivityLaunchUtils { *; }
# 保留 IntentConfig 内部类
-keep class com.core.baseutil.ActivityLaunchUtils$* { *; }
# 干脆全部保留
-keep class com.core.baseutil.*


# 保留所有的 Kotlin 数据类和反射相关的类
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keepclassmembers class * {
    public <init>(...);
}