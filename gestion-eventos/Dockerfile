# Paso 1: Compilar la aplicación usando Maven y Java 17
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Paso 2: Ejecutar la aplicación usando Java 17
FROM openjdk:17-jdk-slim
COPY --from=build /target/gestion-eventos-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]