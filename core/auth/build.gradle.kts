plugins {
    id("autistic.android-library")
}

android {
    namespace = "org.meow.autistic.core.auth"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.google.play.services.auth)
    implementation(libs.google.api.services.tasks)
    implementation(libs.google.api.services.calendar)
    implementation(libs.google.api.services.drive)
    implementation(libs.androidx.security.crypto)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
}
