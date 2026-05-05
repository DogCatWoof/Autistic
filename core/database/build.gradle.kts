plugins {
    id("autistic.android-library-room")
}

android {
    namespace = "org.meow.autistic.core.database"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.kotlinx.coroutines.android)
}
