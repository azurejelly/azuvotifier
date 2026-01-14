import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.shadow)
}

dependencies {
    api(project(":azuvotifier-common"))

    implementation(libs.jackson.yaml)
    implementation(libs.apache.cli)
    implementation(libs.bundles.slf4j)
    implementation(libs.jedis)
}

tasks {
    jar {
        from("src/main/resources") {
            include("config.yml")
        }

        manifest {
            attributes["Main-Class"] = "com.vexsoftware.votifier.standalone.Main"
        }

        duplicatesStrategy = DuplicatesStrategy.WARN
    }

    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("azuvotifier-standalone")
        archiveClassifier.set("dist")
    }

    named("assemble").configure {
        dependsOn("shadowJar")
    }
}
