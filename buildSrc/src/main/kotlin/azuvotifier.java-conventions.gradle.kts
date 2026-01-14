plugins {
    id("java")
    `java-library`
}

val targetJavaVersion = 21

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(targetJavaVersion)
    }

    java {
        if (JavaVersion.current() < JavaVersion.toVersion(targetJavaVersion)) {
            toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
        }

        disableAutoTargetJvm()
        withJavadocJar()
        withSourcesJar()
    }

    withType<JavaCompile> {
        val disabledLint = listOf("processing", "path", "fallthrough", "serial")

        options.release.set(11)
        options.compilerArgs.addAll(listOf("-Xlint:all") + disabledLint.map { "-Xlint:-$it" })
        options.isDeprecation = true
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            tags(
                "apiNote:a:API Note:",
                "implSpec:a:Implementation Requirements:",
                "implNote:a:Implementation Note:"
            )
        }
    }

    named<Jar>("jar") {
        manifest {
            attributes("Implementation-Version" to rootProject.version)
        }
    }

    configurations.all {
        resolutionStrategy {
            cacheChangingModulesFor(5, "MINUTES")
        }
    }
}