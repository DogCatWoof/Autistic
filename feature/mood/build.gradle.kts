plugins {
    id("autistic.android-library-compose")
}

android {
    namespace = "org.meow.autistic.feature.mood"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:ui"))
    implementation(project(":core:notifications"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
}
