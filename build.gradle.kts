// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}

/**
 * 版本号自增：patch 位 +1，versionCode +1，写回 gradle.properties。
 * 用法： .\gradlew.bat bumpVersion
 * 脚本 scripts/release.ps1 会在构建发行版前自动调用。
 *
 * 注意：doLast 内部不引用脚本级函数/属性，保持配置缓存兼容。
 */
tasks.register("bumpVersion") {
    val propsFile = layout.projectDirectory.file("gradle.properties").asFile
    doLast {
        val text = propsFile.readText()
        val currentName = Regex("(?m)^gitboxVersionName=(.*)$")
            .find(text)?.groupValues?.get(1)?.trim()?.removePrefix("v") ?: "0.0.1"
        val currentCode = Regex("(?m)^gitboxVersionCode=(.*)$")
            .find(text)?.groupValues?.get(1)?.trim()?.toIntOrNull() ?: 1

        val parts = currentName.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        val nextName = "$major.$minor.${patch + 1}"
        val nextCode = currentCode + 1

        propsFile.writeText(
            text
                .replace(Regex("(?m)^gitboxVersionName=.*$"), "gitboxVersionName=$nextName")
                .replace(Regex("(?m)^gitboxVersionCode=.*$"), "gitboxVersionCode=$nextCode")
        )
        println("version: $currentName ($currentCode) -> $nextName ($nextCode)")
    }
}

/** 简写命令：.\gradlew.bat debug / release，等价于 :app:assembleDebug / :app:assembleRelease */
tasks.register("debug") {
    group = "build"
    description = "编译 debug 包"
    dependsOn(":app:assembleDebug")
}

tasks.register("release") {
    group = "build"
    description = "编译 release 包（不自增版本）"
    dependsOn(":app:assembleRelease")
}
