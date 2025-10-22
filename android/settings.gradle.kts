pluginManagement {
    repositories {
        val mavenUrls = listOf(
            "https://maven.aliyun.com/repository/central",
            "https://maven.aliyun.com/repository/jcenter",
            "https://maven.aliyun.com/repository/google",
            "https://maven.aliyun.com/repository/public",
            "https://jitpack.io"
        )

        // 改为阿里云的镜像地址
        mavenUrls.forEach { url ->
            maven(url)
        }

        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        val mavenUrls = listOf(
            "https://maven.aliyun.com/repository/central",
            "https://maven.aliyun.com/repository/jcenter",
            "https://maven.aliyun.com/repository/google",
            "https://maven.aliyun.com/repository/public",
            "https://jitpack.io"
        )

        // 改为阿里云的镜像地址
        mavenUrls.forEach { url ->
            maven(url)
        }

        google()
        mavenCentral()
    }
}

rootProject.name = "MagicVector"
include(":app")
include(":core:appcore")
include(":core:baseutil")
include("data:dao")
include("data:domain")
include("view:appview")
include("vad:silero")
include("vad:yamnet")
// 不迁移到VAD文件夹中：1.文件夹过长NDK无法编译 2.文件夹变化需要配置全部的JNI名称变化和Cpp头文件路径
include(":webrtc")
