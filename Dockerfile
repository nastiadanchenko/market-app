FROM gradle:jdk21 AS builder
WORKDIR /home/gradle/project
COPY . .
RUN gradle bootJar --no-daemon --exclude-task test

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=builder /home/gradle/project/build/libs/market-app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

