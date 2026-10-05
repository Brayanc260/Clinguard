# ==================== Dockerfile — ClinGuard CDSS ====================
# Etapa 1: Compilar el proyecto con Maven
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copiar solo el pom primero para aprovechar cache de dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar
COPY src ./src
RUN mvn package -DskipTests -B

# ==================== Etapa 2: Imagen final de ejecución ====================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar el JAR generado desde la etapa de build
COPY --from=builder /app/target/clinica-2.0.0.jar app.jar

# Exponer el puerto de la aplicación
EXPOSE 8080

# Comando de arranque
ENTRYPOINT ["java", "-jar", "app.jar"]
