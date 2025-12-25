# Dev stage for local development (used by docker-compose.dev.yml build target)
FROM maven:3.9.0-eclipse-temurin-17 AS dev
WORKDIR /app
# Copy sources so container can run 'mvn spring-boot:run' (overridden by mount in compose)
COPY pom.xml ./
COPY src ./src
CMD ["mvn","-DskipTests","spring-boot:run"]

# Build stage
# Supports build args to speed up local development builds and CI.
# Use: docker build --build-arg SKIP_TESTS=true --build-arg SKIP_REPACKAGE=true --build-arg MVN_THREADS=1C .
FROM maven:3.9.0-eclipse-temurin-17 AS builder
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src

# Build-time args for faster builds / dev convenience
ARG SKIP_TESTS=true
ARG SKIP_REPACKAGE=false
ARG MVN_THREADS=1C
ARG APP_JAR=backend-0.1.0.jar

# Conditional mvn invocation: skip repackaging if requested (much faster)
RUN if [ "${SKIP_REPACKAGE}" = "true" ]; then \
    mvn -T ${MVN_THREADS} -B -DskipTests=${SKIP_TESTS} package -Dspring-boot.repackage.skip=true ; \
    else \
    mvn -T ${MVN_THREADS} -B -DskipTests=${SKIP_TESTS} package spring-boot:repackage ; \
    fi

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Re-declare ARG so it's available in this stage (prevents empty expansion causing directory copy)
ARG APP_JAR=backend-0.1.0.jar
# Allow overriding artifact name with build-arg APP_JAR
COPY --from=builder /workspace/target/${APP_JAR} ./app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
