plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

allprojects {
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
var serializationVersion = "0.90.0"
var mockkVersion = "1.13.16"



dependencies {
    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.github.pdvrieze.xmlutil:core:${serializationVersion}")
    implementation("io.github.pdvrieze.xmlutil:serialization:${serializationVersion}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.3.0")
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(17)
}

tasks.test{
    useJUnitPlatform()
}
