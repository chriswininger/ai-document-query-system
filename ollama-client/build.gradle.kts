plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

group = "com.chriswininger"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("org.jboss.logging:jboss-logging:3.6.1.Final")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
}
