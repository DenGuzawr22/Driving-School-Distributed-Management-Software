group = "dsdms.client"
version = "0.0.1"


@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.johnrengelman.shadow)

    application
}

application.mainClass.set("dsdms.client.Main")

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("test"))
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.bundles.kotlinx)
    implementation(libs.bundles.vertx.client)
    implementation(libs.bundles.cucumber)

    //Allows to add external module classes as dependencies
    implementation(project(":DossierService"))
}

tasks.test {
    useJUnitPlatform()
}


tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    manifest.attributes["Main-Class"] = application.mainClass
    archiveFileName.set("${project.name}-${project.version}.jar")
    destinationDirectory.set(file("$buildDir/output"))
}