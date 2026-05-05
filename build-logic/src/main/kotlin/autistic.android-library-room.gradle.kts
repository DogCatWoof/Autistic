plugins {
    id("autistic.android-library")
    id("com.google.devtools.ksp")
}

val libs = the<VersionCatalogsExtension>().named("libs")

dependencies {
    "implementation"(libs.findLibrary("androidx-room-runtime").get())
    "implementation"(libs.findLibrary("androidx-room-ktx").get())
    "ksp"(libs.findLibrary("androidx-room-compiler").get())
    "androidTestImplementation"(libs.findLibrary("androidx-room-testing").get())
}
