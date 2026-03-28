# novartc-server 本地 K8s 部署说明

这套清单用于在本机 Docker Desktop Kubernetes 环境中运行 `novartc-server`。
当前方案只把 `novartc-server` 放进 K8s，MySQL、Redis、MongoDB、RocketMQ 继续复用宿主机上已有的本地服务。

## 当前约定

- Kubernetes: Docker Desktop 自带集群
- Namespace: `novartc`
- Workload: `StatefulSet/novartc-server`
- Replicas: `2`
- HTTP NodePort: `30070`
- IM NodePort: `30688`
- HTTP 访问地址: `http://127.0.0.1:30070`
- IM 访问地址: `127.0.0.1:30688`
- 依赖地址通过 `host.docker.internal` 访问宿主机服务
- 使用 `headless service` 为 Pod 提供稳定网络身份
- `workerId/datacenterId` 由 Pod 名序号自动推导，不再手工配置

## 目录说明

- `novartc-server/Dockerfile`: 本地镜像构建文件
- `k8s/novartc-server/base`: Kubernetes 基础清单
- `k8s/novartc-server/redeploy-local.ps1`: 一键重发版脚本

## 已适配的本地配置

`k8s/novartc-server/base/configmap.yaml` 已按本机环境写好:

- Redis: `host.docker.internal:6379`
- MongoDB: `host.docker.internal:27017`
- MySQL: `host.docker.internal:3307`
- RocketMQ NameServer: `host.docker.internal:9876`
- IM 对客户端暴露端口: `30688`

`k8s/novartc-server/base/secret.yaml` 当前使用本地账号:

- MySQL username: `root`
- MySQL password: `123456`

如果你的本地服务端口或密码变化了，只需要改这两个文件。

## 多副本 ID 分配说明

当前 `novartc-server` 使用 Snowflake 风格消息 ID。
为了支持多副本，服务启动时会优先读取 `POD_NAME`，从 `novartc-server-0`、`novartc-server-1` 这类 Pod 名里提取末尾序号，再映射成:

- `datacenterId = ordinal / 32`
- `workerId = ordinal % 32`

当前 2 个副本的实际效果是:

- `novartc-server-0 -> datacenterId=0, workerId=0`
- `novartc-server-1 -> datacenterId=0, workerId=1`

如果将来副本数继续增加，序号会继续顺延。

## 一键重发版

优先使用仓库内脚本:

```powershell
powershell -ExecutionPolicy Bypass -File D:\NovaRTC\NovaRTC\k8s\novartc-server\redeploy-local.ps1
```

可选: 指定镜像 tag

```powershell
powershell -ExecutionPolicy Bypass -File D:\NovaRTC\NovaRTC\k8s\novartc-server\redeploy-local.ps1 -ImageTag 20260328-184200
```

脚本会自动完成:

- Maven 打包 `novartc-server`
- 本地构建镜像
- 更新 `base/deployment.yaml` 里的镜像 tag
- 如果当前工作负载是 `StatefulSet`，自动删除遗留同名 `Deployment`
- `kubectl apply -k`
- 等待 rollout 完成

## 手动部署步骤

### 1. 重新打包服务

在仓库根目录执行:

```powershell
mvn -pl novartc-server -am -DskipTests package
```

### 2. 构建本地镜像

在 `novartc-server` 目录执行:

```powershell
docker build -t novartc-server:20260328-184200 .
```

说明:

- `base/deployment.yaml` 当前使用具体镜像 tag
- 如果后续重新发版，建议换一个新 tag，再同步修改 `deployment.yaml`

### 3. 部署到本机 K8s

```powershell
kubectl apply -k D:\NovaRTC\NovaRTC\k8s\novartc-server\base
```

查看滚动发布状态:

```powershell
kubectl -n novartc rollout status statefulset/novartc-server --timeout=300s
```

## 验证

查看资源状态:

```powershell
kubectl -n novartc get pods -l app=novartc-server -o wide
kubectl -n novartc get svc
kubectl -n novartc logs novartc-server-0 --tail=100
kubectl -n novartc logs novartc-server-1 --tail=100
```

期望当前有 2 个 Pod，且都为 `1/1 Running`。

验证 HTTP:

```powershell
curl.exe -i http://127.0.0.1:30070/actuator/health/readiness
```

期望返回:

```json
{"status":"UP"}
```

验证 IM 端口:

```powershell
Test-NetConnection -ComputerName 127.0.0.1 -Port 30688
```

期望 `TcpTestSucceeded` 为 `True`。

验证每个 Pod 的 Snowflake 节点号:

```powershell
kubectl -n novartc logs novartc-server-0 | Select-String -Pattern 'Using derived snowflake node id'
kubectl -n novartc logs novartc-server-1 | Select-String -Pattern 'Using derived snowflake node id'
```

## 当前行为说明

- `/api/**` 会走 token 鉴权拦截
- `/actuator/**` 不再走 token 鉴权
- `/actuator/**` 不再被统一响应包装，会返回 Spring Boot 原生 JSON
- NodePort 入口仍然只有一个，但服务内部已经是 2 个 IM 实例共同工作

## 可选 Ingress

`base/ingress.yaml` 是 Docker Desktop 环境下的可选配置，不是当前联调必需项。

使用前提:

- 你已经在本机集群安装并启用了 NGINX Ingress Controller
- 本机 `80` 端口可以被 Ingress 使用

启用后可通过以下域名访问:

- `http://novartc.127.0.0.1.nip.io/actuator/health/readiness`

如果你当前没有安装 Ingress Controller，直接走 `NodePort` 即可。

## 后续扩容注意事项

当前多副本已经能稳定分配唯一 `workerId/datacenterId`，但如果继续往生产化推进，仍建议继续完善:

- IM 长连接摘流和客户端重连策略
- NodePort 或四层入口的连接分布与会话迁移策略
- RocketMQ、Redis、MongoDB、MySQL 的高可用和监控
