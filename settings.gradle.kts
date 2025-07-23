rootProject.name = "nuvotifier"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

listOf(
    "api", "common", "bukkit", "bungeecord",
    "velocity", "standalone", "sponge", "folia"
).forEach {
    include(":nuvotifier-$it")
    project(":nuvotifier-$it").projectDir = file(it)
}