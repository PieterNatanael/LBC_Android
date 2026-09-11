import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// ── Auto version (reads version.properties, never touch manually) ─────────────
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().also { props ->
    versionPropsFile.inputStream().use { props.load(it) }
}
val buildVersionCode = versionProps["VERSION_CODE"].toString().toInt()
val buildVersionName = versionProps["VERSION_NAME"].toString()

android {
    namespace = "com.example.bodycamoutdoorgear"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.bodycamz.app"
        minSdk = 24
        targetSdk = 36
        versionCode = buildVersionCode
        versionName = buildVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // ── Release signing ───────────────────────────────────────────────────────
    val keystorePropsFile = rootProject.file("keystore.properties")
    if (keystorePropsFile.exists()) {
        val keystoreProps = Properties().also { props ->
            keystorePropsFile.inputStream().use { props.load(it) }
        }
        signingConfigs {
            create("release") {
                keyAlias      = keystoreProps["KEY_ALIAS"].toString()
                keyPassword   = keystoreProps["KEY_PASSWORD"].toString()
                storeFile     = rootProject.file(keystoreProps["STORE_FILE"].toString())
                storePassword = keystoreProps["STORE_PASSWORD"].toString()
            }
        }
        buildTypes {
            release {
                signingConfig = signingConfigs.getByName("release")
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
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/DEPENDENCIES"
            )
        }
    }
}

// ── Bump version code after every release build ───────────────────────────────
// Runs automatically after bundleRelease or assembleRelease succeeds.
// Updates version.properties so the next build gets the next number.
tasks.register("incrementVersionCode") {
    doLast {
        val newCode = buildVersionCode + 1
        versionProps["VERSION_CODE"] = newCode.toString()
        versionPropsFile.outputStream().use { versionProps.store(it, null) }
        println("✅ Version code: $buildVersionCode → $newCode  (saved to version.properties)")
    }
}

tasks.matching { it.name in listOf("bundleRelease", "assembleRelease") }.configureEach {
    finalizedBy("incrementVersionCode")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    // Navigation
    implementation(libs.androidx.navigation.compose)
    // Media3 / ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    // Google Play Billing
    implementation(libs.billing.ktx)
    // Location
    implementation(libs.play.services.location)
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation("androidx.exifinterface:exifinterface:1.3.7")
    // QR code generation
    implementation(libs.zxing.core)
    // Ktor embedded HTTP server and client
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
