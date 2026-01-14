import org.gradle.internal.extensions.stdlib.toDefaultLowerCase

plugins {
    id("maven-publish")
    signing
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
        maven {
            val version = project.version.toString().toDefaultLowerCase()
            val repo = when {
                version.contains("snapshot") ||
                        version.contains("beta") ||
                        version.contains("alpha") ||
                        version.contains("dev") -> "snapshots"
                else -> "releases"
            }

            name = "azurejelly"
            url = uri("https://repo.azuuure.dev/repository/maven-$repo")
            credentials(PasswordCredentials::class)
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}