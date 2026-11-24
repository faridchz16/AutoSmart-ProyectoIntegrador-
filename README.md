# AutoSmart · Proyecto Integrador

Sistema de gestión para un taller mecánico: el cliente reserva y sigue la atención de su vehículo, aprueba presupuestos, paga en línea y califica al mecánico; el mecánico atiende las órdenes de trabajo; y el administrador supervisa citas, pagos, inventario, calificaciones e indicadores. Incluye un asistente (chatbot) para preguntas frecuentes, consultas de estado y derivación a un asesor.

Curso Construcción y Pruebas de Software · Tecsup · C24-4-B · 2026-2.

## Partes del sistema

| Parte | Carpeta | Tecnología | Responsable |
|---|---|---|---|
| Backend (API REST) | [`usuario/backend`](usuario/backend) | Spring Boot 4 (Java 21), Spring Security + JWT, JPA | Farid Chávez |
| Base de datos | [`database`](database) | MySQL / MariaDB | Farid Chávez |
| Web (cliente, mecánico y administrador) | [`usuario/frontend`](usuario/frontend) | React 19 + Vite + React Router | Jordan Abad |
| App móvil (cliente y mecánico) | [`mobile/android`](mobile/android) | Android (Kotlin, Jetpack Compose, Retrofit) | Yony Ortiz |
| Panel de consulta (opcional) | [`usuario/backend-django`](usuario/backend-django) | Django 4.2 | Equipo |

## Empezar

Sigue la guía de instalación: **[docs/INSTALACION.md](docs/INSTALACION.md)** (base de datos, backend, web, app y primer uso).

Resumen para desarrollo:

```bash
# 1. MySQL encendido (XAMPP) y database/schema.sql ejecutado
# 2. Backend
cd usuario/backend && mvnw.cmd spring-boot:run     # http://localhost:8081
# 3. Web (otra consola)
cd usuario/frontend && npm install && npm run dev  # http://localhost:5173
```

## Documentación

| Documento | Contenido |
|---|---|
| [docs/INSTALACION.md](docs/INSTALACION.md) | Instalación completa y problemas frecuentes |
| [usuario/frontend/README.md](usuario/frontend/README.md) | Web: configuración, modo de datos de prueba, pantallas por rol |
| [mobile/android/README.md](mobile/android/README.md) | App móvil |
| [docs/pruebas/PRUEBAS_FUNCIONALES.md](docs/pruebas/PRUEBAS_FUNCIONALES.md) | Pruebas de la API (Postman) y de la web de los Sprints 1 y 2 |
| [docs/pruebas/PRUEBAS_ACEPTACION.md](docs/pruebas/PRUEBAS_ACEPTACION.md) | Pruebas de aceptación de punta a punta del Sprint 4 con el Product Owner |

## Forma de trabajo

Una rama `feature/*` por módulo (`feature/backend-*`, `feature/frontend-*`, `feature/mobile-*`) → Pull Request revisado → `main`. Cada commit lleva el ID de su tarea del Sprint Backlog (por ejemplo, `feat(frontend): ... (T70.2)`).
