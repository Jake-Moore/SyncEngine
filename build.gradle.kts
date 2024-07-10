import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.time.Instant
import java.time.format.DateTimeFormatter

@Suppress("PropertyName")
val VERSION = "0.5.3-b4"

plugins {
    id("java")
    id("java-library")
    id("io.github.goooler.shadow") version "8.1.7"
    id("maven-publish")
}

// Export KamiCommonVer for use in all subprojects
val kamiCommonVer = "3.1.0.6"

group = "com.kamikazejam"
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

// All modules use Java 21
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    // Spigot (from public nexus)
    compileOnly("net.techcable.tacospigot:server:1.8.8-R0.2-REDUCED")

    // KamiCommon
    compileOnly("com.kamikazejam.kamicommon:spigot-utils:$kamiCommonVer")
    compileOnly("com.kamikazejam.kamicommon:generic-jar:$kamiCommonVer")

    // MongoJack
    shadow("org.mongojack:mongojack:5.0.0")

    // Annotation Processors
    //   Lombok
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    testCompileOnly("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")
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

tasks {
    build.get().dependsOn(shadowJar)
    shadowJar.get().dependsOn("cleanLibs")

    shadowJar {
        // Relocations
        // don't relocate jackson
        relocate("com.mongodb", "shaded.com.kamikazejam.syncengine.mongodb")
        relocate("org.mongojack", "shaded.com.kamikazejam.syncengine.mongojack")

        // Other relocations (like transitive dependencies)
        relocate("org.bson", "shaded.com.kamikazejam.syncengine.bson")
        relocate("org.slf4j", "shaded.com.kamikazejam.syncengine.slf4j")
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "name" to rootProject.name,
            "version" to project.version,
            "description" to project.description,
            "date" to DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
        filesMatching("**/version.json") {
            expand(props)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = rootProject.group.toString()
            artifactId = project.name
            version = rootProject.version.toString()
            from(components["java"])

            // Customize the generated POM
            pom.withXml {
                val dependenciesNode = asNode().appendNode("dependencies")
                project.configurations.getByName("shadow").dependencies.forEach {
                    val dependencyNode = dependenciesNode.appendNode("dependency")
                    dependencyNode.appendNode("groupId", it.group)
                    dependencyNode.appendNode("artifactId", it.name)
                    dependencyNode.appendNode("version", it.version)
                }

                // Remove the shaded dependencies from the POM
                asNode().remove(dependenciesNode)
            }
        }
    }

    repositories {
        maven {
            url = uri("https://nexus.luxiouslabs.net/public")
            credentials {
                username = System.getenv("LUXIOUS_NEXUS_USER")
                password = System.getenv("LUXIOUS_NEXUS_PASS")
            }
        }
    }
}


tasks.register<Copy>("unpackShadow") {
    dependsOn(tasks.shadowJar)
    from(zipTree(layout.buildDirectory.dir("libs").map { it.file(tasks.shadowJar.get().archiveFileName) }))
    into(layout.buildDirectory.dir("unpacked-shadow"))
}
tasks.getByName("build").finalizedBy(tasks.getByName("unpackShadow"))