import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.regex.Pattern

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("androidx.baselineprofile")
}

fun releaseSecret(propertyName: String, environmentName: String): String? =
    providers.gradleProperty(propertyName)
        .orElse(providers.environmentVariable(environmentName))
        .orNull

val releaseStoreFilePath = releaseSecret("sleepdown.releaseStoreFile", "SLEEPDOWN_RELEASE_STORE_FILE")
val releaseStorePassword = releaseSecret("sleepdown.releaseStorePassword", "SLEEPDOWN_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSecret("sleepdown.releaseKeyAlias", "SLEEPDOWN_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSecret("sleepdown.releaseKeyPassword", "SLEEPDOWN_RELEASE_KEY_PASSWORD")

// Android Studio「Generate Signed Bundle or APK」向导注入的签名参数。向导不把它们写进任何配置文件，
// 而是触发构建时以 -P 参数传给 Gradle；AGP 读到四项齐全的值后会建一个名为 externalOverride 的签名
// 配置覆盖该次构建的签名配置，所以这里只需要认可它们，不需要在本脚本里再建一遍 signing config。
val ideStoreFilePath = providers.gradleProperty("android.injected.signing.store.file").orNull
val ideStorePassword = providers.gradleProperty("android.injected.signing.store.password").orNull
val ideKeyAlias = providers.gradleProperty("android.injected.signing.key.alias").orNull
val ideKeyPassword = providers.gradleProperty("android.injected.signing.key.password").orNull

val remoteConfigSecret = releaseSecret("sleepdown.remoteConfigSecret", "SLEEPDOWN_REMOTE_CONFIG_SECRET").orEmpty()
val skipReleaseResourceShrink = providers.gradleProperty("sleepdown.skipReleaseResourceShrink")
    .map(String::toBoolean)
    .getOrElse(false)
val hasConfiguredReleaseSigning = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

// 向导那四项必须一起齐全才算数：只认得半套等于没有签名身份。
val hasIdeReleaseSigning = listOf(
    ideStoreFilePath,
    ideStorePassword,
    ideKeyAlias,
    ideKeyPassword
).all { !it.isNullOrBlank() }

val hasReleaseSigning = hasConfiguredReleaseSigning || hasIdeReleaseSigning

@Suppress("UnstableApiUsage")
android {
    namespace = "com.xiaomanjun.sleepdownschedule"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }
    buildFeatures {
        buildConfig = true
    }
    lint {
        disable += setOf(
            "NullSafeMutableLiveData",
            "RememberInComposition",
            "FrequentlyChangingValue",
            "AutoboxingStateCreation",
            "ObsoleteLintCustomCheck",
            "GradleDependency",
            "VectorPath",
            "NestedWeights",
            "UnusedResources",
            "IconLauncherShape",
            "IconLocation",
            "IconDuplicates",
            // API 37 is still used only for compilation; changing target behavior is a release decision.
            "OldTargetApi",
            // The benchmark variant must stay unshrunk so baseline-profile tooling can inspect it.
            "NotShrinkingResources"
        )
    }

    defaultConfig {
        applicationId = "com.xiaomanjun.sleepdownschedule"
        minSdk = 26
        targetSdk = 36
        versionCode = 32
        versionName = "1.2.6_beta12"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SLEEPDOWN_API_BASE_URL", "\"https://api.sleepdownschedule.cn\"")
        buildConfigField(
            "boolean",
            "SLEEPDOWN_LARGE_GLASS_EXPERIMENT",
            "true"
        )
    }

    sourceSets {
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }

    signingConfigs {
        // 只认四项 sleepdown.release* 的值：向导那次构建的签名由 AGP 的 externalOverride 负责，
        // 这里没有可用的明文值，混进来会在 requireNotNull 处直接崩在配置阶段。
        if (hasConfiguredReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFilePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        val glassOcclusionMode = providers.gradleProperty("sleepdown.glassOcclusionMode")
            .getOrElse("legacy")
        require(glassOcclusionMode in setOf("legacy", "retained", "live"))
        all {
            buildConfigField("String", "GLASS_OCCLUSION_MODE", "\"legacy\"")
            buildConfigField("boolean", "GLASS_FIXED_MORPH", "false")
        }
        getByName("debug") {
            buildConfigField("boolean", "GLASS_FIXED_MORPH", providers.gradleProperty("sleepdown.glassFixedMorph").getOrElse("false").toBoolean().toString())
            buildConfigField("String", "GLASS_OCCLUSION_MODE", "\"$glassOcclusionMode\"")
            applicationIdSuffix = ".debug"
            buildConfigField("String", "SLEEPDOWN_REMOTE_CONFIG_SECRET", "\"\"")
            buildConfigField("boolean", "SLEEPDOWN_REMOTE_AI_ENABLED", "false")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = !skipReleaseResourceShrink
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
            buildConfigField("String", "SLEEPDOWN_REMOTE_CONFIG_SECRET", "\"$remoteConfigSecret\"")
            buildConfigField("boolean", "SLEEPDOWN_REMOTE_AI_ENABLED", remoteConfigSecret.isNotBlank().toString())
        }
        create("benchmark") {
            initWith(getByName("release"))
            buildConfigField("boolean", "GLASS_FIXED_MORPH", providers.gradleProperty("sleepdown.glassFixedMorph").getOrElse("false").toBoolean().toString())
            buildConfigField("String", "GLASS_OCCLUSION_MODE", "\"$glassOcclusionMode\"")
            matchingFallbacks += listOf("release")
            applicationIdSuffix = ".benchmark"
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
        }
        // 基线配置 / Macrobenchmark 插件会在配置阶段从 release 派生这两个构建类型（前缀 nonMinified / benchmark），
        // 并在运行时补 matchingFallbacks = ["release"]。但那件事发生在插件回调里，Android Studio 的
        // 依赖分析只读构建脚本里解析出来的 DSL 模型，看不到插件补的那份，于是对比 :kyant-backdrop 时
        // 会报 "No build type in module 'kyant-backdrop' matches build type 'benchmarkRelease' / nonMinifiedRelease"。
        // 这里显式声明出来，就是为了把 matchingFallbacks 落进脚本本身。插件发现同名类型已存在时会走
        // “保留已声明配置”的分支，只覆盖它自己需要的那几项（isDebuggable / isProfileable / signing 等），
        // 所以 initWith(release) 得到的 minify、shrink、proguardFiles 与插件自己创建的结果一致。
        create("nonMinifiedRelease") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
        }
        create("benchmarkRelease") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("github") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"github\"")
        }
        create("store") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION_CHANNEL", "\"store\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

