import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

configurations.all {
    resolutionStrategy {
        force("androidx.concurrent:concurrent-futures:1.2.0")
        force("androidx.concurrent:concurrent-futures-ktx:1.2.0")
    }
}

val localProps = Properties()
val localPropsFile = rootProject.file("local-only/local.properties")
if (localPropsFile.exists()) {
    localPropsFile.inputStream().use { stream -> localProps.load(stream) }
}

tasks.matching { it.name.startsWith("process") && it.name.endsWith("GoogleServices") }.configureEach {
    doFirst {
        copy {
            from(rootProject.file("local-only/google-services.json"))
            into(layout.projectDirectory)
        }
    }
}

android {
    namespace = "org.meow.autistic"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.meow.autistic"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "ANTHROPIC_API_KEY", "\"${localProps.getProperty("anthropic.api.key", "")}\"")
        buildConfigField("String", "USDA_API_KEY", "\"${localProps.getProperty("usda.api.key", "")}\"")
        buildConfigField("String", "FIREBASE_WEB_CLIENT_ID", "\"${localProps.getProperty("firebase.web.client.id", "")}\"")
    }

    packaging {
        resources {
            excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/NOTICE")
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:database"))
    implementation(project(":core:auth"))
    implementation(project(":core:notifications"))
    implementation(project(":data:sync"))
    implementation(project(":data:firestore"))
    implementation(project(":data:backup"))
    implementation(project(":feature:conversation"))
    implementation(project(":feature:mood"))
    implementation(project(":feature:note"))
    implementation(project(":feature:health"))
    implementation(project(":feature:task"))
    implementation(project(":feature:sequence"))
    implementation(project(":feature:food"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Google Sign-In / OAuth
    implementation(libs.google.play.services.auth)

    // Google API Client + Tasks + Calendar
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.tasks)
    implementation(libs.google.api.services.calendar)
    implementation(libs.google.api.services.drive)

    // Encrypted token storage
    implementation(libs.androidx.security.crypto)

    // HTTP logging at boundary
    implementation(libs.okhttp.logging.interceptor)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit barcode scanning
    implementation(libs.mlkit.barcode.scanning)

    // JSON serialisation for product data
    implementation(libs.gson)

    // Health Connect
    implementation(libs.health.connect.client)

    // DataStore for sync timestamps
    implementation(libs.androidx.datastore.preferences)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)

    // Koin DI
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
