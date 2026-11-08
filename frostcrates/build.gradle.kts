version = "1.3.10"

repositories {
    maven("https://repo.lucko.me/repository/maven-public/")
}

dependencies {
    compileOnly("net.luckperms:api:5.4")
}

tasks.jar {
    archiveBaseName.set("FrostCrates")
}
