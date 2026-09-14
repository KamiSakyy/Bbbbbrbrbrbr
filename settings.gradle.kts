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
        // Единственный репозиторий, откуда берётся движок GeckoView (не WebView!).
        maven {
            name = "Mozilla"
            url = uri("https://maven.mozilla.org/maven2/")
        }
    }
}

rootProject.name = "YandexBrowser"
include(":app")
