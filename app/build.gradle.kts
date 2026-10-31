import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val lp = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

val keystorePass = lp.getProperty("KEYSTORE_PASS") ?: System.getenv("KEYSTORE_PASS")
val aliasName = lp.getProperty("ALIAS_NAME") ?: System.getenv("ALIAS_NAME") ?: "twi_from_67"
val aliasPass = lp.getProperty("ALIAS_PASS") ?: System.getenv("ALIAS_PASS") ?: keystorePass
val releaseKeystore = rootProject.file("release.keystore")

android {
    namespace = "Akari.Twitter.share"
    compileSdk = 37

    defaultConfig {
        applicationId = "Akari.Twitter.share"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "release-1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    signingConfigs {
        create("release") {
            if (releaseKeystore.exists() && !keystorePass.isNullOrBlank()) {
                storeFile = releaseKeystore
                storePassword = keystorePass
                keyAlias = aliasName
                keyPassword = aliasPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.findByName("release")?.let {
                if (it.storeFile != null) {
                    signingConfig = it
                }
            }
        }
        debug {
            signingConfigs.findByName("release")?.let {
                if (it.storeFile != null) {
                    signingConfig = it
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        outputs.all {
            val output = this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output?.let {
                it.outputFileName = "Twi-From-67-${versionName}.apk"
            }
        }
    }
}

dependencies {
    // LSPosed / Xposed API (API 101/102 backwards compatible with API 82)
    compileOnly(files("libs/api-82.jar"))
    compileOnly("de.robv.android.xposed:api:82")

    // AndroidX & Material 3 Expressive UI
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
}