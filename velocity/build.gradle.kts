import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.velocity)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
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

        val pkg = "com.vexsoftware.votifier.libs"
        relocate("org.bstats", "$pkg.bstats")
        relocate("redis.clients.jedis", "$pkg.jedis")
        relocate("org.json", "$pkg.json")
        relocate("org.apache.commons.pool2", "$pkg.pool2")
        relocate("io.netty", "$pkg.netty")
        relocate("com.google.gson", "$pkg.gson")
        relocate("com.google.errorprone", "$pkg.errorprone")

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