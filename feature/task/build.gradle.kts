plugins {
    id("autistic.android-library-compose")
}

android {
    namespace = "org.meow.autistic.feature.task"
}

dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:auth"))
    implementation(project(":core:common"))
    implementation(project(":core:ui"))
    implementation(project(":core:notifications"))
    implementation(project(":data:sync"))
    implementation(project(":data:backup"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    testImplementation(libs.junit)
}
