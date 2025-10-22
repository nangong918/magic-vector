plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
}

android {
    namespace = "com.magicvector"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.magicvector"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        setProperty("archivesBaseName", "Android-VAD-v$versionName")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++11"
            }
        }
    }

    buildTypes {
        release {
            // 混淆
            isMinifyEnabled = true
            // 兼容多Dex
            multiDexEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            multiDexEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
}

dependencies {

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // 基础
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.activity)

    // 测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // jetpack compose
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 圆形的 ImageView 组件
    implementation(libs.circleimageview)
    implementation(libs.roundedimageview)

    // OkHttp3 / WebSocket支持
    implementation(libs.okhttp)
    // OkHttp的日志
    implementation(libs.logging.interceptor)
    // SSE支持
    implementation(libs.okhttp.sse)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // event bus
    implementation(libs.eventbus)

    // Gson
    implementation(libs.gson)

    // multidex
    implementation(libs.multidex)

    /**
     * 自定义Module
     * 基本依赖关系：
     * 1.baseutil       通用依赖
     *      无
     * 2.appcore         核心框架 + 核心逻辑
     *      baseutil
     * 3.domain：               Domain数据层
     *      baseutil
     * 4.appview：     UI层
     *      baseutil，appcore，domain
     * 5.dao：           Mapper层：DataBase + Network
     *      baseutil，appcore，domain
     * 6.app：               Application
     *      baseutil，appcore，dao，appview，domain
     */
    implementation(project(":core:baseutil"))
    implementation(project(":data:domain"))
    implementation(project(":core:appcore"))
    implementation(project(":data:dao"))
    implementation(project(":view:appview"))

    // vad库
    implementation(project(":vad:silero"))
    implementation(project(":vad:yamnet"))
//    implementation(project(":vad:webrtc"))
}