# Stage 1: build the jar with the full JDK.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# Copy only the build definition first so Docker caches downloaded dependencies between code changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
# Tests run in CI, not here: integration tests need Docker, which isn't available inside a Docker build.
RUN ./mvnw -B -q package -DskipTests

# Stage 2: run on the smaller JRE-only image.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/planning-service-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
