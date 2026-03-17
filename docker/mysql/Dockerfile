# 基于官方 MySQL 8.0 镜像
FROM mysql:8.0

# 将本地的 my.cnf 复制到镜像内的 conf.d 目录
# 注意：COPY 会自动保留文件权限，但建议显式设置
COPY --chown=mysql:mysql --chmod=644 ./conf/my.cnf /etc/mysql/conf.d/my.cnf

# 可选：添加健康检查或标签
# LABEL version="1.0" description="MySQL with custom my.cnf"