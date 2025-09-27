# 1. 构建阶段
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# 2. 部署阶段
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=builder /build/target/elm-1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
