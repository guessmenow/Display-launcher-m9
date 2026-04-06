plugins {
    id("com.android.application")
}

import java.util.Properties
import com.android.build.gradle.internal.api.BaseVariantOutputImpl

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
        versionName = "2.1"

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
                storeType = keystoreValue("M9_KEYSTORE_TYPE") ?: "JKS"
                keyAlias = keystoreValue("M9_KEY_ALIAS")
                keyPassword = keystoreValue("M9_KEY_ALIAS_PASS") ?: keystoreValue("M9_KEYSTORE_PASS")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
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

android.applicationVariants.all {
    val vName = versionName ?: "unknown"
    outputs.all {
        val output = this as BaseVariantOutputImpl
        output.outputFileName = "DisplayLauncher_${vName}.apk"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core:1.13.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
