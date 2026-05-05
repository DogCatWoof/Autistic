plugins {
    id("autistic.android-library-compose")
}

android {
    namespace = "org.meow.autistic.feature.conversation"
}

dependencies {
    implementation(project(":core:ui"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.gson)
    implementation(libs.koin.compose)
}
