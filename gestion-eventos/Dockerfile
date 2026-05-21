# Paso 1: Usar una imagen limpia de Maven con Java 17 para compilar
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Paso 2: Usar una imagen liviana de Java 17 para ejecutar el sistema
FROM openjdk:17-jdk-slim
WORKDIR /app
# Copiamos el archivo .jar apuntando directo a la carpeta target sin adivinanzas
COPY --from=build /app/target/gestion-eventos-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]