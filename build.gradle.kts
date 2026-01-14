import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

plugins {
    id("java")
    alias(libs.plugins.idea)
}

subprojects {
    apply(plugin = "azuvotifier.java-conventions")
    apply(plugin = "azuvotifier.publishing-conventions")

    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
        maven {
            name = "Glaremasters"
            url = uri("https://repo.glaremasters.me/repository/public")
        }
        maven {
            name = "Sonatype"
            url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        }
        maven {
            name = "azurejelly"
            url = uri("https://repo.azuuure.dev/repository/maven-proxy/")
        }
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

    tasks {
        test {
            useJUnitPlatform()
        }
    }
}

// this sucks
idea.project.settings.taskTriggers {
    afterSync(":azuvotifier-velocity:generateTemplates")
}