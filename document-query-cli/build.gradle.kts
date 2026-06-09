plugins {
    java
    id("io.quarkus")
    id("org.openapi.generator") version "7.21.0"
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-picocli")

    implementation("com.chriswininger:ollama-client")

    implementation("dev.langchain4j:langchain4j:1.15.1")
    implementation("dev.langchain4j:langchain4j-ollama:1.15.1")

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5-mockito")
}

group = "com.chriswininger"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("$rootDir/../openapi-specs/openapi.json")
    outputDir.set("${layout.buildDirectory.get()}/generated/openapi")
    apiPackage.set("com.chriswininger.client.api")
    modelPackage.set("com.chriswininger.client.model")
    invokerPackage.set("com.chriswininger.client")
    configOptions.set(mapOf(
        "library" to "native",
        "useJakartaEe" to "true",
        "dateLibrary" to "java8",
        "openApiNullable" to "false",
        "generateBuilders" to "true"
    ))
}

sourceSets {
    main {
        java {
            srcDir("${layout.buildDirectory.get()}/generated/openapi/src/main/java")
        }
    }
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}
