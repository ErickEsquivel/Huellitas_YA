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

rootProject.name = "Huellitas YA!"
include(":app")

include(":core:model")
include(":core:common")
include(":core:domain")
include(":core:data")
include(":core:designsystem")
include(":core:ui")
include(":core:testing")

include(":feature:auth")
include(":feature:home")
include(":feature:calendar")
include(":feature:profile")
include(":feature:settings")
