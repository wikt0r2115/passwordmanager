FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean verify

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --home-dir /data --no-create-home app \
    && mkdir -p /app /data/.passwordmanager \
    && chown -R app:app /data
WORKDIR /data
COPY --from=build /build/target/passwordmanager-1.0.0.jar /app/passwordmanager.jar
USER app
ENTRYPOINT ["java", "-Duser.home=/data", "-jar", "/app/passwordmanager.jar"]
CMD ["--help"]
