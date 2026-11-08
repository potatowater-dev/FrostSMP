plugins {
    `java-library`
}

import java.security.MessageDigest

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

val licensePassword = (findProperty("frostLicensePassword") as String?) ?: "FrostSMP-ChangeMe"

val generateLicenseConstants = tasks.register("generateLicenseConstants") {
    val outputDir = layout.buildDirectory.dir("generated/sources/license")
    outputs.dir(outputDir)
    inputs.property("licensePassword", licensePassword)
    doLast {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(licensePassword.toByteArray(Charsets.UTF_8))
        val hash = digest.joinToString("") { byte: Byte -> "%02x".format(byte) }
        val file = outputDir.get().asFile.resolve("com/frostsmp/license/LicenseConstants.java")
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.frostsmp.license;

            final class LicenseConstants {
                static final String EXPECTED_HASH = "$hash";
                private LicenseConstants() {
                }
            }
            """.trimIndent()
        )
    }
}

sourceSets.named("main") {
    java.srcDir(generateLicenseConstants.map { it.outputs.files })
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(generateLicenseConstants)
}
