plugins {
	java
	id("org.springframework.boot") version "3.4.2"
	id("io.spring.dependency-management") version "1.1.7"
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

extra["springAiVersion"] = "1.0.0-M5"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	// implementation("org.springframework.ai:spring-ai-ollama-spring-boot-starter")

  	// === for loading FAISS vector database saved from python ===
  	// Core DJL API (required for Predictor/Criteria)
    implementation("ai.djl:api:0.32.0")
    // BOM for version management (must be platform)
    implementation(platform("ai.djl:bom:0.32.0"))
    // HuggingFace tokenizers
    implementation("ai.djl.huggingface:tokenizers")
    // PyTorch engine (required for model loading)
    implementation("ai.djl.pytorch:pytorch-engine")
    // Native library (choose based on hardware)
    // runtimeOnly("ai.djl.pytorch:pytorch-native-cpu::linux-x86_64") // Linux CPU
    runtimeOnly("ai.djl.pytorch:pytorch-native-cu124::linux-x86_64") // NVIDIA CUDA 12.4

	// === might not need the above, maybe just this, but we seem to be stuck on spu ===
	implementation("com.criteo.jfaiss:jfaiss-cpu:1.7.0-0")

	// === or maybe none of that and just this ===
	// nvm this also all looks made up
	//implementation("org.springframework.ai:spring-ai-vertex-ai-spring-boot-starter")

	// For HuggingFace embeddings (hypothetical - check for actual implementation):
	//implementation("com.huggingface:transformers-java:4.12.0")
	//implementation("org.springframework.ai:spring-ai-vectorstore-spring-boot-starter")

	// === grr ===
	implementation("org.springframework.ai:spring-ai-openai")
	implementation("ai.djl.huggingface:tokenizers:0.32.0")
	implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")
  // implementation("org.springframework.ai:spring-ai-starter-model-transformers")
	// made up/not real, wish it was
	//	implementation("com.facebook.faiss:faiss-java:1.7.0")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
