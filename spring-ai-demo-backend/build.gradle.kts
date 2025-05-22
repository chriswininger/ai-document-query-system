extra["springAiVersion"] = "1.0.0-M7"
extra["flywayVersion"] = "11.7.2"

plugins {
	java
	id("org.springframework.boot") version "3.4.4"
	id("io.spring.dependency-management") version "1.1.7"

	// migration framework
	id("org.flywaydb.flyway") version "11.7.2"

	// jooq
	id("nu.studer.jooq") version "8.2.1"
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

	// db driver for migration and jooq
	implementation("org.postgresql:postgresql:42.7.4")
	jooqGenerator("org.postgresql:postgresql:42.7.4")

	// migration framework
	implementation("org.flywaydb:flyway-core:${property("flywayVersion")}")
	implementation("org.flywaydb:flyway-database-postgresql:11.7.2")

	// jooq
	implementation("org.jooq:jooq:3.19.3")
	implementation("org.jooq:jooq-meta:3.19.3")
	implementation("org.jooq:jooq-codegen:3.19.3")

	// utils
	implementation("org.javatuples:javatuples:1.2")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

buildscript {
	dependencies {
		// migration framework
		classpath("org.flywaydb:flyway-database-postgresql:11.7.2")
		classpath("org.postgresql:postgresql:42.7.4")
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

val dbUrl = System.getenv("SPRING_AI_DEMO_DB_URL") ?: "jdbc:postgresql://localhost:5436/spring-ai-demo-db"
val dbUser = System.getenv("SPRING_AI_DEMO_DB_USER") ?: "postgres"
val dbPassword = System.getenv("SPRING_AI_DEMO_DB_PASSWORD") ?: "xxx"

// == migrations ===
tasks.named("flywayMigrate") {
	dependsOn(tasks.named("classes"))
}
flyway {
	url = dbUrl
	user = dbUser
	password = dbPassword
	locations = arrayOf("filesystem:src/main/resources/db/migration")
}

// == jooq ==
//tasks.named("jooq") {
//	dependsOn(tasks.named("classes"))
//}
jooq {
	version.set("3.19.3")
	configurations {
		create("main") {  // name of the jOOQ configuration
			generateSchemaSourceOnCompilation.set(true)

			jooqConfiguration.apply {
				jdbc.apply {
					driver = "org.postgresql.Driver"
					url = dbUrl
					user = dbUser
					password = dbPassword
				}
				generator.apply {
					name = "org.jooq.codegen.JavaGenerator"
					database.apply {
						name = "org.jooq.meta.postgres.PostgresDatabase"
						inputSchema = "public"
						includes = ".*"
					}
					target.apply {
						packageName = "com.wininger.spring_ai_demo.jooq.generated"
						directory = "build/generated-src/jooq/main"
					}
				}
			}
		}
	}
}

tasks.named<JavaCompile>("compileJava") {
	dependsOn("generateJooq")
}

sourceSets {
	main {
		java {
			srcDir("build/generated-src/jooq/main")
		}
	}
}

