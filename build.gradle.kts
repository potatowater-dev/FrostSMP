subprojects {
    apply(plugin = "java")

    group = "com.frostsmp"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    dependencies {
        add("compileOnly", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    }

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.named<ProcessResources>("processResources").configure {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}

subprojects {
    if (name == "frostlicense" || name == "frostffa" || name == "froststore") {
        return@subprojects
    }

    dependencies {
        add("implementation", project(":frostlicense"))
    }

    tasks.named<Jar>("jar") {
        from(project(":frostlicense").extensions.getByType(JavaPluginExtension::class.java).sourceSets.getByName("main").output)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
