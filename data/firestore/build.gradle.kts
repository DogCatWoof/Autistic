plugins {
    id("autistic.android-library")
}

android {
    namespace = "org.meow.autistic.data.firestore"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:auth"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
}
