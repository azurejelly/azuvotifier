rootProject.name = "nuvotifier"

listOf(
    "api", "common", "bukkit", "bungeecord",
    "velocity", "standalone", "sponge", "folia"
).forEach {
    include(":nuvotifier-$it")
    project(":nuvotifier-$it").projectDir = file(it)
}