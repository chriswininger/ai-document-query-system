# document-query-cli

A Quarkus CLI application (picocli) for querying the document-ingestion-api.

## Prerequisites

- [SDKman](https://sdkman.io/) — all `java`, `quarkus`, and `./gradlew` commands require it:

  ```bash
  source ~/.sdkman/bin/sdkman-init.sh
  ```

- Java 25 (Amazon Corretto)
- The `document-ingestion-api` project (sibling directory) for the OpenAPI spec

## Generated API Client

This project auto-generates a typed Java HTTP client from the OpenAPI spec
produced by `document-ingestion-api`. The generated code lives under
`build/generated/openapi/` and is **not** checked into source control.

### How it works

1. `document-ingestion-api` uses SmallRye OpenAPI to write its spec to
   `../openapi-specs/openapi.json` at build time.
2. This project's Gradle build uses the `org.openapi.generator` plugin to
   generate a Java client from that spec before compilation.
3. The generated classes are compiled as part of the normal `compileJava` task.

### Updating the client after API changes

```bash
# 1. Rebuild the API to regenerate the spec
cd ../document-ingestion-api
source ~/.sdkman/bin/sdkman-init.sh && ./gradlew build -x test

# 2. Rebuild this project to regenerate the client
cd ../document-query-cli
source ~/.sdkman/bin/sdkman-init.sh && ./gradlew build -x test
```

### Generated packages

| Package | Contents |
|---------|----------|
| `com.chriswininger.client` | `ApiClient`, configuration, JSON helpers |
| `com.chriswininger.client.api` | One API class per resource (e.g. `DocumentResourceApi`) |
| `com.chriswininger.client.model` | DTOs matching the API's request/response schemas |

### Key API classes

- `DocumentResourceApi` — `listDocuments()`, `getDocumentById()`, `submitDocument()`
- `ChapterResourceApi` — `listChaptersByDocument()`, `getChapterById()`
- `SectionResourceApi` — `listSectionsByChapter()`, `listSectionsByDocument()`, `getSectionById()`
- `BookMetadataResourceApi` — `getBookMetadataByDocument()`, `getBookMetadataById()`
- `SemanticSearchResourceApi` — `searchSemantic()`

## Shared Libraries

### ollama-client

`OllamaApiService` and `@InferenceDescription` live in the sibling
`../ollama-client` project — a plain Java library shared via Gradle composite
build (`includeBuild` in `settings.gradle.kts`).

The CLI wires it via `OllamaApiServiceProducer` in `cli/services/`, which reads
`ollama.*` config from `application.properties` and constructs the service.
No Jandex indexing is needed because the producer lives in this project.

To change Ollama logic, edit `ollama-client/` and rebuild this project.

## Running in dev mode

```bash
source ~/.sdkman/bin/sdkman-init.sh && ./gradlew quarkusDev --quarkus-args='greet -n World'
```

Or use the provided script:

```bash
./run.sh
```

## Building

```bash
source ~/.sdkman/bin/sdkman-init.sh && ./gradlew build
```
