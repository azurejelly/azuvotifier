import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    alias(libs.plugins.shadow)
    alias(libs.plugins.pluginyml.bungee)
}

dependencies {
    api(project(":nuvotifier-api"))
    api(project(":nuvotifier-common"))
    compileOnly(libs.bungeecord)
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

bungee {
    name = "NuVotifier"
    version = project.version.toString()
    main = "com.vexsoftware.votifier.bungee.NuVotifierBungee"
    author = "azurejelly, Ichbinjoe, blakeman8192, Kramer, tuxed"
}