androidComponents {
    onVariants(selector().withName(Pattern.compile("(github|store)BenchmarkRelease"))) { variant ->
        variant.applicationId.set("${variant.applicationId.get()}.benchmark")
    }
}

tasks.configureEach {
    val createsReleaseArtifact = name.matches(Regex("(assemble|bundle|package).*(Release)$"))
    if (createsReleaseArtifact) {
        doFirst {
            check(hasReleaseSigning) {
                "SleepDown release signing is missing. Configure sleepdown.releaseStoreFile, " +
                    "sleepdown.releaseStorePassword, sleepdown.releaseKeyAlias and " +
                    "sleepdown.releaseKeyPassword (or the matching SLEEPDOWN_RELEASE_* environment variables), " +
                    "or build through the Android Studio 'Generate Signed Bundle or APK' wizard."
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

ksp {
    arg("room.schemaLocation", file("$projectDir/schemas").path)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.runtime:runtime-tracing")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.metrics:metrics-performance:1.0.0")
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    implementation("androidx.palette:palette-ktx:1.0.0")
    compileOnly("com.oplus.animation:viewseamless:1.0.0@aar")
    implementation(project(":kyant-backdrop"))
    implementation("io.github.kyant0:shapes:1.2.0")
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.3")
    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.room:room-ktx:2.8.3")
    ksp("androidx.room:room-compiler:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.room:room-testing:2.8.3")
    baselineProfile(project(":benchmark"))
}
