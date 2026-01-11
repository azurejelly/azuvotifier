plugins {
    `java-library`
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public")
}

dependencies {
    api(project(":azuvotifier-common"))
    compileOnly(libs.paper)
}
