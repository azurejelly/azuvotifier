import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    `java-library`
    alias(libs.plugins.pluginyml.bukkit)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public")
}

dependencies {
    api(project(":azuvotifier-common"))
    api(project(":azuvotifier-folia"))

    compileOnly(libs.paper)
    implementation(libs.jedis)
    implementation(libs.bstats.bukkit)
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("dist")

        val reloc = "com.vexsoftware.votifier.libs"
        relocate("org.bstats", "$reloc.bstats")
        relocate("redis.clients.jedis", "$reloc.jedis")
        relocate("org.json", "$reloc.json")
        relocate("org.apache.commons.pool2", "$reloc.pool2")
        relocate("io.netty", "$reloc.netty")
        relocate("com.google.gson", "$reloc.gson")
        relocate("com.google.errorprone", "$reloc.errorprone")

        exclude("GradleStart**")
        exclude(".cache");
        exclude("LICENSE*")
        exclude("META-INF/services/**")
        exclude("META-INF/maven/**")
        exclude("META-INF/versions/**")
        exclude("org/intellij/**")
        exclude("org/jetbrains/**")
        exclude("org/slf4j/**")
        exclude("**/module-info.class")
    }

    named("assemble").configure {
        dependsOn("shadowJar")
    }

    runServer {
        minecraftVersion("1.21.11")
        jvmArgs("-Dcom.mojang.eula.agree=true")

        downloadPlugins {
            modrinth("fancyfirework", "1.4.4")
        }
    }
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

bukkit {
    name = "Votifier"
    description = "A plugin that gets notified when votes are made for the server on toplists."
    version = project.version.toString()
    main = "com.vexsoftware.votifier.NuVotifierBukkit"
    authors = listOf("azurejelly", "Ichbinjoe", "blakeman8192", "Kramer", "tuxed")
    apiVersion = "1.13"
    foliaSupported = true

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