import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    alias(libs.plugins.shadow)
    alias(libs.plugins.pluginyml.bungee)
    alias(libs.plugins.run.waterfall)
}

dependencies {
    api(project(":nuvotifier-api"))
    api(project(":nuvotifier-common"))
    compileOnly(libs.bungeecord)
    implementation(libs.jedis)
    implementation(libs.bstats.bungeecord)
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
    }

    named("assemble").configure {
        dependsOn("shadowJar")
    }

    runWaterfall {
        waterfallVersion("1.20")
    }
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(21)
    }
    jvmArgs("-XX:+AllowEnhancedClassRedefinition")
}

bungee {
    name = "NuVotifier"
    version = project.version.toString()
    main = "com.vexsoftware.votifier.bungee.NuVotifierBungee"
    author = "azurejelly, Ichbinjoe, blakeman8192, Kramer, tuxed"
}
