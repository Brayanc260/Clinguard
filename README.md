# ClinGuard - Clinical Decision Support System (CDSS)

ClinGuard es una plataforma de apoyo a la toma de decisiones clínicas diseñada para la dosificación de medicamentos y la conversión de unidades médicas. El sistema permite validar dosis de forma segura mediante un semáforo clínico, gestionar pacientes, medicamentos, citas y mantener un historial de auditoría clínica. 

## Características Principales

- **Autenticación y Autorización:** Sistema de inicio de sesión seguro implementado con Spring Security y tokens JWT. Control de acceso basado en roles (Administrador, Médico, Enfermero, Recepcionista, Usuario).
- **Módulo de Conversiones Médicas:**
  - Conversión de unidades para Masa, Volumen y Laboratorio.
  - Validación Clínica (Semáforo): Evaluación automática de dosis (Segura, Infradosis, Sobredosis) basada en el peso del paciente y los rangos establecidos del medicamento.
  - Firma de Auditoría: Registro automático del profesional de la salud que realiza la conversión, junto con la fecha y hora exacta.
- **Gestión Clínica:** Operaciones CRUD (Crear, Leer, Actualizar, Eliminar) para Pacientes, Medicamentos y Citas.
- **Dashboard de Auditoría:** Panel exclusivo para administradores que incluye gráficos estadísticos de conversiones, alertas clínicas y trazabilidad de las dosis administradas.
- **Perfil de Usuario:** Gestión y actualización de datos personales y credenciales de la cuenta.
- **Diseño de Interfaz:** Interfaz responsiva con soporte para modo claro/oscuro, desarrollada con CSS/JS y gráficos dinámicos integrados con Chart.js.

## Tecnologías Utilizadas

- **Backend:** Java 21, Spring Boot 3.3, Spring Security, JWT.
- **Base de Datos:** MongoDB.
- **Frontend:** HTML5, CSS3, Thymeleaf, JavaScript, Boxicons, Chart.js.
- **Despliegue y Herramientas:** Docker, Docker Compose, Maven, GitHub Codespaces.

## Instalación y Ejecución

### Ejecución Local (IDE)
1. Clonar el repositorio:
   ```bash
   git clone https://github.com/Brayanc260/Clinguard.git
