import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

plugins {
    id("java")
    alias(libs.plugins.idea)
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven("https://repo.glaremasters.me/repository/public")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }

    dependencies {
        val libs = rootProject.libs

        compileOnly(libs.lombok)
        annotationProcessor(libs.lombok)

        testCompileOnly(libs.lombok)
        testAnnotationProcessor(libs.lombok)
        testImplementation(platform(libs.junit.bom))
        testImplementation(libs.junit.jupiter)
        testRuntimeOnly(libs.junit.launcher)
    }

    configurations.all {
        resolutionStrategy {
            cacheChangingModulesFor(5, "MINUTES")
        }
    }

    tasks {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
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

        withType<Test>().configureEach {
            useJUnitPlatform()
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
    }
}

// this sucks
idea.project.settings.taskTriggers {
    afterSync(":azuvotifier-velocity:generateTemplates")
}