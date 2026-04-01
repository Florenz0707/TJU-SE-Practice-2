FROM eclipse-temurin:21-jre
WORKDIR /app
# Copy pre-built jar from build context (run 'mvn package' on host first)
COPY wallet-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
