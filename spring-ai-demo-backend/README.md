Spring-ai-demo-backend
=======================

## Requirements

* ollama
  * `ollama pull mxbai-embed-large`

## Tech Stack

### Migrations

Migrations are manged with flywheel and can be run with: `./gradlew flywayMigrate`

### JOOQ

We use JOOQ as an ORM. Source is generated from a schema.

We output generated src to `src/generated/jooq`. It would be more typical to generate this source
into the build directory as a dependency of the compile task and not check it in. This caused problems
those with the current simple setup. Migrations are part of the same module. To run migrations the code needs to compile
but to compile we need a schema to generate jooq.

To simplify this I'm checking in the generated jooq code. If you make changes to the schema manually run:
`./gradlew jooqGenerate` and checkin the changes with your migration.
