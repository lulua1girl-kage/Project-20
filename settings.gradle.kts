pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "Project-20"
include(":apps:app-01-taskforge")
include(":apps:app-02-calendar")
include(":apps:app-03-clock")
include(":apps:app-04-taskforge2")
