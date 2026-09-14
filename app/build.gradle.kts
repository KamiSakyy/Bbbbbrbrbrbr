import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// ============================================================================
//  ВЕРСИИ (пиннинг)
// ============================================================================
//  Движок: GeckoView (Gecko + SpiderMonkey + WebRender) — НЕ Android WebView.
//  Актуальная стабильная ветка движка на дату сборки — 155.
val geckoVersion = "155.0.20260903215306"

// ============================================================================
//  Подпись релиза: ключи берём из -P / env (CI secrets), иначе debug-подпись,
//  чтобы локальная сборка `assembleRelease` работала без секретов.
// ============================================================================
fun propOrEnv(name: String): String? =
    (findProperty(name) as String?)?.takeIf { it.isNotBlank() } ?: System.getenv(name)?.takeIf { it.isNotBlank() }

val storeFilePath = propOrEnv("KEYSTORE_FILE")
val storePass = propOrEnv("KEYSTORE_PASSWORD")
val keyAliasName = propOrEnv("KEY_ALIAS")
val keyPass = propOrEnv("KEY_PASSWORD")
val hasReleaseKeys = storeFilePath != null && storePass != null && keyAliasName != null && keyPass != null

android {
    namespace = "ru.yandex.browser.mobile"
    compileSdk = 37

    defaultConfig {
        applicationId = "ru.yandex.browser.mobile"
        minSdk = 26            // GeckoView требует API 26+
        targetSdk = 36   // compileSdk 37 — требование свежих AndroidX
        versionCode = 3
        versionName = "1.0.2"
    }

    // ------------------------------------------------------------------
    //  Раздельные APK по ABI: движок весит много, поэтому каждый APK
    //  содержит только свою архитектуру (arm64 — основной).
    // ------------------------------------------------------------------
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    signingConfigs {
        if (hasReleaseKeys) {
            create("release") {
                storeFile = file(storeFilePath!!)
                storePassword = storePass
                keyAlias = keyAliasName
                keyPassword = keyPass
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true          // R8: обфускация + удаление мёртвого кода
            isShrinkResources = true        // выкидываем неиспользованные ресурсы
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseKeys) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
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
        // Движок весит много (libxul.so ~ десятки МБ). Сжимаем нативные
        // библиотеки в APK: файл для скачивания становится в ~2.5 раза меньше.
        // При установке система распаковывает их — это стандартный путь,
        // которым собирался и Firefox для Android.
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "META-INF/*.kotlin_module"
            )
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // --- Kotlin / AndroidX core ---
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")

    // --- Jetpack Compose (BOM фиксирует все compose-артефакты) ---
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")

    // --- ДВИЖОК: GeckoView (полноценный браузерный движок, а не WebView) ---
    implementation("org.mozilla.geckoview:geckoview:$geckoVersion")
}
