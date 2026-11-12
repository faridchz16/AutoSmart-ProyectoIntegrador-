import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services") version "4.4.2" apply false
}

if (file("google-services.json").isFile) {
    apply(plugin = "com.google.gms.google-services")
}

// TEC2.3: URL del backend por entorno.
// Cada integrante pone su IP en local.properties (no se sube al repositorio):
//   autosmart.baseUrl=http://192.168.1.63:8081/
// En el emulador se puede usar http://10.0.2.2:8081/
val localProps = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.isFile) archivo.inputStream().use { load(it) }
}
val backendUrl: String = (localProps.getProperty("autosmart.baseUrl")
    ?: providers.gradleProperty("autosmart.baseUrl").orNull
    ?: "http://192.168.1.63:8081/").let { if (it.endsWith("/")) it else "$it/" }

android {
    namespace = "com.tecsup.autosmart"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.tecsup.autosmart"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // TEC2.3: URL Base del Backend para BuildConfig (se toma de local.properties)
        buildConfigField("String", "BASE_URL", "\"$backendUrl\"")

        // Client ID Web de Google Auth Platform
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"613347565665-08u7d81kvsuoa1c4qm6qurvarj562uof.apps.googleusercontent.com\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true // Habilita BuildConfig para la variable BASE_URL (TEC.2)
    }
}

dependencies {
    // ... tus otras dependencias ...

    // Iconos extendidos para Jetpack Compose
    implementation("androidx.compose.material:material-icons-extended")
}
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("com.google.firebase:firebase-messaging:26.0.1")
    implementation("androidx.work:work-runtime:2.10.1")

    // --- LIBRERÍAS AUTOSMART (SPRINT 1) ---
    // 1. Navegación entre pantallas en Compose
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // 2. Retrofit + Gson para peticiones HTTP al Backend (Spring Boot)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // 3. Gestión de estado (ViewModel)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")

    // 4. TAREA T03.1: DataStore Preferences para la sesión persistente del JWT
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // 5. Autenticación con Google (Credential Manager & Google ID)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}