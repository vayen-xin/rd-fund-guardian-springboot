# 构建阶段
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# 复制Maven wrapper和pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# 下载依赖
RUN ./mvnw dependency:go-offline -B

# 复制源码
COPY src src

# 打包
RUN ./mvnw clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制jar文件
COPY --from=builder /app/target/*.jar app.jar

# 复制配置文件
COPY application.yml /app/application.yml

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
