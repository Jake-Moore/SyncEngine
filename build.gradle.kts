import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

@Suppress("PropertyName")
val VERSION = "0.3.2.3"

plugins {
    id("java")
    id("java-library")
    id("io.github.goooler.shadow") version "8.1.7"
    id("maven-publish")
}

// Disable root project build
tasks.jar.get().enabled = false

// Export KamiCommonVer for use in all subprojects
val kamiCommonVer = "3.0.4.1"
ext {
    set("kamiCommonVer", kamiCommonVer)
}

allprojects {
    group = "com.kamikazejam.syncengine"
    version = VERSION
    description = "A data storage and synchronization library for Spigot plugins."

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://nexus.luxiouslabs.net/public")
    }

    // We want UTF-8 for everything
    tasks.withType<JavaCompile> {
        options.encoding = Charsets.UTF_8.name()
    }
    tasks.withType<Javadoc> {
        options.encoding = Charsets.UTF_8.name()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "io.github.goooler.shadow")
    apply(plugin = "maven-publish")

    // All modules use Java 21
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    dependencies {
        // Spigot (from public nexus)
        compileOnly("net.techcable.tacospigot:server:1.8.8-R0.2-REDUCED")

        // Annotation Processors
        //   Lombok
        compileOnly("org.projectlombok:lombok:1.18.32")
        annotationProcessor("org.projectlombok:lombok:1.18.32")
        testCompileOnly("org.projectlombok:lombok:1.18.32")
        testAnnotationProcessor("org.projectlombok:lombok:1.18.32")
        //   JetBrains
        compileOnly("org.jetbrains:annotations:24.1.0")
        testCompileOnly("org.jetbrains:annotations:24.1.0")
    }

    // Register a task to delete the jars in the libs folder
    tasks.register<Delete>("cleanLibs") {
        delete("build/libs")
    }

    // Define
    tasks.withType<ShadowJar> {
        // Do Not minimize -> messes up the spigot-jar which may need those classes
        // Apply common settings to all shadow jars
        archiveClassifier.set("")
        configurations = listOf(project.configurations.shadow.get())
    }
}

tasks {
    build.get().dependsOn("forceModuleBuild")
    // Define a custom task to fail the build with a message
    register("forceModuleBuild") {
        doFirst {
            throw GradleException("\n\n*****\nPlease use the build.sh script instead of ./gradlew build !\n*****\n\n")
        }
    }
}
