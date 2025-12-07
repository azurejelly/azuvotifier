// i fucking hate fabric
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.task.RemapJarTask

plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.shadow)
    `maven-publish`
}

base {
    archivesName = "azuvotifier-fabric"
}

loom {
    splitEnvironmentSourceSets()
}

configurations {
    create("shade")
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

    modImplementation(libs.fabric.permissions)
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)

    api(project(":nuvotifier-api"))
    api(project(":nuvotifier-common"))
    implementation(libs.jedis)
    implementation(libs.configurate)

    add("shade", project(":nuvotifier-api"))
    add("shade", project(":nuvotifier-common"))
    add("shade", libs.fabric.permissions) // this is shading fabric api too
    add("shade", libs.jedis)
    add("shade", libs.configurate)
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

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("shaded")
    configurations = listOf(project.configurations["shade"])

    val pkg = "com.vexsoftware.votifier.libs"
    relocate("com.google.errorprone", "$pkg.errorprone")
    relocate("com.google.gson", "$pkg.gson")
    relocate("io.netty", "$pkg.netty")
    relocate("io.leangen.geantyref", "$pkg.geantyref")
    relocate("net.kyori.option", "$pkg.option")
    relocate("org.apache.commons.pool2", "$pkg.pool2")
    relocate("org.json", "$pkg.json")
    relocate("org.spongepowered.configurate", "$pkg.configurate")
    relocate("redis.clients.jedis", "$pkg.jedis")
    relocate("me.lucko.fabric.api.permissions.v0", "$pkg.permissions") // should this be relocated?

    exclude("fabric-installer*")
    exclude("LICENSE-*/")
    exclude("LICENSE_*/")
    exclude("ui/icon/**")
    exclude("assets/**")
    exclude("net/fabricmc/**")
    exclude("GradleStart**")
    exclude(".cache")
    exclude("META-INF/services/**")
    exclude("META-INF/maven/**")
    exclude("META-INF/versions/**")
    exclude("META-INF/proguard/**")
    exclude("META-INF/jars/**")
    exclude("org/intellij/**")
    exclude("org/jetbrains/**")
    exclude("org/slf4j/**")
    exclude("**/module-info.class")
    exclude("mappings/mappings.tiny")
}

tasks.register<RemapJarTask>("remapShadowJar") {
    val shadowJar = tasks.getByName<ShadowJar>("shadowJar")
    dependsOn(shadowJar)
    inputFile.set(shadowJar.archiveFile)
    archiveClassifier.set("dist")
    addNestedDependencies.set(true)
}

tasks.named("assemble").configure {
    dependsOn("remapShadowJar")
}