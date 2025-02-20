pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
    }
    
}
rootProject.name = "version-overloading-plugin"

include("plugin-annotations")
