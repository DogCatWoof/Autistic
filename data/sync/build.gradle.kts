plugins {
    id("autistic.android-library")
}

android {
    namespace = "org.meow.autistic.data.sync"

}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:auth"))
    implementation(project(":data:firestore"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.tasks)
    implementation(libs.google.api.services.calendar)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.koin.android)
}
