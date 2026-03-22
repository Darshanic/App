plugins {
    kotlin("jvm") version "1.9.0"
}

repositories {
    mavenCentral()
}

dependencies {
    // Basic Swing works out of the box with Kotlin JVM
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "DesktopAppKt"
    }
}
