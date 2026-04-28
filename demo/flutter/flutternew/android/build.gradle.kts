allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        // 正确写法：flatDir 放在 repositories 内部
        flatDir {
            // 指向 app 模块下的 libs 目录（AIKit.aar 所在位置）
            dirs = setOf(file("app/libs"))
            // 若有多个模块，可追加：dirs += file("其他模块/libs")
        }
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
