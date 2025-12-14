FROM gradle:jdk21 AS builder
WORKDIR /home/gradle/project
COPY . .
RUN gradle :market-service:bootJar --no-daemon --exclude-task test

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=builder /home/gradle/project/market-service/build/libs/market-service.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

