# Etapa 1: Compilación
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copiar definición del proyecto para aprovechar la caché de capas de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y empaquetar la aplicación
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen ligera de ejecución
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el artefacto JAR compilado
COPY --from=builder /app/target/puntoventaapi-0.0.1-SNAPSHOT.jar app.jar

# Puerto por defecto (Render asignará su propio PORT en runtime)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
