## 本地挂载版后端运行方案

这个方案的目标是：

- `jar` 不打进镜像，直接挂载宿主机文件
- 配置文件挂载到宿主机目录，便于改完即重启生效
- 日志目录挂载到宿主机目录
- MySQL、Redis、MongoDB、RocketMQ、ELK 相关数据或日志都落在 `docker` 目录下

### 目录说明

- 后端 jar：`../novartc-server/target/novartc-server-1.0.0-SNAPSHOT.jar`
- 后端配置：`./backend/config/application-docker.yml`
- 后端日志：容器标准输出/标准错误，由 Docker 容器日志统一承载
- workerId/datacenterId：容器启动脚本自动分配，仍可用环境变量 IM_WORKERID、IM_DATACENTERID 手工覆盖
- MySQL 数据：`./mysql/data`
- MySQL 日志：`./mysql/logs`
- Redis 数据：`./redis/data`
- MongoDB 数据：`./mongodb/data`
- MongoDB 配置数据：`./mongodb/config`
- RocketMQ Broker 日志：`./rocketmq/logs`
- RocketMQ Broker 存储：`./rocketmq/store`
- RocketMQ Dashboard 日志：`./dashboard/logs`
- RocketMQ Dashboard 数据：`./dashboard/data`
- Elasticsearch 数据：`./elasticsearch/data`
- Filebeat 配置：`./filebeat/filebeat.yml`（文件日志版）和 `./filebeat/filebeat-docker.yml`（Docker 容器日志版）

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
docker compose -f docker-compose.local.yml up -d --scale novartc-server=3
```

### 查看状态

```bash
docker compose -f docker-compose.local.yml ps
docker compose -f docker-compose.local.yml logs -f novartc-server
docker compose -f docker-compose.local.yml logs -f filebeat
```

### 停止

```bash
docker compose -f docker-compose.local.yml down
```

### 说明

- 宿主机访问后端 HTTP：`http://localhost:7000`，由 HAProxy 负载到多个 `novartc-server` 实例
- 宿主机访问 IM 端口：`localhost:9688`，由 HAProxy 以 TCP 四层方式负载到多个 `novartc-server` 实例
- RocketMQ Dashboard：`http://localhost:8082`
- Elasticsearch：`http://localhost:9200`
- Kibana：`http://localhost:5601`
- 容器内后端使用 `docker` profile，并通过服务名访问中间件，不再使用 `localhost`
- Compose 已固定 Docker 网段为 `172.19.0.0/24`，网关为 `172.19.0.1`，便于和 Druid 白名单联动
- Filebeat 容器日志版配置采集的是 Docker 容器日志（`/var/lib/docker/containers`），并按容器名与 JSON 日志中的 logger 字段自动分类
- 如果你重新打包了 jar，重启后端容器即可：

```bash
docker compose -f docker-compose.local.yml restart novartc-server filebeat
```
