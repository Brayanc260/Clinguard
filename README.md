# ClinGuard — CDSS Platform

Sistema de Soporte a Decisiones Clínicas (CDSS) para conversión y validación de dosis médicas.

## 🚀 Despliegue con Docker

### Requisitos
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y en ejecución

### Instrucciones

```bash
# 1. Clonar el repositorio
git clone <url-del-repositorio>
cd ClinicaSpring

# 2. Levantar toda la aplicación con un solo comando
docker compose up
```

La primera vez tardará unos minutos mientras Docker descarga las imágenes base y compila el proyecto.

### Acceso

Una vez que veas el mensaje `Started ClinicaApplication` en la consola, abre:

```
http://localhost:8080
```

### Credenciales de demo

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | Administrador |
| `medico` | `medico123` | Médico |
| `enfermero` | `enfermero123` | Enfermero |

### Detener la aplicación

```bash
docker compose down
```

Para eliminar también los datos de MongoDB:

```bash
docker compose down -v
```

---

## 🏗️ Arquitectura

| Servicio | Imagen | Puerto |
|---|---|---|
| `app` | Spring Boot 3.3 / Java 21 | `8080` |
| `mongo` | MongoDB 7.0 | `27017` |

## 🛠️ Desarrollo local (sin Docker)

Requiere MongoDB instalado localmente en `localhost:27017`.

```bash
# Compilar y ejecutar
mvn spring-boot:run
```
