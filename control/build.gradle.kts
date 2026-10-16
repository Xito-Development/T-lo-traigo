// Panel de control para Android: la misma interfaz que el programa de Windows (desktop/renderer) dentro de un WebView,
// con un puente nativo para la sesión cifrada, las comprobaciones de red y las notificaciones.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }
val keystorePath = env("KEYSTORE_PATH")
val panelVersion: String = env("VERSION_NAME")?.removePrefix("v") ?: rootProject.file("version.properties").readText().removePrefix("\uFEFF")
    .lineSequence().first { it.startsWith("VERSION_NAME=") }.substringAfter("=").trim()

android {
    namespace = "es.unocerobits.tlotraigo.control"
    compileSdk = 36
    defaultConfig {
        applicationId = "es.unocerobits.tlotraigo.control"
        minSdk = 23
        targetSdk = 36
        versionCode = env("VERSION_CODE")?.toInt() ?: 1
        versionName = panelVersion
    }
    signingConfigs {
        create("release") {
            if (keystorePath != null) {
                storeFile = file(keystorePath); storePassword = env("KEYSTORE_PASSWORD"); keyAlias = env("KEY_ALIAS"); keyPassword = env("KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName(if (keystorePath != null) "release" else "debug")
        }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    // La interfaz se copia del programa de Windows en cada compilación: un solo diseño para los dos
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/panel"))
}
val copyPanel by tasks.registering(Copy::class) {
    from(rootProject.file("desktop/renderer")); into(layout.buildDirectory.dir("generated/panel/panel"))
}
tasks.named("preBuild") { dependsOn(copyPanel) }

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
