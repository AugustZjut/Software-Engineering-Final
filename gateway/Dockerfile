# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# --- 关键修复：复制所有模块的 POM 文件 ---
COPY pom.xml ./pom.xml
COPY Server/pom.xml ./Server/pom.xml
COPY gateway/pom.xml ./gateway/pom.xml
COPY eureka-server/pom.xml ./eureka-server/pom.xml

# 编译 Gateway
COPY gateway/src ./gateway/src
RUN mvn -pl gateway -am clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /workspace/gateway/target/*.jar app.jar
ENV JAVA_OPTS=""
# 端口由 Java 代码控制，此处仅做声明
EXPOSE 8040
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]