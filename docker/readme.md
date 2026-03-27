### 启动
```bash
docker compose -f docker-compose.yml up -d
```
```bash
docker compose -f docker-compose.local.yml up -d
```

### 停止
```bash
docker compose down
```
### 修改文件重新创建，以*dashboard*为例
```bash
docker-compose up -d --force-recreate redis
```
```bash
docker compose -f docker-compose.local.yml restart novartc-server
```