# FROM openjdk:11-jre-slim

# WORKDIR /app
# COPY target/elmboot-0.0.1-SNAPSHOT.jar app.jar

# EXPOSE 8080
# ENTRYPOINT ["java", "-jar", "app.jar"]

# === Stage 1: Build with Maven ===
FROM maven:3.9.6-eclipse-temurin-11 AS builder

WORKDIR /app

# 先复制 pom.xml 和依赖，缓存依赖
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 再复制源码，打包 JAR
COPY src ./src
RUN mvn clean package -DskipTests

# === Stage 2: Runtime with JRE ===
FROM openjdk:11-jre-slim

WORKDIR /app

# 从前一阶段拷贝 JAR
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

