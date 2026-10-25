# Stage 1: build the jar with the full JDK.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# Copy only the build definition first so Docker caches downloaded dependencies between code changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
# Tests run in CI, not during the image build, to keep image builds fast.
RUN ./mvnw -B -q package -DskipTests

# Stage 2: run on the smaller JRE-only image.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/commentary-service-*.jar app.jar
USER app
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
