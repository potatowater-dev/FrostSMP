version = "1.1.7"

repositories {
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks.jar {
    archiveBaseName.set("FrostChat")
}
