rootProject.name = "azuvotifier"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

listOf(
    "api", "common", "bukkit", "bungeecord",
    "velocity", "standalone", "sponge", "folia",
    "fabric"
).forEach {
    include(":azuvotifier-$it")
    project(":azuvotifier-$it").projectDir = file(it)
}