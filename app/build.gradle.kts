plugins {
    alias (libs.plugins.android.application)
    alias (libs.plugins.kotlin.android)
    alias (libs.plugins.kotlin.compose)
}

android {
    namespace = "com.anto426.dynamicisland"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.anto426.dynamicisland"
        minSdk = 32
        targetSdk = 36
        versionCode = 15
        versionName = "2.1.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }

    kotlin {
        jvmToolchain(24)
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }
}

dependencies {
    // -------------------- Kotlin --------------------
    implementation (libs.kotlin.stdlib)

    // -------------------- Compose --------------------
    implementation( platform(libs.androidx.compose.bom))
    implementation (libs.androidx.compose.ui)
    implementation (libs.androidx.compose.ui.graphics)
    implementation (libs.androidx.compose.ui.tooling.preview)
    implementation (libs.androidx.compose.material3)
    implementation (libs.androidx.compose.material.icons.core)
    implementation (libs.androidx.compose.material.icons.extended)
    implementation (libs.androidx.compose.ui.text.google.fonts)
    implementation (libs.androidx.compose.runtime.livedata)
    implementation (libs.androidx.compose.runtime)
    implementation (libs.androidx.compose.animation.core)
    implementation (libs.androidx.navigation.compose)
    implementation (libs.androidx.constraintlayout.compose)
    implementation (libs.androidx.compose.foundation)
    implementation (libs.androidx.compose.animation)

    // -------------------- Accompanist --------------------
    implementation (libs.accompanist.systemuicontroller)
    implementation (libs.accompanist.placeholder.material)
    implementation (libs.accompanist.flowlayout)
    implementation (libs.accompanist.webview)
    implementation (libs.accompanist.navigation.animation)

    // -------------------- Landscapist --------------------
    implementation (libs.landscapist.glide)
    implementation (libs.landscapist.palette)
    implementation (libs.landscapist.animation)


    // -------------------- Coil & Lottie --------------------
    implementation (libs.coil.compose)
    implementation (libs.lottie.compose)
    implementation (libs.composewaveloading)

    // -------------------- Media --------------------
    implementation (libs.androidx.media3.common.ktx)

    // -------------------- Lifecycle + Activity --------------------
    implementation (libs.androidx.core.ktx)
    implementation (libs.androidx.lifecycle.runtime.ktx)
    implementation (libs.androidx.lifecycle.runtime.compose)
    implementation (libs.androidx.activity.compose)

    // -------------------- Networking --------------------
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.okhttp)
    implementation (libs.logging.interceptor)
    implementation (libs.okhttp.dnsoverhttps)
    implementation (libs.gson)

    // -------------------- Coroutines --------------------
    implementation (libs.kotlinx.coroutines.core)
    implementation (libs.kotlinx.coroutines.android)

    // -------------------- WorkManager & Volley --------------------
    implementation (libs.androidx.work.runtime.ktx)
    implementation (libs.volley)

    // -------------------- Testing --------------------
    testImplementation (libs.junit)
    androidTestImplementation (libs.androidx.junit)
    androidTestImplementation (libs.androidx.espresso.core)
    androidTestImplementation (platform(libs.androidx.compose.bom))
    androidTestImplementation (libs.androidx.compose.ui.test.junit4)
    debugImplementation (libs.androidx.compose.ui.tooling)
    debugImplementation (libs.androidx.compose.ui.test.manifest)
}
