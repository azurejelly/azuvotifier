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
                version.contains("snapshot") -> "snapshots"
                else -> "releases"
            }

            name = "azurejelly"
            url = uri("https://repo.azuuure.dev/repository/maven-$repo")
            credentials(PasswordCredentials::class)
        }
    }
}

signing {
    if (System.getenv("GITHUB_ACTIONS") == "true") {
        // prefer in memory pgp keys when publications are being
        // made from github actions
        useInMemoryPgpKeys(
            System.getenv("GPG_PRIVATE_KEY"),
            System.getenv("GPG_PASSPHRASE")
        )
    } else {
        // use the gpg command on local development environments
        useGpgCmd()
    }

    sign(publishing.publications["mavenJava"])
}