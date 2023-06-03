group = "it.unibo.dsdms.dossier"

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.johnrengelman.shadow)

    application

    id("java-library")
}
application.mainClass.set("dsdms.dossier.Main")

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.bundles.kotlinx)
    implementation(libs.bundles.kmongo)
    implementation(libs.bundles.vertx.server)
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    manifest.attributes["Main-Class"] = application.mainClass
    val projectVersion = project.properties["version"] as String
    archiveFileName.set("${project.name}-$projectVersion.jar")
    destinationDirectory.set(file("$buildDir/output"))
}
