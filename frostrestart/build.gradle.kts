version = "1.2.0"

dependencies {
    compileOnly(project(":frostcombat"))
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(":frostcombat:compileJava")
}

tasks.jar {
    archiveBaseName.set("FrostRestart")
}
