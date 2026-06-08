plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.google.services)
}

android {
    namespace = "com.sifa.sifa_go"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sifa.sifa_go"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        // Habilita el soporte para APIs modernas de Java (java.time) en versiones antiguas de Android
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.compose.foundation)
    // Librería para que java.time.LocalDateTime funcione en minSdk 24
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    // Manejo de permisos (Cámara, GPS, etc.) optimizado para Jetpack Compose
    implementation("com.google.accompanist:accompanist-permissions:0.28.0")

    // CameraX: Funciones de cámara (captura y visualización)
    val cameraxVersion = "1.6.0"
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Integración del ciclo de vida de la app con Compose
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Navegación entre pantallas en Jetpack Compose
    implementation("androidx.navigation:navigation-compose:2.9.7")

    // Coil: Carga y visualización de imágenes desde internet o archivos
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Retrofit: Cliente para realizar peticiones a APIs (HTTP)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Gson: Conversor de datos JSON recibidos de la API a clases de Kotlin
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Biometría: Autenticación por huella digital o rostro
    implementation("androidx.biometric:biometric:1.1.0")

    // Google Play Services: Servicios de ubicación y GPS
    implementation(libs.play.services.location)

    // ExifInterface: Manejo de metadatos de imágenes (ej. corregir rotación de fotos)
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Iconos, Material Design 3 y herramientas base de Jetpack Compose
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Dependencias para pruebas unitarias y de interfaz de usuario
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Dependencias para Firebase Messaging (Push notifications)
    implementation("com.google.firebase:firebase-messaging:24.1.0")
}