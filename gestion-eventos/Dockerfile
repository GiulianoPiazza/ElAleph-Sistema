# Paso 1: Buscar y compilar todos los pom.xml del repositorio
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Paso 2: Ejecutar la aplicación buscando el archivo .jar de forma automática
FROM openjdk:17-jdk-slim
COPY --from=build /**/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]