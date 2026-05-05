plugins {
    id("autistic.android-library-compose")
}

android {
    namespace = "org.meow.autistic.feature.health"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.health.connect.client)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
}
