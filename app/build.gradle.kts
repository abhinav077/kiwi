import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

val keystoreProperties = Properties().apply {
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use(::load)
    }
}

val releaseSigningConfigured = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { !keystoreProperties.getProperty(it).isNullOrBlank() }

fun localProperty(name: String): String = localProperties.getProperty(name, "")
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

room {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.abhinavsirohi.kiwi"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.abhinavsirohi.kiwi"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "SUPABASE_URL", "\"${localProperty("SUPABASE_URL")}\"")
        buildConfigField(
            "String",
            "SUPABASE_PUBLISHABLE_KEY",
            "\"${localProperty("SUPABASE_PUBLISHABLE_KEY")}\"",
        )
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localProperty("GOOGLE_WEB_CLIENT_ID")}\"",
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.android)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
