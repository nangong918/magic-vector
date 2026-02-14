**flutter集成AAR**
====





### 打AAR包


### 使用AAR

* 拷贝AAR

首先将AAR包拷贝到`android/libs/`目录下

* 添加依赖

首先在项目级别目录kts引入aar
```kts
allprojects {
    repositories {
        google()
        mavenCentral()
        // 正确写法：flatDir 放在 repositories 内部
        flatDir {
            // 指向 app 模块下的 libs 目录（AIKit.aar 所在位置）
            dirs = setOf(file("app/libs"))
            // 若有多个模块，可追加：dirs += file("其他模块/libs")
        }
    }
}
```

然后在应用级别目录`android/app/build.gradle.kts`中添加依赖扫描
```kts
dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
}
```


* 调用

直接在MainActivity中引用：
```java
import com.iflytek.aikit.core.AiAudio;
import com.iflytek.aikit.core.AiHandle;
import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.AiListener;
import com.iflytek.aikit.core.AiRequest;
import com.iflytek.aikit.core.AiResponse;
import com.iflytek.aikit.core.AiStatus;
import com.iflytek.aikit.core.BaseLibrary;
import com.iflytek.aikit.core.CoreListener;
import com.iflytek.aikit.core.ErrType;
import com.iflytek.aikit.core.LogLvl;
```

































