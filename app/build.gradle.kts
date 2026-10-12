plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

// Valores públicos (clave anon protegida por RLS). Sobrescribibles con variables de entorno en CI.
val supabaseUrl = env("SUPABASE_URL") ?: "https://qqqudthqgpojbipsevna.supabase.co"
val supabaseKey = env("SUPABASE_ANON_KEY")
    ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFxcXVkdGhxZ3BvamJpcHNldm5hIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTE0NDIxODUsImV4cCI6MjEwNzAxODE4NX0.GDqWTmFMgu_DX7oZ3YGcf1HHgT7-FIOJ7n8ctjRUc-w"
val keystorePath = env("KEYSTORE_PATH")
val appVersion: String = rootProject.file("version.properties").takeIf { it.exists() }?.readText()
    ?.removePrefix("\uFEFF")?.lineSequence()?.firstOrNull { it.startsWith("VERSION_NAME=") }
    ?.substringAfter("=")?.trim() ?: "0.0.0"

android {
    namespace = "es.unocerobits.tlotraigo"
    compileSdk = 36

    defaultConfig {
        applicationId = "es.unocerobits.tlotraigo"
        minSdk = 21          // Android 5.0
        targetSdk = 36         // requisito de Google Play desde 31/08/2026
        versionCode = env("VERSION_CODE")?.toInt() ?: 1
        // Fuente única de la versión: version.properties (lo actualiza scripts/publicar.ps1)
        versionName = env("VERSION_NAME")?.removePrefix("v") ?: appVersion
        vectorDrawables.useSupportLibrary = true
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
        buildConfigField("String", "SUPPORT_PHONE", "\"+34600000000\"")
        buildConfigField("String", "CONTACT_EMAIL", "\"info@unocerobits.com\"")
        buildConfigField("String", "WEB_URL", "\"https://unocerobits.com/t-lo-traigo\"")
        buildConfigField("String", "PRIVACY_URL", "\"https://unocerobits.com/t-lo-traigo/privacidad.html\"")
    }

    signingConfigs {
        create("release") {
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = env("KEYSTORE_PASSWORD")
                keyAlias = env("KEY_ALIAS")
                keyPassword = env("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(if (keystorePath != null) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    // Versiones fijadas: últimas compatibles con minSdk 21.
    val bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(bom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
