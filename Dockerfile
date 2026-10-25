# syntax=docker/dockerfile:1

# Dependencies only: this layer is reused until pom.xml changes.
FROM maven:3.9-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml lombok.config ./
RUN mvn -B -q dependency:go-offline

# Test image: `docker run` it to execute the suite against a throwaway H2 file inside the container.
FROM deps AS test
COPY src ./src
COPY fixtures ./fixtures
CMD ["mvn", "-B", "test"]

FROM deps AS build
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN useradd --system --no-create-home app && mkdir /app/data && chown app /app/data
COPY --from=build /app/target/travel-billing-*.jar app.jar
USER app
EXPOSE 8080
VOLUME ["/app/data"]
ENTRYPOINT ["java", "-jar", "app.jar"]
