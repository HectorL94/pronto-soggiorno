import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

room {
    // Esquemas versionados de la base de datos: necesarios para migraciones seguras.
    schemaDirectory("$projectDir/schemas")
}

/**
 * Firma de publicación. Los datos de la clave están en `keystore.properties` (raíz del proyecto),
 * que NUNCA se sube al repositorio (.gitignore). Plantilla: `keystore.properties.example`.
 * Si el archivo no existe (p. ej. en F-Droid, que firma con su propia clave), el APK de
 * publicación se genera sin firmar.
 */
val releaseSigning: Properties? = providers
    .fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
    .asText.orNull
    ?.let { text -> Properties().apply { load(text.reader()) } }

android {
    namespace = "app.prontosoggiorno"
    compileSdk = 37

    defaultConfig {
        // Identidad pública de la app (Google Play, F-Droid). NO cambiar después de publicar:
        // sería otra app distinta para Android y para las tiendas.
        // El paquete del código (`namespace`) es interno y puede ser distinto.
        applicationId = "io.github.hectorl94.prontosoggiorno"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = file(releaseSigning.getProperty("storeFile"))
                storePassword = releaseSigning.getProperty("storePassword")
                keyAlias = releaseSigning.getProperty("keyAlias")
                keyPassword = releaseSigning.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Pseudo-idiomas para probar textos largos (en-XA) y derecha a izquierda (ar-XB).
            isPseudoLocalesEnabled = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        // Genera la lista de idiomas para el selector de idioma por app de Android 13+.
        generateLocaleConfig = true
    }

    dependenciesInfo {
        // Sin bloque de metadatos de dependencias firmado por Google (requisito de F-Droid).
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    // Serialización (rutas de navegación): versión reciente y única para la app y los tests
    // (room-testing necesita una más nueva que la que trae Navigation).
    implementation(platform(libs.kotlinx.serialization.bom))

    // Una sola versión de corrutinas para la app y sus tests.
    implementation(platform(libs.kotlinx.coroutines.bom))
    implementation(libs.kotlinx.coroutines.android)
    androidTestImplementation(platform(libs.kotlinx.coroutines.bom))

    // Tests instrumentados: se ejecutan en el emulador/teléfono (Keystore y Room reales).
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    // Espresso reciente: la versión que trae Compose no funciona en Android 37.
    androidTestImplementation(libs.androidx.test.espresso.core)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
