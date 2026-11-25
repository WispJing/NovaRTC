<div align="center">

# NovaRTC

**A modular real-time instant messaging backend built with Spring Boot, Netty, and Protocol Buffers.**

**English** · [简体中文](README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Netty](https://img.shields.io/badge/Netty-4.2-4C9A2A)](https://netty.io/)
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)

</div>

## Overview

NovaRTC is a Maven multi-module backend for real-time instant messaging. It combines TCP and WebSocket connectivity, Protocol Buffers messages, REST APIs, contact and chat management, file storage, administration and risk-control APIs, and scheduled-job dispatch.

The repository includes the main application, a dedicated Flyway migration runner, a Quartz-based job console, reusable job-executor auto-configuration, local infrastructure definitions, and Kubernetes manifests.

> **Note:** The included configuration is intended for local development. Replace sample credentials and review exposed ports and security settings before using the project in a shared or production environment.

## Table of Contents

- [Features](#features)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Default Endpoints](#default-endpoints)
- [API and Messaging Protocol](#api-and-messaging-protocol)
- [Development](#development)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Real-time messaging:** one-to-one and group chats, text and media messages, heartbeats, presence updates, read operations, and message recall.
- **TCP and WebSocket support:** automatic protocol detection on one Netty port, with WebSocket connections available at `/ws`.
- **Account and social features:** registration, login, logout, contacts, friend requests, direct-chat creation, and avatar updates.
- **File handling:** authenticated uploads backed by MinIO, with file-type and size restrictions.
- **Administration and risk control:** user and menu management, online-session administration, chat and audit-log queries, login bans, mutes, blacklists, and login alerts.
- **Scheduled jobs:** a Thymeleaf management console, in-memory Quartz scheduling, RocketMQ-based dispatch, registered Spring Bean handlers, and execution logs.
- **Operational support:** Flyway migrations, Actuator health probes, structured logging, Docker Compose dependencies, and sample Kubernetes deployment manifests.

## Technology Stack

| Area | Technologies |
| --- | --- |
| Language and runtime | Java 21 |
| Application framework | Spring Boot 3.2.5 |
| Real-time transport | Netty, TCP, WebSocket, Protocol Buffers |
| Relational data | MySQL 8, MyBatis-Plus, Druid |
| Document data | MongoDB |
| Cache and coordination | Redis, Redisson |
| Asynchronous messaging | RocketMQ |
| Object storage | MinIO |
| Scheduling | Quartz, Thymeleaf |
| Database migrations | Flyway |
| Observability | Spring Boot Actuator, Log4j2, Elasticsearch, Kibana, Filebeat |
| Build and deployment | Maven, Docker Compose, Kubernetes |

## Project Structure

```text
NovaRTC/
├── novartc-server/        # Main Spring Boot application, REST APIs, and Netty IM server
├── novartc-common/        # Shared constants, utilities, exceptions, and infrastructure
├── novartc-flyway/        # Standalone database migration runner and SQL migrations
├── novartc-job/           # Quartz scheduling platform and Thymeleaf job console
├── novartc-job-starter/   # Job-handler registration and RocketMQ executor auto-configuration
├── novartc-generator/     # MyBatis-Plus code generation utilities
├── docker/                # Local development dependencies and Docker Compose file
├── k8s/                   # Kubernetes manifests and local deployment scripts
└── docs/                  # Additional design and implementation documents
```

The main application entry point is [`NovaRtcApplication.java`](novartc-server/src/main/java/io/github/xiaotang/novartc/server/NovaRtcApplication.java).

## Getting Started

### Prerequisites

- JDK 21 (`JAVA_HOME` must point to a JDK, not a JRE)
- Maven 3.9 or later
- Docker with Docker Compose, if you use the provided local infrastructure
- MySQL 8, Redis, MongoDB, RocketMQ, and MinIO, either through Docker or your own installations

### 1. Clone the repository

```bash
git clone https://github.com/WispJing/NovaRTC.git
cd NovaRTC
```

### 2. Start local dependencies

The Compose file uses a locally built MySQL image. Build it first, then start the infrastructure from the repository root:

```bash
docker build -t im-mysql:8.0 docker/mysql
docker compose -f docker/docker-compose.yml up -d
```

This starts MySQL, Redis, MongoDB, RocketMQ and its dashboard, MinIO, Elasticsearch, Kibana, and Filebeat.

If you already run these dependencies, skip this step and update the application configuration with your own addresses and credentials. See [docker/readme.md](docker/readme.md) for common Compose commands.

### 3. Create the database

Connect to MySQL and create the database expected by the default configuration:

```sql
CREATE DATABASE IF NOT EXISTS novartc
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

### 4. Review the configuration

The default Spring profile is `dev`. Review these files before starting the applications:

- [`novartc-server/src/main/resources/application-dev.yml`](novartc-server/src/main/resources/application-dev.yml)
- [`novartc-flyway/src/main/resources/application-dev.yml`](novartc-flyway/src/main/resources/application-dev.yml)
- [`novartc-job/src/main/resources/application-dev.yml`](novartc-job/src/main/resources/application-dev.yml)

The defaults expect MySQL on `localhost:3307`, Redis on `localhost:6379`, MongoDB on `localhost:27017`, RocketMQ on `localhost:9876`, and MinIO on `localhost:9000`.

Use external Spring Boot configuration for other environments, and do not commit real credentials or secrets.

### 5. Run database migrations

Run the dedicated migration module from its own directory:

```bash
cd novartc-flyway
mvn spring-boot:run
cd ..
```

The process applies the versioned migrations and development seed data, then exits. Schema changes must be added as new scripts under `novartc-flyway/src/main/resources/db/migration/`; do not modify migrations that have already been applied.

### 6. Start the applications

Start the main backend from the repository root:

```bash
mvn -pl novartc-server -am spring-boot:run
```

Optionally, start the job platform in a separate terminal:

```bash
mvn -pl novartc-job -am spring-boot:run
```

> **Important:** `novartc-job` uses in-memory Quartz scheduling and must run as a single instance.

Verify the main application after it starts:

```bash
curl http://localhost:7000/actuator/health/readiness
```

## Default Endpoints

| Service | Address |
| --- | --- |
| REST API | `http://localhost:7000/api` |
| IM over TCP | `localhost:9688` |
| IM over WebSocket | `ws://localhost:9688/ws` |
| Job console | `http://localhost:7010/` |
| Actuator health | `http://localhost:7000/actuator/health` |
| Readiness probe | `http://localhost:7000/actuator/health/readiness` |
| Liveness probe | `http://localhost:7000/actuator/health/liveness` |
| RocketMQ dashboard | `http://localhost:8082/` |
| MinIO console | `http://localhost:6001/` |
| Kibana | `http://localhost:5601/` |

Infrastructure dashboards are available only when their corresponding Docker services are running.

## API and Messaging Protocol

### REST APIs

| Area | Base path | Examples |
| --- | --- | --- |
| Authentication | `/api` | `/register`, `/login`, `/logout` |
| Contacts | `/api/contact` | Contact lists, direct chats, friend requests |
| Chats | `/api/chat` | Chat lists, history, read operations, recall |
| Users | `/api/user` | Avatar updates |
| Files | `/api/file` | Authenticated file uploads |
| Administration | `/admin` | Authentication, users, online sessions, chats, logs, and risk control |

Most `/api/**` endpoints require token authentication. Refer to the classes under [`controller`](novartc-server/src/main/java/io/github/xiaotang/novartc/server/controller) and [`admin/controller`](novartc-server/src/main/java/io/github/xiaotang/novartc/server/admin/controller) for request and response types.

### IM protocol

- TCP and WebSocket clients use the message contract in [`ChatMessage.proto`](novartc-server/src/main/proto/ChatMessage.proto).
- WebSocket clients connect to `ws://localhost:9688/ws`.
- The Netty listener detects TCP and HTTP/WebSocket traffic on the configured IM port.
- Protobuf Java sources are generated automatically during Maven compilation; a separate local `protoc` installation is not required.

See the [Protobuf generation guide](novartc-server/src/main/proto/protobuf.md) for details.

## Development

### Common Maven commands

```bash
# Compile all modules
mvn clean compile

# Compile the backend and its required modules
mvn -pl novartc-server -am compile

# Run tests across all modules
mvn test -DskipTests=false
```

Tests are skipped by default in the parent POM, so use `-DskipTests=false` when running them explicitly.

### Code generation

Run the MyBatis-Plus generator with:

```bash
mvn -pl novartc-generator compile exec:java -Dexec.mainClass=io.github.xiaotang.novartc.generator.CodeGenerator
```

Generated output targets `novartc-server` by default. Use `-Dgen.module=<module>` and `-Dgen.parent=<parent package>` to select another target. Configure the generator's `addInclude` selection before generating code for a specific table.

### Development conventions

- Use Java 21, UTF-8, and four-space indentation.
- Keep service interfaces in `service`, implementations in `service/impl`, and MyBatis XML files under `src/main/resources/mappers`.
- Add database changes through new Flyway migrations; regenerate mapping code for new tables and mapping-affecting column changes.
- Use UTC explicitly for backend time handling, persistence, serialization, scheduled jobs, and logs.
- Name test classes `*Test` and place them beside the module they cover.

## Deployment

- [Local Docker dependency guide](docker/readme.md)
- [Kubernetes deployment guide](k8s/novartc-server/README.md)

The Kubernetes manifests currently target a local Docker Desktop Kubernetes cluster and reuse infrastructure running on the host. Before adapting them for production, externalize secrets, replace sample credentials, configure TLS and access control, and plan high availability for MySQL, Redis, MongoDB, RocketMQ, and MinIO.

## Contributing

Contributions are welcome. To propose a change:

1. Fork the repository and create a focused feature branch.
2. Follow the existing module structure and coding conventions.
3. Add or update tests and documentation where appropriate.
4. Run the relevant checks and include any configuration or schema impact in your pull request.
5. Open a pull request with a clear summary of the behavior changes and verification status.

For bugs or feature requests, use [GitHub Issues](https://github.com/WispJing/NovaRTC/issues).

## License

NovaRTC is licensed under the [GNU Affero General Public License v3.0](LICENSE) (AGPL-3.0). This includes source-availability obligations for modified versions offered to users over a network. See the license file for the complete terms.
