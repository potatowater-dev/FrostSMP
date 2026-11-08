version = "1.0.0"

dependencies {
    compileOnly("com.google.code.gson:gson:2.11.0")
}

tasks.jar {
    archiveBaseName.set("FrostStore")
}
