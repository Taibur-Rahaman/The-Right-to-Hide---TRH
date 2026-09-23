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
        // Prefer Maven Central for Kotlin/JVM; Google only for AndroidX/Google artifacts.
        // Avoids hard-failing resolution when dl.google.com returns intermittent 502s
        // for artifacts that also live on Maven Central (e.g. kotlin-stdlib).
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx\\..*")
                includeGroup("com.google.mlkit")
                includeGroup("com.google.android.gms")
                includeGroup("com.google.firebase")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "KycLivenessSandbox"
include(":app")
