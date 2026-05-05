plugins {
    id("autistic.android-library")
}

android {
    namespace = "org.meow.autistic.data.backup"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:auth"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)
    implementation(libs.koin.android)
}
