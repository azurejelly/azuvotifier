plugins {
    alias(libs.plugins.fabric.loom)
    `maven-publish`
}

base {
    archivesName = "azuvotifier-fabric"
}

loom {
    splitEnvironmentSourceSets()
}

repositories {
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.
}

dependencies {
    minecraft(libs.fabric.minecraft)
    mappings(
        variantOf(libs.fabric.yarn) {
            classifier("v2")
        }
    )

    implementation(project(":nuvotifier-api"))
    implementation(project(":nuvotifier-common"))

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", libs.versions.fabric.minecraft.get())
    inputs.property("loader_version", libs.versions.fabric.loader.get())
    filteringCharset = "utf-8"

    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to project.version,
                "minecraft_version" to libs.versions.fabric.minecraft.get(),
                "loader_version" to libs.versions.fabric.loader.get()
            )
        )
    }
}


java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }

    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}
