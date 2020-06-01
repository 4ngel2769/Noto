import com.noto.buildsrc.*

plugins {
    val plugins = com.noto.buildsrc.Plugins
    id(plugins.ANDROID_LIBRARY)
    kotlin(plugins.KOTLIN_ANDROID)
    id(plugins.KOTLIN_ANDROID_EXTENSIONS)
}

android {
    compileSdkVersion(com.noto.buildsrc.App.COMPILE_SDK)
    buildToolsVersion(com.noto.buildsrc.App.BUILD_TOOLS)
    defaultConfig {
        minSdkVersion(com.noto.buildsrc.App.MIN_SDK)
        targetSdkVersion(com.noto.buildsrc.App.TARGET_SDK)
        versionCode = com.noto.buildsrc.App.APP_VERSION_CODE
        versionName = com.noto.buildsrc.App.APP_VERSION_NAME

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

dependencies {
    implementation (project(com.noto.buildsrc.Modules.LOCAL))
    implementation (project(com.noto.buildsrc.Modules.REMOTE))
    implementation (project(com.noto.buildsrc.Modules.DATA))
    implementation (project(com.noto.buildsrc.Modules.DOMAIN))
    api(Libraries.KOIN)
}