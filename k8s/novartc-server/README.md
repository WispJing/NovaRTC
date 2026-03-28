# novartc-server 本地 K8s 部署说明

这套清单用于在本机 Docker Desktop Kubernetes 环境中运行 `novartc-server`。
当前方案只把 `novartc-server` 放进 K8s，MySQL、Redis、MongoDB、RocketMQ 继续复用宿主机上已有的本地服务。

## 当前约定

- Kubernetes: Docker Desktop 自带集群
- Namespace: `novartc`
- HTTP NodePort: `30070`
- IM NodePort: `30688`
- HTTP 访问地址: `http://127.0.0.1:30070`
- IM 访问地址: `127.0.0.1:30688`
- 依赖地址通过 `host.docker.internal` 访问宿主机服务
- 当前固定单副本运行

## 目录说明

- `novartc-server/Dockerfile`: 本地镜像构建文件
- `k8s/novartc-server/base`: Kubernetes 基础清单

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

## 部署步骤

### 1. 重新打包服务

在仓库根目录执行:

```powershell
mvn -pl novartc-server -am -DskipTests package
```

### 2. 构建本地镜像

在 `novartc-server` 目录执行:

```powershell
docker build -t novartc-server:20260328-1655 .
```

说明:

- `deployment.yaml` 当前使用镜像 `novartc-server:20260328-1655`
- 如果后续重新发版，建议换一个新 tag，再同步修改 `deployment.yaml`

### 3. 部署到本机 K8s

```powershell
kubectl apply -k D:\NovaRTC\NovaRTC\k8s\novartc-server\base
```

如果只是镜像内容更新，也可以:

```powershell
kubectl -n novartc rollout restart deployment/novartc-server
```

## 验证

查看资源状态:

```powershell
kubectl -n novartc get pods -o wide
kubectl -n novartc get svc
kubectl -n novartc logs deploy/novartc-server --tail=100
```

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

## 当前行为说明

- `/api/**` 会走 token 鉴权拦截
- `/actuator/**` 不再走 token 鉴权
- `/actuator/**` 不再被统一响应包装，会返回 Spring Boot 原生 JSON

## 可选 Ingress

`base/ingress.yaml` 是 Docker Desktop 环境下的可选配置，不是当前联调必需项。

使用前提:

- 你已经在本机集群安装并启用了 NGINX Ingress Controller
- 本机 `80` 端口可以被 Ingress 使用

启用后可通过以下域名访问:

- `http://novartc.127.0.0.1.nip.io/actuator/health/readiness`

如果你当前没有安装 Ingress Controller，直接走 `NodePort` 即可。

## 后续扩容注意事项

如果未来要把 `novartc-server` 扩成多副本，至少还需要处理:

- `workerId/datacenterId` 的唯一分配
- IM 长连接摘流和重连
- 四层流量入口策略
