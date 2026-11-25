<div align="center">

# NovaRTC

**基于 Spring Boot、Netty 和 Protocol Buffers 的模块化即时通讯后端。**

[English](README.md) · **简体中文**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Netty](https://img.shields.io/badge/Netty-4.2-4C9A2A)](https://netty.io/)
[![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](LICENSE)

</div>

## 项目简介

NovaRTC 是一个采用 Maven 多模块结构的实时即时通讯后端，集成了 TCP 与 WebSocket 长连接、Protocol Buffers 消息、REST API、联系人与聊天管理、文件存储、后台管理与风控接口，以及定时任务调度能力。

仓库包含主应用、独立的 Flyway 数据库迁移模块、基于 Quartz 的任务控制台、可复用的任务执行器自动配置、本地基础设施配置，以及 Kubernetes 部署清单。

> **注意：** 仓库中的配置面向本地开发。在共享环境或生产环境使用前，请更换示例凭据，并检查端口暴露及相关安全配置。

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [默认访问地址](#默认访问地址)
- [API 与消息协议](#api-与消息协议)
- [开发指南](#开发指南)
- [部署](#部署)
- [参与贡献](#参与贡献)
- [许可证](#许可证)

## 功能特性

- **实时消息：** 支持单聊与群聊、文本与媒体消息、心跳、在线状态、已读操作和消息撤回。
- **TCP 与 WebSocket：** 在同一个 Netty 端口上自动识别协议，WebSocket 连接路径为 `/ws`。
- **账号与社交：** 提供注册、登录、登出、联系人、好友申请、单聊创建和头像更新等功能。
- **文件处理：** 提供需要身份认证的文件上传接口，使用 MinIO 存储，并限制文件类型与大小。
- **后台管理与风控：** 支持用户与菜单管理、在线会话管理、聊天与审计日志查询、登录封禁、禁言、黑名单和登录告警。
- **定时任务：** 提供 Thymeleaf 管理控制台、Quartz 内存调度、基于 RocketMQ 的任务分发、Spring Bean 处理器注册和执行日志。
- **运维支持：** 集成 Flyway 数据库迁移、Actuator 健康探针、结构化日志、Docker Compose 依赖环境和 Kubernetes 示例清单。

## 技术栈

| 领域 | 技术 |
| --- | --- |
| 语言与运行时 | Java 21 |
| 应用框架 | Spring Boot 3.2.5 |
| 实时通信 | Netty、TCP、WebSocket、Protocol Buffers |
| 关系型数据 | MySQL 8、MyBatis-Plus、Druid |
| 文档数据 | MongoDB |
| 缓存与协调 | Redis、Redisson |
| 异步消息 | RocketMQ |
| 对象存储 | MinIO |
| 任务调度 | Quartz、Thymeleaf |
| 数据库迁移 | Flyway |
| 可观测性 | Spring Boot Actuator、Log4j2、Elasticsearch、Kibana、Filebeat |
| 构建与部署 | Maven、Docker Compose、Kubernetes |

## 项目结构

```text
NovaRTC/
├── novartc-server/        # Spring Boot 主应用、REST API 与 Netty IM 服务
├── novartc-common/        # 公共常量、工具、异常与基础设施代码
├── novartc-flyway/        # 独立数据库迁移启动模块与 SQL 脚本
├── novartc-job/           # Quartz 调度平台与 Thymeleaf 任务控制台
├── novartc-job-starter/   # 任务处理器注册与 RocketMQ 执行器自动配置
├── novartc-generator/     # MyBatis-Plus 代码生成工具
├── docker/                # 本地开发依赖与 Docker Compose 配置
├── k8s/                   # Kubernetes 清单与本地部署脚本
└── docs/                  # 其他设计与实现文档
```

主应用入口为 [`NovaRtcApplication.java`](novartc-server/src/main/java/io/github/xiaotang/novartc/server/NovaRtcApplication.java)。

## 快速开始

### 环境要求

- JDK 21（`JAVA_HOME` 必须指向 JDK，而不是 JRE）
- Maven 3.9 或更高版本
- 如果使用仓库提供的本地基础设施，需要安装 Docker 和 Docker Compose
- MySQL 8、Redis、MongoDB、RocketMQ 和 MinIO，可通过 Docker 或已有实例提供

### 1. 克隆仓库

```bash
git clone https://github.com/WispJing/NovaRTC.git
cd NovaRTC
```

### 2. 启动本地依赖

Compose 配置使用本地构建的 MySQL 镜像。请先在仓库根目录构建该镜像，再启动基础设施：

```bash
docker build -t im-mysql:8.0 docker/mysql
docker compose -f docker/docker-compose.yml up -d
```

上述命令会启动 MySQL、Redis、MongoDB、RocketMQ 及其控制台、MinIO、Elasticsearch、Kibana 和 Filebeat。

如果已有这些依赖，可以跳过此步骤，并在应用配置中填写实际地址和凭据。常用 Compose 命令请参阅 [docker/readme.md](docker/readme.md)。

### 3. 创建数据库

连接 MySQL，创建默认配置所需的数据库：

```sql
CREATE DATABASE IF NOT EXISTS novartc
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

### 4. 检查配置

默认启用的 Spring Profile 为 `dev`。启动应用前，请检查以下文件：

- [`novartc-server/src/main/resources/application-dev.yml`](novartc-server/src/main/resources/application-dev.yml)
- [`novartc-flyway/src/main/resources/application-dev.yml`](novartc-flyway/src/main/resources/application-dev.yml)
- [`novartc-job/src/main/resources/application-dev.yml`](novartc-job/src/main/resources/application-dev.yml)

默认配置分别连接 `localhost:3307` 上的 MySQL、`localhost:6379` 上的 Redis、`localhost:27017` 上的 MongoDB、`localhost:9876` 上的 RocketMQ，以及 `localhost:9000` 上的 MinIO。

其他环境请使用 Spring Boot 外部化配置，不要提交真实凭据或密钥。

### 5. 执行数据库迁移

进入独立的迁移模块目录后执行：

```bash
cd novartc-flyway
mvn spring-boot:run
cd ..
```

该进程会应用版本化迁移和开发环境种子数据，完成后自动退出。数据库结构变更必须新增到 `novartc-flyway/src/main/resources/db/migration/` 目录，不要修改已经执行过的迁移脚本。

### 6. 启动应用

在仓库根目录启动主后端：

```bash
mvn -pl novartc-server -am spring-boot:run
```

如有需要，在另一个终端启动任务调度平台：

```bash
mvn -pl novartc-job -am spring-boot:run
```

> **重要：** `novartc-job` 使用 Quartz 内存调度，只能以单实例方式部署。

主应用启动后，可以通过以下命令检查就绪状态：

```bash
curl http://localhost:7000/actuator/health/readiness
```

## 默认访问地址

| 服务 | 地址 |
| --- | --- |
| REST API | `http://localhost:7000/api` |
| TCP IM 连接 | `localhost:9688` |
| WebSocket IM 连接 | `ws://localhost:9688/ws` |
| 任务控制台 | `http://localhost:7010/` |
| Actuator 健康检查 | `http://localhost:7000/actuator/health` |
| 就绪探针 | `http://localhost:7000/actuator/health/readiness` |
| 存活探针 | `http://localhost:7000/actuator/health/liveness` |
| RocketMQ 控制台 | `http://localhost:8082/` |
| MinIO 控制台 | `http://localhost:6001/` |
| Kibana | `http://localhost:5601/` |

基础设施控制台仅在对应的 Docker 服务运行时可用。

## API 与消息协议

### REST API

| 功能 | 基础路径 | 示例 |
| --- | --- | --- |
| 身份认证 | `/api` | `/register`、`/login`、`/logout` |
| 联系人 | `/api/contact` | 联系人列表、单聊、好友申请 |
| 聊天 | `/api/chat` | 会话列表、历史消息、已读操作、消息撤回 |
| 用户 | `/api/user` | 头像更新 |
| 文件 | `/api/file` | 需要认证的文件上传 |
| 后台管理 | `/admin` | 认证、用户、在线会话、聊天、日志与风控 |

大多数 `/api/**` 接口需要 Token 认证。具体请求与响应类型请参阅 [`controller`](novartc-server/src/main/java/io/github/xiaotang/novartc/server/controller) 和 [`admin/controller`](novartc-server/src/main/java/io/github/xiaotang/novartc/server/admin/controller) 下的类。

### IM 协议

- TCP 和 WebSocket 客户端统一使用 [`ChatMessage.proto`](novartc-server/src/main/proto/ChatMessage.proto) 中定义的消息协议。
- WebSocket 客户端连接 `ws://localhost:9688/ws`。
- Netty 监听器会在配置的 IM 端口上识别 TCP 和 HTTP/WebSocket 流量。
- Maven 编译时会自动生成 Protobuf Java 源码，无需单独安装本地 `protoc`。

详情请参阅 [Protobuf 代码生成指南](novartc-server/src/main/proto/protobuf.md)。

## 开发指南

### 常用 Maven 命令

```bash
# 编译所有模块
mvn clean compile

# 编译后端及其依赖模块
mvn -pl novartc-server -am compile

# 运行所有模块的测试
mvn test -DskipTests=false
```

父 POM 默认跳过测试，因此显式运行测试时需要添加 `-DskipTests=false`。

### 代码生成

运行 MyBatis-Plus 代码生成器：

```bash
mvn -pl novartc-generator compile exec:java -Dexec.mainClass=io.github.xiaotang.novartc.generator.CodeGenerator
```

生成结果默认输出到 `novartc-server`。可通过 `-Dgen.module=<模块>` 和 `-Dgen.parent=<父包名>` 指定其他目标。为特定表生成代码前，请先配置生成器中的 `addInclude` 选项。

### 开发约定

- 使用 Java 21、UTF-8 和四空格缩进。
- Service 接口放在 `service` 包，实现类放在 `service/impl` 包，MyBatis XML 文件放在 `src/main/resources/mappers`。
- 数据库结构变更必须通过新的 Flyway 迁移完成；新增表或修改影响映射的字段后，需要重新生成映射代码。
- 后端时间处理、持久化、序列化、定时任务和日志必须显式使用 UTC。
- 测试类使用 `*Test` 命名，并放在对应模块的测试目录中。

## 部署

- [本地 Docker 依赖说明](docker/readme.md)
- [Kubernetes 部署说明](k8s/novartc-server/README.md)

当前 Kubernetes 清单面向 Docker Desktop 本地 Kubernetes 集群，并复用宿主机上的基础设施。改造为生产部署前，请将密钥外部化、替换示例凭据、配置 TLS 与访问控制，并规划 MySQL、Redis、MongoDB、RocketMQ 和 MinIO 的高可用方案。

## 参与贡献

欢迎提交贡献。建议按以下流程进行：

1. Fork 仓库，并创建职责单一的功能分支。
2. 遵循现有模块结构和编码规范。
3. 根据需要补充或更新测试与文档。
4. 运行相关检查，并在 Pull Request 中说明配置或数据库结构影响。
5. 提交 Pull Request，清楚描述行为变更和验证情况。

如需报告问题或提出功能建议，请使用 [GitHub Issues](https://github.com/WispJing/NovaRTC/issues)。

## 许可证

本项目采用 [GNU Affero 通用公共许可证 v3.0](LICENSE)（AGPL-3.0）授权。对于通过网络向用户提供服务的修改版本，该许可证也规定了相应的源代码提供义务。完整条款请参阅许可证文件。
