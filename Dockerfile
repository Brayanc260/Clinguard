# ==========================================
# ETAPA 1: CONSTRUCCIÓN (Compilación con Maven)
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copiar el archivo pom.xml y descargar dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar (Forzando los nombres de parámetros)
COPY src ./src
RUN mvn clean package -DskipTests -B -Dmaven.compiler.parameters=true

# ==========================================
# ETAPA 2: EJECUCIÓN (Imagen ligera)
# ==========================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "--add-opens", "java.base/java.time=ALL-UNNAMED", "-jar", "app.jar"]