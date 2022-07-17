import org.gradle.api.*

plugins {
    java
    application
    id("io.freefair.lombok") version "6.5.0.2"
}

group = "dev.wido"
version = "0.0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        name = "central-snapshot"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation("dev.dominion.ecs:dominion-ecs-engine:0.6.0-SNAPSHOT")

    val gdxVersion = "1.11.0"
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    // Painful TODO: Separate for platforms (well, that's long way ahead)
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")

    implementation("org.greenrobot:eventbus-java:3.3.1")

    implementation("ch.qos.logback:logback-classic:1.3.0-alpha16")
    implementation("org.slf4j:slf4j-jdk-platform-logging:2.0.0-alpha7")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

java {
    // Gradle is as stellar as usual and Java 18 is still marked
    // as @incubating as of 7.5.0 despite being supported since,
    // well, this version. See issue #21269 @ Gradle repo.
    sourceCompatibility = JavaVersion.VERSION_18
    targetCompatibility = JavaVersion.VERSION_18
}

application {
    mainClass.set("dev.wido.RiaEngine.Main")
}