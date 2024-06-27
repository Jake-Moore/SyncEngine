import java.time.Instant
import java.time.format.DateTimeFormatter

plugins {
    // unique plugins for this module
}

val kamiCommonVer = (project.property("kamiCommonVer") as String)
dependencies {
    // unique dependencies for this module
    compileOnly(project(":spigot-library"))
    shadow(files(project(":spigot-library")
        .dependencyProject.layout.buildDirectory.dir("unpacked-shadow"))
    )
    // Include KamiCommon utils in the spigot jar (shaded/relocated)
    //  So we don't need to depend on the KamiCommon plugin
    shadow("com.kamikazejam.kamicommon:spigot-utils:$kamiCommonVer")
}

tasks {
    publish.get().dependsOn(build)
    build.get().dependsOn(shadowJar)
    shadowJar.get().dependsOn("cleanLibs")

    // Configure plugin.yml processing
    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "name" to rootProject.name,
            "version" to project.version,
            "description" to project.description,
            "date" to DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            "kamicommonVersion" to (project.property("kamiCommonVer") as String),
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
        filesMatching("**/version.json") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        configurations = listOf(project.configurations.shadow.get())

        relocate("com.kamikazejam.kamicommon", "shaded.com.kamikazejam.syncengine.kc")
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

// not required, but useful to see what's in the jar
//tasks.register<Copy>("unpackShadow") {
//    dependsOn(tasks.shadowJar)
//    from(zipTree(layout.buildDirectory.dir("libs").map { it.file(tasks.shadowJar.get().archiveFileName) }))
//    into(layout.buildDirectory.dir("unpacked-shadow"))
//}
//tasks.getByName("build").finalizedBy(tasks.getByName("unpackShadow"))