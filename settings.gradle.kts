// Network note: this machine sits behind a secure-web-gateway that returns HTTP 403 for
// repo1.maven.org (and services.gradle.org). Google's GCS-hosted Maven Central mirror IS
// reachable and serves identical artifacts, so it is listed first. mavenCentral() is kept as
// a fallback for machines without that restriction — remove the mirror once IT allowlists
// repo1.maven.org.
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven(url = "https://maven-central.storage-download.googleapis.com/maven2/") {
            name = "MavenCentralMirror"
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven(url = "https://maven-central.storage-download.googleapis.com/maven2/") {
            name = "MavenCentralMirror"
        }
        mavenCentral()
    }
}

rootProject.name = "GlowUp"
include(":app")
