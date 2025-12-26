FROM gradle:jdk21 AS builder
WORKDIR /home/gradle/project
COPY . .

RUN gradle :payment-service:bootJar --no-daemon --exclude-task test

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=builder /home/gradle/project/payment-service/build/libs/payment-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]