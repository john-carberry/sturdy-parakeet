pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "diy-brick"

include(":app")
include(":core:model")
include(":core:security")
include(":core:data")
include(":core:ui")
include(":feature:nfc")
include(":feature:qr")
include(":feature:service")
include(":feature:blocker")
