import java.time.Instant
import java.time.format.DateTimeFormatter

plugins {
    id("java")
    id("java-library")
    id("io.github.goooler.shadow") version "8.1.7"
    id("maven-publish")
}

group = "com.kamikazejam"
version = "0.0.8"
description = "A data storage and synchronization library for Spigot plugins."

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://nexus.luxiouslabs.net/public")
}

val kamiCommonVer = "3.0.3.9"
dependencies {
    // Spigot (from public nexus)
    compileOnly("net.techcable.tacospigot:server:1.8.8-R0.2-REDUCED")
    // KamiCommon (spigot dependency) - for MultiVersion support
    compileOnly("com.kamikazejam.kamicommon:spigot-jar:$kamiCommonVer")

    // Internal Libraries
    shadow("dev.morphia.morphia:morphia-core:2.4.14")
    shadow("io.lettuce:lettuce-core:6.3.2.RELEASE")

    // Annotation Processors
    //   Lombok
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    testCompileOnly("org.projectlombok:lombok:1.18.32")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.32")
    //   JetBrains
    compileOnly("org.jetbrains:annotations:24.1.0")
}

// We want UTF-8 for everything
tasks.withType<JavaCompile> {
    options.encoding = Charsets.UTF_8.name()
}
tasks.withType<Javadoc> {
    options.encoding = Charsets.UTF_8.name()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    build.get().dependsOn(shadowJar)

    shadowJar {
        minimize()
        archiveClassifier.set("")
        configurations = listOf(project.configurations.shadow.get())
        // Relocations
        relocate("dev.morphia", "shaded.com.kamikazejam.syncengine.morphia")
        relocate("io.lettuce.core", "shaded.com.kamikazejam.syncengine.lettuce.core")
        relocate("com.mongodb", "shaded.com.kamikazejam.syncengine.mongodb")
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "name" to rootProject.name,
            "version" to project.version,
            "description" to project.description,
            "date" to DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            "kamicommonVersion" to kamiCommonVer,
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
        filesMatching("**/properties.json") {
            expand(props)
        }
        filesMatching("**/version.json") {
            expand(props)
        }
    }
}

// Publishing to Nexus
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = rootProject.group.toString()
            artifactId = project.name
            version = rootProject.version.toString()
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://nexus.luxiouslabs.net/private/")
            credentials {
                username = System.getenv("LUXIOUS_NEXUS_USER")
                password = System.getenv("LUXIOUS_NEXUS_PASS")
            }
        }
    }
}