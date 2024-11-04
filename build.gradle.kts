plugins {
    kotlin("jvm") version "2.0.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Lincheck dependency
    testImplementation(kotlin("test"))
    // This dependency allows you to work with kotlin.test and JUnit:
    testImplementation("org.jetbrains.kotlinx:lincheck:2.34")
    implementation("org.jetbrains.kotlinx:atomicfu:0.26.0")
}

tasks.test {
    useJUnitPlatform()
}
