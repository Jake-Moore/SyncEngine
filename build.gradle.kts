import java.time.Instant
import java.time.format.DateTimeFormatter

plugins {
    id("java")
    id("java-library")
    id("io.github.goooler.shadow") version "8.1.7"
    id("maven-publish")
}

group = "com.kamikazejam"
version = "0.2.1"
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

    // Internal Libraries
    shadow("org.mongojack:mongojack:5.0.0")
    shadow("io.lettuce:lettuce-core:6.3.2.RELEASE")
    // KamiCommon (spigot utils) - for MultiVersion support
    shadow("com.kamikazejam.kamicommon:spigot-utils:$kamiCommonVer")

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
        relocate("com.fasterxml.jackson", "shaded.com.kamikazejam.syncengine.jackson")
        relocate("io.lettuce.core", "shaded.com.kamikazejam.syncengine.lettuce.core")
        relocate("com.mongodb", "shaded.com.kamikazejam.syncengine.mongodb")
        relocate("com.kamikazejam.kamicommon", "shaded.com.kamikazejam.syncengine.kc")
        // TODO Other relocations that didn't get picked-up in the script below
//        relocate("org.bson", "shaded.com.kamikazejam.syncengine.bson")
//        relocate("nonapi.io.github.classgraph", "shaded.com.kamikazejam.syncengine.classgraph")
//        relocate("reactor", "shaded.com.kamikazejam.syncengine.reactor")
//        relocate("javax.annotation", "shaded.com.kamikazejam.syncengine.javax.annotation")
//        relocate("org.jetbrains.annotations", "shaded.com.kamikazejam.syncengine.jetbrains.annotations")
//        relocate("org.objectweb.asm", "shaded.com.kamikazejam.syncengine.objectweb.asm")
//        relocate("edu.umd", "shaded.com.kamikazejam.syncengine.edu.umd")

        // Dynamically relocate all dependencies
        doFirst {
            project.configurations.getByName("shadow").resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
                val moduleId = artifact.moduleVersion.id
                val originalPackage = moduleId.group.replace('.', '/')
                val targetPackage = "shaded/com/kamikazejam/syncengine/libs/$originalPackage"
                relocate(originalPackage, targetPackage)
            }
        }
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
            url = uri("https://nexus.luxiouslabs.net/private/")
            credentials {
                username = System.getenv("LUXIOUS_NEXUS_USER")
                password = System.getenv("LUXIOUS_NEXUS_PASS")
            }
        }
    }
}