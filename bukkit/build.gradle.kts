import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    `java-library`
    alias(libs.plugins.pluginyml.bukkit)
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

repositories {
    maven {
        name = "Paper"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    api(project(":azuvotifier-common"))

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
        minecraftVersion("1.21.1")
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
    description = "Yet another Votifier fork with various enhancements, such as Redis forwarding and support for additional platforms."
    version = project.version.toString()
    main = "com.vexsoftware.votifier.NuVotifierBukkit"
    authors = listOf("azurejelly", "Ichbinjoe", "blakeman8192", "Kramer", "tuxed")
    apiVersion = "1.13"
    foliaSupported = true

    commands {
        register("votifier") {
            description = "Main azuvotifier command."
            aliases = listOf("azuvotifier")
        }
    }

    permissions {
        register("azuvotifier.more-info") {
            description = "Allows you to see server information when running the main azuvotifier command."
            default = BukkitPluginDescription.Permission.Default.OP
        }

        register("azuvotifier.reload") {
            description = "Allows you to reload the azuvotifier plugin"
            default = BukkitPluginDescription.Permission.Default.OP
        }

        register("azuvotifier.test") {
            description = "Allows you to send a test vote"
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}