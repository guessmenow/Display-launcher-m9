plugins {
    id("com.android.application")
}

import java.util.Properties

android {
    namespace = "com.oai.displaylauncher"
    compileSdk = 35

    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties()
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { keystoreProps.load(it) }
    }

    fun keystoreValue(key: String): String? {
        val fromFile = keystoreProps.getProperty(key)
        if (!fromFile.isNullOrBlank()) return fromFile
        val fromEnv = System.getenv(key)
        return if (fromEnv.isNullOrBlank()) null else fromEnv
    }

    defaultConfig {
        applicationId = "com.oai.displaylauncher"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "2.1-experimental"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    val hasSigning = !keystoreValue("M9_KEYSTORE_PATH").isNullOrBlank() &&
        !keystoreValue("M9_KEYSTORE_PASS").isNullOrBlank() &&
        !keystoreValue("M9_KEY_ALIAS").isNullOrBlank()

    if (hasSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(keystoreValue("M9_KEYSTORE_PATH")!!)
                storePassword = keystoreValue("M9_KEYSTORE_PASS")
                keyAlias = keystoreValue("M9_KEY_ALIAS")
                keyPassword = keystoreValue("M9_KEY_ALIAS_PASS") ?: keystoreValue("M9_KEYSTORE_PASS")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core:1.13.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
