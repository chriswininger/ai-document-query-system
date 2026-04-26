# document-ingestion-api

This project allows you to ingest large documents and stores them in a queryable database 

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./gradlew build
```

It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./gradlew build -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./gradlew build -Dquarkus.native.enabled=true
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./gradlew build -Dquarkus.native.enabled=true -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/document-ingestion-api-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/gradle-tooling>.

## Database

### Starting the database

A `start-db.sh` script is provided to run a local PostgreSQL instance (via Docker):

```shell script
bash start-db.sh
```

This starts a `pgvector/pgvector:pg17` container on port `5436` with:

- User: `postgres`
- Password: `xxx`
- Database: `spring-ai-demo-db`

To verify the database is running and migrations have applied:

```shell script
PGPASSWORD=xxx psql -h localhost -p 5436 -U postgres -d spring-ai-demo-db -c "\dt"
```

### Migrations (Flyway)

Database schema is managed with [Flyway](https://flywaydb.org/) 12.4.0. Migration scripts live in:

```
src/main/resources/db/migration/
```

Scripts follow the naming convention `V{version}__{description}.sql`. Flyway runs automatically on application startup (`quarkus.flyway.migrate-at-start=true`). Migration is skipped in the `test` profile.

Current migrations:

| Version | Description |
|---------|-------------|
| V1 | Create `documents` table |
| V2 | Create `chapters` table (FK to `documents`) |
| V3 | Create `sections` table (FK to `chapters`) |


## jOOQ Code Generation

The data layer uses [jOOQ](https://www.jooq.org/) 3.21.2 with type-safe generated classes. Generation uses the `DDLDatabase` approach — it reads the Flyway migration SQL files directly via jOOQ's built-in SQL parser and an in-memory H2 instance, so **no live database connection is required** to regenerate types.

Generated sources are written to `src/main/generated/` and are compiled as part of the main source set. This directory should be committed to version control.

To regenerate after adding or modifying a migration:

```shell script
./gradlew jooqCodegen
```

Generated classes are located at `com.chriswininger.db.generated` and include table definitions, record types, keys, and indexes for all three tables.

### Repository layer

CDI repository beans in `com.chriswininger.repository` provide typed read/write access to each table:

- `DocumentRepository` — `findAll`, `findById`, `insert`, `update`, `deleteById`
- `ChapterRepository` — `findAll`, `findById`, `findByDocumentId`, `insert`, `update`, `deleteById`
- `SectionRepository` — `findAll`, `findById`, `findByChapterId`, `insert`, `update`, `deleteById`

A `DSLContextProducer` bean in `com.chriswininger.db` wires the Quarkus-managed Agroal datasource into jOOQ's `DSLContext` with `SQLDialect.POSTGRES`.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
