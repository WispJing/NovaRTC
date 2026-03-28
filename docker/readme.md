### 启动
```bash
docker compose up -d
```

### 停止
```bash
docker compose down
```

### 修改文件重新创建，以*dashboard*为例
```bash
docker-compose up -d --force-recreate redis
```

### 重启
```bash
docker compose restart redis
```