# Repository Guidelines

## Project Structure & Module Organization
This repository is a Maven multi-module Java 21 project.

- `novartc-server/`: main Spring Boot backend, including controllers, services, mappers, runtime config, and MyBatis XML under `src/main/resources/mappers`.
- `novartc-flyway/`: Flyway migration module, containing schema and development seed SQL under `src/main/resources/db`, plus a standalone Spring Boot startup entry used only for database version management.
- `novartc-common/`: shared constants, utilities, exceptions, and reusable infrastructure code.
- `novartc-generator/`: local code generation utilities, with `CodeGenerator` as the main entry point.
- `novartc-job/`: 定时任务调度平台（Spring Boot + Thymeleaf + Quartz 内存调度），任务定义存 `job_info` 表，触发时经 RocketMQ `novartc-job-topic` 按服务 Tag 路由到目标服务，结果经 `novartc-job-result-topic` 回传更新 `job_log`；目标服务通过 `novartc-job-starter` 注册带 `@JobHandler` 的公开 Spring Bean 方法，禁止任意类/方法反射；内存调度，需单实例部署。
- `docker/` and `k8s/`: local dependency setup and deployment manifests.
- `backup/`: reference SQL/assets; do not treat as active runtime code.

Primary application entry: `novartc-server/src/main/java/io/github/xiaotang/novartc/server/NovaRtcApplication.java`.

## Build, Test, and Development Commands
- `mvn clean compile`: compile all modules.
- `cd novartc-flyway && mvn spring-boot:run`: execute Flyway migrations only, then exit. Run from inside the module; `-pl ... -am` fails because parent modules have no main class.
- `mvn -pl novartc-server -am compile`: compile the backend module and required dependencies.
- `mvn test -DskipTests=false`: run unit tests across modules; the parent POM skips tests by default.
- `mvn -pl novartc-server -am spring-boot:run`: start the backend locally.
- `mvn -pl novartc-job -am spring-boot:run`: start the scheduled-task platform on port 7010 (single instance only).
- `mvn -pl novartc-generator compile exec:java -Dexec.mainClass=io.github.xiaotang.novartc.generator.CodeGenerator`: run the code generator (see Database Schema Changes); output targets `novartc-server` by default, use `-Dgen.module=<module> -Dgen.parent=<parent package>` to generate into another module (e.g. `novartc-job`).

Use Java 21. `JAVA_HOME` must point to a JDK, not a JRE.

## Database Schema Changes
All MySQL schema changes must go through Flyway migrations; hand-altering tables is not allowed. Regenerate entity/mapper code for new tables and mapping-affecting column changes; do not hand-write entity/mapper code for new tables.

- Schema edits only as new versioned scripts under `novartc-flyway/src/main/resources/db/migration/` (`VX.Y.Z__name.sql`); never edit an already-applied migration.
- Apply migrations from inside the `novartc-flyway` module: `mvn spring-boot:run`.
- For a new table or mapping-affecting column change, set `addInclude` in `CodeGenerator` to only the target table, run the generator with explicit `-Dgen.module=<module> -Dgen.parent=<parent package>`, and keep generated-table history separate from the active `addInclude` selection.

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
- Run the affected tests with `-DskipTests=false` before opening a PR when possible, and report any verification that the local environment prevented.

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
