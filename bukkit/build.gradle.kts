import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    `java-library`
    alias(libs.plugins.pluginyml.bukkit)
    alias(libs.plugins.shadow)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public")
}

dependencies {
    api(project(":nuvotifier-api"))
    api(project(":nuvotifier-common"))
    compileOnly(libs.paper)
    implementation(libs.jedis)
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("dist")

        val reloc = "com.vexsoftware.votifier.libs"
        relocate("redis.clients.jedis", "$reloc.redis.clients.jedis")
        relocate("org.json", "$reloc.json")
        relocate("org.apache.commons.pool2", "$reloc.apache.commons.pool2")
        relocate("org.slf4j", "$reloc.slf4j")
        relocate("io.netty", "$reloc.netty")
        relocate("com.google.gson", "$reloc.gson")

        exclude("GradleStart**")
        exclude(".cache");
        exclude("LICENSE*")
        exclude("META-INF/services/**")
        exclude("META-INF/maven/**")
        exclude("META-INF/versions/**")
        exclude("org/intellij/**")
        exclude("org/jetbrains/**")
        exclude("**/module-info.class")
    }

    named("assemble").configure {
        dependsOn("shadowJar")
    }
}

bukkit {
    name = "Votifier"
    description = "A plugin that gets notified when votes are made for the server on toplists."
    version = project.version.toString()
    main = "com.vexsoftware.votifier.NuVotifierBukkit"
    authors = listOf("azurejelly", "Ichbinjoe", "blakeman8192", "Kramer", "tuxed")
    apiVersion = "1.13"

    commands {
        register("nvreload") {
            description = "Reloads the NuVotifier configuration"
            permission = "nuvotifier.reload"
            permissionMessage = "You do not have permission to run this command."
            usage = "/nvreload"
        }

        register("testvote") {
            description = "Sends a test vote to the server"
            permission = "nuvotifier.testvote"
            permissionMessage = "You do not have permission to run this command."
            usage = "/testvote [username] [serviceName] [username] [address] [localTimestamp] [timestamp]"
        }
    }

    permissions {
        register("nuvotifier.reload") {
            description = "Allows you to reload the NuVotifier plugin"
            default = BukkitPluginDescription.Permission.Default.OP
        }

        register("nuvotifier.testvote") {
            description = "Allows you to send a test vote"
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}