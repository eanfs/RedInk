# 构建阶段
FROM maven:3.9.4-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制pom.xml并下载依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装必要的工具
RUN apk add --no-cache curl

# 复制jar文件
COPY --from=builder /app/target/redink-java-backend-1.0.0.jar app.jar

# 暴露端口
EXPOSE 12398

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:12398/api/health || exit 1

# 运行应用
ENTRYPOINT ["java", "-jar", "app.jar"]