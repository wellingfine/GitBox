import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // 注意：AGP 9 已内置 Kotlin Android 插件，不能再显式声明 kotlin-android；
    // Compose 编译器插件仍需显式声明，版本需与 AGP 内置 Kotlin 版本一致（2.2.10）。
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.v0tx.gitbox"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.v0tx.gitbox"
        minSdk = 35
        targetSdk = 37
        // 版本来自 gradle.properties，由根工程 bumpVersion 任务维护
        versionCode = providers.gradleProperty("gitboxVersionCode").getOrElse("1").toInt()
        versionName = providers.gradleProperty("gitboxVersionName").getOrElse("0.0.1")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 有 keystore.properties 时用正式签名，否则回退到 debug 签名，保证随时能出包。
    // 用 provider 读取文件内容，配置缓存才能正确追踪该文件。
    val keystorePropsText = providers
        .fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
        .asText
        .getOrElse("")
    signingConfigs {
        if (keystorePropsText.isNotBlank()) {
            val props = Properties()
            props.load(keystorePropsText.reader())
            create("release") {
                storeFile = rootProject.layout.projectDirectory.file(props.getProperty("storeFile")).asFile
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 全量 R8 优化。不要配 packageScope：那是给「尚未使用 R8 的应用」做渐进迁移
            // 的实验配置，会让依赖库不被裁剪（APK 暴涨）并可能破坏第三方库的访问可见性
            optimization { enable = true }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

// 输出文件名带上版本号：GitBox.0.1.1.release.apk / GitBox.0.1.1.debug.apk
androidComponents {
    val appVersion = providers.gradleProperty("gitboxVersionName").getOrElse("0.0.1")
    onVariants(selector().all()) { variant ->
        val buildType = variant.buildType ?: variant.name
        variant.outputs.forEach { output ->
            output.outputFileName.set("GitBox.$appVersion.$buildType.apk")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.gson)
    implementation(libs.jgit)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

/**
 * 构建完把 APK 复制到仓库最外层的 build/（.gitignore 已忽略 /build）。
 * 用 finalizer 挂在 assemble 上，简写命令、长命令、脚本构建都会带上这一步；
 * 路径在配置阶段取好，doLast 里只引用局部变量，保持配置缓存兼容。
 */
listOf("Debug", "Release").forEach { suffix ->
    val copyTask = tasks.register("copy${suffix}ApkToRoot") {
        val targetDir = rootProject.layout.projectDirectory.dir("build").asFile
        val apkDir = layout.buildDirectory.dir("outputs/apk/${suffix.lowercase()}").get().asFile
        group = "build"
        description = "把 $suffix APK 复制到仓库最外层的 build/"
        doLast {
            val apk = apkDir.listFiles { file -> file.isFile && file.name.endsWith(".apk") }
                ?.maxByOrNull { it.lastModified() }
            if (apk == null) {
                logger.warn("没有找到 APK：$apkDir")
                return@doLast
            }
            targetDir.mkdirs()
            apk.copyTo(targetDir.resolve(apk.name), overwrite = true)
            println("APK -> build/${apk.name}")
        }
    }
    tasks.matching { it.name == "assemble$suffix" }.configureEach { finalizedBy(copyTask) }
}
