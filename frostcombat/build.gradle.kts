version = "1.4.3"

repositories {
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks.jar {
    archiveBaseName.set("FrostCombat")
}
