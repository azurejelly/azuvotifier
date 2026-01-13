FROM gradle:8.12.0-jdk21 AS build

WORKDIR /build

COPY gradlew gradlew
COPY gradle/ gradle/
COPY build.gradle.kts .
COPY settings.gradle.kts .

RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    ./gradlew --no-daemon build -x test || true

COPY . .

RUN --mount=type=cache,target=/home/gradle/.gradle/caches \
    ./gradlew --no-daemon azuvotifier-standalone:build

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build "/build/standalone/build/libs/azuvotifier-standalone-*-dist.jar" ./azuvotifier-standalone.jar

EXPOSE 8192/tcp

ENTRYPOINT ["java", "-jar", "azuvotifier-standalone.jar"]