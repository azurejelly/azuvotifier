import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    `java-library`
    alias(libs.plugins.shadow)
    alias(libs.plugins.sponge)
}

repositories {
    maven("https://repo.spongepowered.org/maven/")
}

sponge {
    apiVersion("11.0.0")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }

    license("GNU General Public License v3.0")

    plugin("nuvotifier") {
        displayName("NuVotifier")
        version(project.version.toString())
        entrypoint("com.vexsoftware.votifier.sponge.NuVotifierSponge")
        description("Safe, smart, and secure Votifier server plugin")
        links {
            source("https://github.com/azurejelly/azuvotifier")
            issues("https://github.com/azurejelly/azuvotifier/issues")
        }
        contributor("Ichbinjoe") {
            description("Lead Developer")
        }
        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
            version("7.2.0")
        }
    }
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

dependencies {
    compileOnly(libs.spongeapi)
    implementation(libs.jedis)

    api(project(":nuvotifier-api"))
    api(project(":nuvotifier-common"))
}