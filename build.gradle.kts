plugins {
    kotlin("jvm") version "1.9.10"
    application
}

group = "com.idk"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.smali:baksmali:2.5.2")
    implementation("org.smali:smali:2.5.2")
    implementation("org.smali:dexlib2:2.5.2")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("com.github.ajalt.clikt:clikt:4.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("com.idk.apk.MainKt")
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "com.idk.apk.MainKt")
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map {
            if (it.isDirectory) it else zipTree(it)
        }
    })
}
