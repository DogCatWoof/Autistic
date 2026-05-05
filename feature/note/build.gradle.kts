plugins {
    id("autistic.android-library-compose")
}

android {
    namespace = "org.meow.autistic.feature.note"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:ui"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.compose)
}
