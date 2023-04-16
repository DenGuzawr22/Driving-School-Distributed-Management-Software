import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val cucumberVersion = "7.11.2"


plugins {
    val kotlinVersion: String by System.getProperties()
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("io.vertx.vertx-plugin") version System.getProperty("vertxVersion")

    //service plugins
    id("se.thinkcode.cucumber-runner") version "0.0.11"
}
vertx.mainVerticle="dsdms.client.Server"

repositories {
    mavenCentral()
}

allprojects{
    group = "dsdms"
    version = "1.0-SNAPSHOT"

}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

dependencies {
    val vertxImplVersion: String by System.getProperties()
    testImplementation(kotlin("test"))
    implementation("io.vertx:vertx-web:$vertxImplVersion")
    implementation("io.vertx:vertx-web-client:$vertxImplVersion")
    testImplementation("io.cucumber:cucumber-java:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-junit:$cucumberVersion")
    testImplementation("io.cucumber:cucumber-java8:$cucumberVersion")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.9.2")
    implementation(kotlin("stdlib-jdk8"))

}

tasks.test {
    useJUnitPlatform()
}