plugins {
    java
    kotlin("jvm") version "2.2.0-RC"
}

group = "ru.itmo.kotlin.plugin"
version = "0.1"

val kotlinVersion: String by project.properties

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test {
        java.setSrcDirs(listOf("test", "test-gen"))
        resources.setSrcDirs(listOf("testResources"))
    }
}
