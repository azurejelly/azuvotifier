rootProject.name = "nuvotifier"

listOf(
    "api", "common", "bukkit", "bungeecord",
    "velocity", "standalone", "sponge"
).forEach {
    include(":nuvotifier-$it")
    project(":nuvotifier-$it").projectDir = file(it)
}