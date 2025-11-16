FROM gradle:8.14-jdk21 AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src src/

RUN gradle bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

RUN apk add --no-cache wget

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
