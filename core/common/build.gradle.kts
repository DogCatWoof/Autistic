plugins {
    id("autistic.android-library")
}

android {
    namespace = "org.meow.autistic.core.common"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
