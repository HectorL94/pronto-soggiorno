pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ProntoSoggiorno"

// :core  -> "cerebro" en Kotlin puro: red, parser y clasificación (sin Android, sin UI).
// :app   -> app Android: pantallas (Compose), datos locales, trabajo en segundo plano.
include(":core")
include(":app")
