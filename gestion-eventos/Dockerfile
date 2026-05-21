# Paso 1: Compilar la aplicación usando Maven y Java 17
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
# Nos metemos a la carpeta donde realmente está tu pom.xml y tu código
WORKDIR /gestion-eventos
RUN mvn clean package -DskipTests

# Paso 2: Ejecutar la aplicación
FROM openjdk:17-jdk-slim
# Copiamos el jar desde la carpeta de compilación correcta
COPY --from=build /gestion-eventos/target/gestion-eventos-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]