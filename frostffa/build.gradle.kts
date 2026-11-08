version = "1.0.17"

repositories {
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.13")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.NEZNAMY:TAB-API:6.1.2")
}

tasks.jar {
    archiveBaseName.set("FrostFFA")
}
