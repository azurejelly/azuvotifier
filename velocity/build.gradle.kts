import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.velocity)
}

repositories {
    maven("https://nexus.velocitypowered.com/repository/maven-public/")
}

dependencies {
    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)
    implementation(libs.bstats.velocity)

    api(project(":nuvotifier-api"))
    api(project(":nuvotifier-common")) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
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
        exclude("**/module-info.class")
        exclude("*.yml")
    }

    named("assemble").configure {
        dependsOn("shadowJar")
    }

    runVelocity {
        velocityVersion("3.4.0-SNAPSHOT")
    }
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

val templateSource = file("src/main/templates")
val templateDest = layout.buildDirectory.dir("generated/sources/templates")
val generateTemplates = tasks.register<Copy>("generateTemplates") {
    inputs.properties(
        "version" to rootProject.version,
    )

    from(templateSource)
    into(templateDest)
    rename { "com/vexsoftware/votifier/velocity/utils/$it" }
    expand(inputs.properties)
}

sourceSets.main {
    java.srcDir(generateTemplates.map { it.outputs })
}