### 启动
```bash
docker compose up -d
```

### 停止
```bash
docker compose down
```

### 变更 RocketMQ 配置后重建

```bash
docker compose up -d --force-recreate namesrv broker dashboard
```

### 重启单个服务

```bash
docker compose restart redis
```
