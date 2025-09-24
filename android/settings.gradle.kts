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
