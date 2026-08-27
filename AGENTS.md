# Repository Guidelines

## Project Structure & Module Organization
This repository is a Maven multi-module Java 21 project.

- `novartc-server/`: main Spring Boot backend, including controllers, services, mappers, runtime config, and MyBatis XML under `src/main/resources/mappers`.
- `novartc-flyway/`: Flyway migration module, containing schema and development seed SQL under `src/main/resources/db`, plus a standalone Spring Boot startup entry used only for database version management.
- `novartc-common/`: shared constants, utilities, exceptions, and reusable infrastructure code.
- `novartc-generator/`: local code generation utilities, with `CodeGenerator` as the main entry point.
- `docker/` and `k8s/`: local dependency setup and deployment manifests.
- `backup/`: reference SQL/assets; do not treat as active runtime code.

Primary application entry: `novartc-server/src/main/java/io/github/xiaotang/novartc/server/NovaRtcApplication.java`.

## Build, Test, and Development Commands
- `mvn clean compile`: compile all modules.
- `cd novartc-flyway && mvn spring-boot:run`: execute Flyway migrations only, then exit. Run from inside the module; `-pl ... -am` fails because parent modules have no main class.
- `mvn -pl novartc-server -am compile`: compile the backend module and required dependencies.
- `mvn test`: run unit tests across modules.
- `mvn -pl novartc-server -am spring-boot:run`: start the backend locally.
- `mvn -pl novartc-generator compile exec:java -Dexec.mainClass=io.github.xiaotang.novartc.generator.CodeGenerator`: run the code generator (see Database Schema Changes).

Use Java 21. `JAVA_HOME` must point to a JDK, not a JRE.

## Database Schema Changes
All MySQL schema changes must go through Flyway migrations plus generator regeneration; hand-altering tables or hand-writing entity/mapper code for new tables is not allowed.

- Schema edits only as new versioned scripts under `novartc-flyway/src/main/resources/db/migration/` (`VX.Y.Z__name.sql`); never edit an already-applied migration.
- Apply migrations from inside the `novartc-flyway` module: `mvn spring-boot:run`.
- After creating or changing a table, narrow `addInclude` in `CodeGenerator` to the target table, run the generator command above, then keep the table in the `addInclude` list so it records every generated table.

## Coding Style & Naming Conventions
- Use 4-space indentation and UTF-8 source files.
- Follow standard Java naming: `UpperCamelCase` for classes, `lowerCamelCase` for methods/fields, `UPPER_SNAKE_CASE` for constants.
- Keep package names lowercase under `io.github.xiaotang.novartc`.
- Prefer concise Chinese JavaDoc for controller/service/mapper public APIs when the surrounding module already uses that style.
- Preserve existing Spring/MyBatis patterns: service interfaces in `service`, implementations in `service/impl`, XML mappers in `src/main/resources/mappers`.
- Backend time must be unified to UTC. When adding or modifying time-related logic, use UTC explicitly in Java APIs, JDBC/MySQL parameters, JSON serialization, scheduled tasks, and logs; do not depend on the machine default time zone or implicit local time.

## Testing Guidelines
Current automated test coverage is minimal. Existing tests live under `*/src/test/java`; at present, `novartc-common/src/test/java/MD5Common.java` is the main example.

- Add tests beside the module you change.
- Name test classes `*Test`.
- Prefer focused unit tests for utilities/services and integration tests only when persistence or messaging behavior changes.
- Run `mvn test` before opening a PR when possible.

## Commit & Pull Request Guidelines
Recent commits use short Chinese summaries such as `Codex-增加注释` and `Codex-增加Admin`. Keep commit messages short, imperative, and scoped to one change.
Suggested Git commit messages in assistant responses must be written in Chinese.

PRs should include:
- a brief summary of behavior changes,
- affected modules and key paths,
- config or schema impact,
- test/build status,
- screenshots only for UI or dashboard-related changes.

## Security & Configuration Tips
Never commit real secrets, local database dumps, or generated runtime logs. Review `application` config changes carefully, especially Redis, MongoDB, RocketMQ, MySQL, and Flyway settings.
