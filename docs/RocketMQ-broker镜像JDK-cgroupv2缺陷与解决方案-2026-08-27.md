# RocketMQ broker 镜像 JDK 8u372 cgroup v2 缺陷与解决方案

> 记录日期：2026-08-27
> 影响范围：`NovaRTC/docker/docker-compose.yml` 中 `apache/rocketmq:4.9.6` 镜像的 broker 服务

## 故障现象

- 新建的 CLUSTERING 消费组（如定时任务消费组 `novartc-job-consumer-group`）无法消费任何消息；
- 客户端日志（`C:\Users\<用户>\logs\rocketmqlogs\rocketmq_client.log`）持续循环报错：
  - `fetchConsumeOffsetFromBroker exception`
  - `Compute consume offset exception`
  - `doRebalance, xxx, add new mq failed`
- broker 返回的异常信息为：
  `java.lang.NoClassDefFoundError: Could not initialize class org.apache.rocketmq.store.StoreUtil, org.apache.rocketmq.store.DefaultMessageStore.checkInDiskByCommitOffset(...)`
- `mqadmin resetOffsetByTime` 报 `consumer group not exist`（消费组尚无提交位点）；
- **重启 broker 后故障复现**，说明不是运行期偶发损坏。

## 根因分析

1. 消费组无提交位点时，客户端首次拉取要向 broker 请求默认位点，
   broker 走 `DefaultMessageStore.checkInDiskByCommitOffset` → 首次触发 `StoreUtil` 类静态初始化；
2. `StoreUtil` 的静态初始化调用 `ManagementFactory.getOperatingSystemMXBean()` 获取物理内存大小；
3. 镜像自带 JDK 为 **OpenJDK 8u372**，该版本在 **cgroup v2** 环境下存在已知缺陷：
   `jdk.internal.platform.CgroupV2Subsystem.getInstance` 初始化抛 `NullPointerException`，
   导致 `OperatingSystemMXBean` 构造失败；
4. 异常在 `StoreUtil.<clinit>` 中触发 `ExceptionInInitializerError`，
   类被标记为初始化失败，此后该 broker JVM 生命周期内所有调用均抛
   `NoClassDefFoundError: Could not initialize class org.apache.rocketmq.store.StoreUtil`，不可恢复。

容器内最小复现（已验证）：

```java
// 直接调用即抛 ExceptionInInitializerError，
# 堆栈：CgroupV2Subsystem.getInstance -> NullPointerException
long v = org.apache.rocketmq.store.StoreUtil.getTotalPhysicalMemorySize();
```

BROADCASTING 消费组（如 IM 消费者）不受影响：广播模式位点存客户端本地，不向 broker 请求默认位点。

## 解决方案

关闭 broker JVM 的容器感知，绕开 cgroup 探测。`runbroker.sh` 支持通过环境变量
`JAVA_OPT_EXT` 追加 JVM 参数，已在 `docker-compose.yml` 的 broker 服务中配置：

```yaml
environment:
  - NAMESRV_ADDR=namesrv:9876
  - TZ=UTC
  # 镜像内 JDK 8u372 在 cgroup v2 环境下初始化 OperatingSystemMXBean 会 NPE，
  # 导致 StoreUtil 静态初始化失败、新消费组无法拉取位点；关闭容器感知规避
  - JAVA_OPT_EXT=-XX:-UseContainerSupport
```

副作用说明：关闭容器感知后 JVM 不再按 cgroup 限制自动推算堆大小，
但本项目 broker 启动参数已显式指定 `-Xms/-Xmx`（见镜像 `runbroker.sh`），无实际影响。

生效方式：

```bash
cd NovaRTC/docker
docker compose up -d broker   # 重建容器使环境变量生效
```

## 验证结果（2026-08-27）

- `consumerProgress -g novartc-job-consumer-group`：4 个队列
  Consumer Offset 与 Broker Offset 持平，`Diff Total: 0`；
- 定时任务端到端链路恢复：调度平台触发 → RocketMQ 通知 →
  `novartc-server` 反射执行 → 结果回传 → `job_log` 更新；
- 此前积压消息全部被正常消费。

## 后续建议

- 升级 RocketMQ 镜像版本（如 5.x），或确认新镜像内置 JDK 已修复 cgroup v2 支持
  （8u392 及以上版本）后，可移除 `-XX:-UseContainerSupport`；
- 升级前请保留本配置，避免 Docker Desktop 的 cgroup v2 环境再次触发该缺陷。
