pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Autistic"
include(":app")
include(":core:common")
include(":core:auth")
include(":core:database")
include(":core:ui")
include(":core:notifications")
include(":data:sync")
include(":data:firestore")
include(":feature:task")
include(":feature:note")
include(":feature:mood")
include(":feature:sequence")
include(":feature:conversation")
