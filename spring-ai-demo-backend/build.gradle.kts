extra["springAiVersion"] = "1.0.0-M7"
extra["flywayVersion"] = "11.7.2"

plugins {
	java
	id("org.springframework.boot") version "3.4.4"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.flywaydb.flyway") version "11.7.2"
}

group = "com.wininger"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(23)
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")

	// support for talking to locally running ollama models
	implementation("org.springframework.ai:spring-ai-starter-model-ollama")

	// vector store for postgres
	implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")

	// migration framework
	implementation("org.flywaydb:flyway-core:${property("flywayVersion")}")
	implementation("org.postgresql:postgresql:42.7.5")
	implementation("org.flywaydb:flyway-database-postgresql:11.7.2")


	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

buildscript {
	dependencies {
		// migration framework
		classpath("org.flywaydb:flyway-database-postgresql:11.7.2")
		classpath("org.postgresql:postgresql:42.7.5")
	}
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// == migrations ===
tasks.named("flywayMigrate") {
	dependsOn(tasks.named("classes"))
}
flyway {
	url = System.getenv("SPRING_AI_DEMO_DB_URL") ?:
			"jdbc:postgresql://localhost:5436/spring-ai-demo-db"
	user = System.getenv("SPRING_AI_DEMO_DB_USER") ?:
			"postgres"
	password = System.getenv("SPRING_AI_DEMO_DB_PASSWORD") ?:
			"xxx"
	locations = arrayOf("filesystem:src/main/resources/db/migration")
}
