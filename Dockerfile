# Multi-stage Dockerfile: build with Maven, run on Java 21
FROM maven:3.9.4-eclipse-temurin-21 as builder
WORKDIR /app
# copy pom and download dependencies first to leverage caching
COPY pom.xml mvnw* ./
COPY .mvn .mvn
RUN mvn -B -DskipTests dependency:go-offline

# copy source and build
COPY src ./src
RUN mvn -B -DskipTests package

# Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/JewelryShopManagement-1.0-SNAPSHOT.jar /app/app.jar
EXPOSE 8081
# Use a shell entrypoint so ${PORT} environment var is expanded at runtime
ENTRYPOINT ["sh","-c","java -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8081} -Xms256m -Xmx512m -jar /app/app.jar"]
