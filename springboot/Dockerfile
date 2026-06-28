# ── Stage 1: Build ─────────────────────────────────────────────────────────────
FROM gradle:8-jdk17 AS build

WORKDIR /app

# Resolve deps before copying source — this layer is cached as long as build.gradle unchanged.
COPY build.gradle settings.gradle ./
RUN gradle --no-daemon dependencies || true

COPY src ./src
RUN gradle --no-daemon bootJar -x test

# ── Stage 2: Runtime ───────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
