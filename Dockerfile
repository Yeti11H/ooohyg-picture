# 1. 基础镜像：OpenJDK 17 Alpine (轻量级)
FROM openjdk:17-jdk-alpine

# 2. 作者信息
LABEL maintainer="ooohyg"

# 3. 设置工作目录
WORKDIR /app

# 4. 复制 jar 包
# 请确保 target 目录下生成的 jar 包名字也是这个
COPY ooohyg-picture-0.0.1-SNAPSHOT.jar app.jar

# 5. 暴露端口 (和你 yml 里的一致)
EXPOSE 8000

# 6. 启动命令 (🚀 重点优化：限制了最大堆内存为 512M)
# 解释：
# -Xms256m: 初始堆内存 256MB
# -Xmx512m: 最大堆内存 512MB (防止 OOM 杀进程)
# --spring.profiles.active=prod: 强制使用 prod 配置
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar", "--spring.profiles.active=prod"]
