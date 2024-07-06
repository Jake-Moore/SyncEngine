import java.time.Instant
import java.time.format.DateTimeFormatter

plugins {
    // unique plugins for this module
}

dependencies {
    // unique dependencies for this module
    shadow("org.mongojack:mongojack:5.0.0")
}

tasks {
    publish.get().dependsOn(build)
    build.get().dependsOn(shadowJar)
    shadowJar.get().dependsOn("cleanLibs")

    shadowJar {
        // Relocations
        relocate("com.fasterxml.jackson", "shaded.com.kamikazejam.syncengine.jackson")
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
