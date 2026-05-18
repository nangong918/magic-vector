import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.datasource.rtmp)
    implementation(libs.androidx.media3.ui)
    implementation(fileTree(mapOf("dir" to "aarlib", "include" to listOf("*.jar", "*.aar"))))
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.demo.kmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.demo.kmp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    aaptOptions {
        noCompress += listOf("onnx", "tflite")
    }
}