
# Stage 1: Build the Spring Boot application
FROM maven:3.9.16-eclipse-temurin-25 AS build

WORKDIR /workspace
COPY . .

RUN mvn -B -DskipTests package \
    && test -f target/aurora-backend-0.0.1-SNAPSHOT.jar

# Stage 2: Run the application
FROM eclipse-temurin:25-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 aurora \
    && useradd --system --uid 10001 --gid aurora --home-dir /app aurora \
    && mkdir -p /app/data/project-workspaces \
    && chown -R aurora:aurora /app

COPY --from=build --chown=aurora:aurora \
    /workspace/target/aurora-backend-0.0.1-SNAPSHOT.jar /app/app.jar

USER 10001:10001

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","/app/app.jar"]
