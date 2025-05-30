
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "2.2.0-RC"
}

group = "org.demiurg906.kotlin.plugin"
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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

dependencies {
    "org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion".let {
        compileOnly(it)
        testImplementation(it)
    }

    testRuntimeOnly("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-annotations-jvm:$kotlinVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:$kotlinVersion")
    testImplementation("junit:junit:4.13.2")

    testImplementation(platform("org.junit:junit-bom:5.8.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-commons")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.platform:junit-platform-runner")
    testImplementation("org.junit.platform:junit-platform-suite-api")
}

tasks.test {
    dependsOn(project(":plugin-annotations").tasks.getByName("jar"))
    useJUnitPlatform()
    doFirst {
        setLibraryProperties("kotlin-stdlib", "org.jetbrains.kotlin.test.kotlin-stdlib", "kotlin.full.stdlib.path", "kotlin.minimal.stdlib.path")

        setLibraryProperties("kotlin-stdlib-jdk8", "org.jetbrains.kotlin.test.kotlin-stdlib-jdk8", "kotlin.full.stdlib.path", "kotlin.minimal.stdlib.path")

        setLibraryProperties("kotlin-reflect", "org.jetbrains.kotlin.test.kotlin-reflect", "kotlin.reflect.jar.path")

        setLibraryProperties("kotlin-test", "org.jetbrains.kotlin.test.kotlin-test", "kotlin.test.jar.path")
        setLibraryProperties("kotlin-script-runtime", "org.jetbrains.kotlin.test.kotlin-script-runtime", "kotlin.script.runtime.path")
        setLibraryProperties("kotlin-annotations-jvm", "org.jetbrains.kotlin.test.kotlin-annotations-jvm")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        optIn.add("org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI")
    }
}

val generateTests by tasks.creating(JavaExec::class) {
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("org.demiurg906.kotlin.plugin.GenerateTestsKt")
}

val compileTestKotlin by tasks.getting {
    doLast {
        generateTests.exec()
    }
}

fun Test.setLibraryProperties(jarName: String, vararg propNames: String){
    val path = project.configurations
        .testRuntimeClasspath.get()
        .files
        .find { """$jarName-\d.*jar""".toRegex().matches(it.name) }
        ?.absolutePath
        ?: return

    for (propName in propNames) {
        systemProperty(propName, path)
    }
}
