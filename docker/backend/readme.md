## 本地挂载版后端运行方案

这个方案的目标是：

- `jar` 不打进镜像，直接挂载宿主机文件
- 配置文件挂载到宿主机目录，便于改完即重启生效
- 日志目录挂载到宿主机目录
- MySQL、Redis、MongoDB、RocketMQ Dashboard 的数据或日志都落在 `docker` 目录下

### 目录说明

- 后端 jar：`../novartc-server/target/novartc-server-1.0.0-SNAPSHOT.jar`
- 后端配置：`./backend/config/application-docker.yml`
- 后端日志：`./backend/logs`
- MySQL 数据：`./mysql/data`
- MySQL 日志：`./mysql/logs`
- Redis 数据：`./redis/data`
- MongoDB 数据：`./mongodb/data`
- MongoDB 配置数据：`./mongodb/config`
- RocketMQ Broker 日志：`./rocketmq/logs`
- RocketMQ Broker 存储：`./rocketmq/store`
- RocketMQ Dashboard 日志：`./dashboard/logs`
- RocketMQ Dashboard 数据：`./dashboard/data`

### 启动前

先打包 jar：

```bash
mvn -pl novartc-server -am -DskipTests package
```

确认以下文件存在：

```bash
../novartc-server/target/novartc-server-1.0.0-SNAPSHOT.jar
./backend/config/application-docker.yml
```

### 启动

首次启用固定网段，或你修改了网络配置后，请先执行：

```bash
docker compose -f docker-compose.local.yml down
```

在 `D:\NovaRTC\NovaRTC\docker` 目录执行：

```bash
docker compose -f docker-compose.local.yml up -d
```

### 查看状态

```bash
docker compose -f docker-compose.local.yml ps
docker compose -f docker-compose.local.yml logs -f novartc-server
```

### 停止

```bash
docker compose -f docker-compose.local.yml down
```

### 说明

- 宿主机访问后端 HTTP：`http://localhost:7000`
- 宿主机访问 IM 端口：`localhost:9688`
- RocketMQ Dashboard：`http://localhost:8082`
- 容器内后端使用 `docker` profile，并通过服务名访问中间件，不再使用 `localhost`
- Compose 已固定 Docker 网段为 `172.19.0.0/24`，网关为 `172.19.0.1`，便于和 Druid 白名单联动
- 如果你重新打包了 jar，重启后端容器即可：

```bash
docker compose -f docker-compose.local.yml restart novartc-server
```
