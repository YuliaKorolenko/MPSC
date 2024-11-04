plugins {
    kotlin("jvm") version "2.0.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
//    testImplementation(kotlin("test"))
    // Lincheck dependency
    testImplementation(kotlin("test"))
    // This dependency allows you to work with kotlin.test and JUnit:
    testImplementation("org.jetbrains.kotlinx:lincheck:2.34")
//    testImplementation 'org.junit.jupiter:junit-jupiter:5.8.2' // замените на актуальную версию JUnit
//    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

tasks.test {
    useJUnitPlatform()
}
