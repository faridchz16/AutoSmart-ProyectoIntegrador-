# SOLID y buenas prácticas en el backend de AutoSmart

Backend: Spring Boot 4 · Java · JPA/Hibernate · MariaDB. Paquete raíz: `usuario/backend/src/main/java/com/tecsup/autosmart`.

## Estructura en capas

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Controladores | `controller` | Reciben la petición HTTP, validan con `@Valid` y devuelven el código correcto. No tienen lógica de negocio |
| Servicios | `service` | Reglas de negocio y transacciones (`@Transactional`) |
| Repositorios | `repository` | Acceso a datos con Spring Data JPA (consultas derivadas y `@Query` en JPQL) |
| Entidades | `model` | Tablas mapeadas con JPA y reglas propias del dominio |
| DTOs | `dto` | Lo que entra y sale de la API (nunca se exponen las entidades) |
| Mappers | `mapper` | Conversión entidad → DTO |
| Errores | `exception` | Excepciones propias y `GlobalExceptionHandler` |
| Estrategias | `pasarela`, `chatbot`, `reporte`, `almacenamiento` | Interfaces con varias implementaciones intercambiables |

## S — Responsabilidad única

| Dónde | Qué se separó |
|---|---|
| `mapper/*Mapper` (8 clases) | La conversión a DTO sale de los servicios: `PagoMapper`, `CalificacionMapper`, `MovimientoMapper`… |
| `PagoService` / `PagoEfectivoService` | Pago en línea y cobro en efectivo del admin son flujos distintos |
| `CalificacionService` / `ModeracionCalificacionService` | El cliente califica; el admin modera |
| `ChatbotService` / `DerivacionService` | El chat conversa; las derivaciones tienen su propia lógica |
| `Cita.cambiarEstado`, `Material.descontar`, `OrdenTrabajo.sumarCostoRepuestos` | Cada entidad protege sus propias reglas (no se puede pasar a un estado inválido ni dejar stock negativo) |
| `MetricasRepository` | Un repositorio solo de lectura para los reportes del admin |

## O — Abierto/cerrado

Se agregan comportamientos **creando clases nuevas**, sin modificar las existentes:

| Punto de extensión | Implementaciones | Para agregar uno nuevo |
|---|---|---|
| `pasarela/PasarelaPago` | `TarjetaSandboxPasarela`, `YapePlinSandboxPasarela` | Nueva clase `@Component` (ej. una pasarela real) |
| `chatbot/ManejadorIntencion` | Saludo, FAQ, Servicios, Horario, Cotizar, Estado, Asesor, Agendar | Nueva clase con su prioridad; `ChatbotService` no cambia |
| `reporte/FuenteReporte` | Pagos, Citas, Métricas | Nueva fuente (ej. inventario) |
| `reporte/ExportadorReporte` | Excel (Apache POI), PDF (OpenPDF) | Nuevo formato (ej. CSV) |
| `model/EstadoCita.puedeCambiarA` | Transiciones de la cita | Se edita solo el mapa de transiciones |

Prueba de que funciona: T76.1, T78.1 y T79.1 agregaron 4 intenciones al chatbot sin cambiar la lógica de `ChatbotService`.

## L — Sustitución de Liskov

Cualquier implementación puede reemplazar a su interfaz sin que el código que la usa lo note:

- `PagoService` cobra con cualquier `PasarelaPago`; todas devuelven `ResultadoPasarela` (aprobado o rechazado), nunca lanzan errores distintos.
- `ReporteService` exporta con cualquier `ExportadorReporte`: todos reciben el mismo `Reporte` y devuelven `byte[]`.
- `FotoDeficienciaService` funciona igual con cualquier `AlmacenFotos`.

## I — Segregación de interfaces

- Interfaces pequeñas y específicas: `AlmacenFotos` (3 métodos), `ExportadorReporte` (3), `FuenteReporte` (2), `PasarelaPago` (2).
- `ManejadorIntencion.requiereSesion()` es un método `default`: solo lo sobrescriben las intenciones que necesitan sesión.
- `MetricasRepository` extiende `Repository` (no `JpaRepository`): expone solo consultas, sin `save` ni `delete`.

## D — Inversión de dependencias

Los servicios dependen de abstracciones; Spring inyecta la implementación:

| Servicio | Depende de | Implementación |
|---|---|---|
| `PagoService` | `List<PasarelaPago>` | Pasarelas sandbox |
| `ChatbotService` | `List<ManejadorIntencion>` | 8 manejadores |
| `ReporteService` | `List<FuenteReporte>`, `List<ExportadorReporte>` | 3 fuentes, 2 formatos |
| `FotoDeficienciaService` | `AlmacenFotos` | `AlmacenFotosDisco` |
| `AgendarManejador` | `AlmacenBorradores` | `AlmacenBorradoresMemoria` (expira en 30 min) |
| Servicios con fechas | `java.time.Clock` (`ClockConfig`) | Reloj de Lima; en los tests, un reloj fijo |

## Manejo de errores

| Situación | Respuesta |
|---|---|
| Datos inválidos (`@Valid`, fechas o filtros mal escritos) | 400 con mensaje claro |
| Sin token o token inválido | 401 |
| Recurso de otro usuario o rol sin permiso | 403 |
| No existe | 404 |
| Método HTTP incorrecto | 405 |
| Regla de negocio (estado inválido, ya pagado, sin stock, turno ocupado) | 409 |
| Base de datos caída | 503 |
| Error inesperado | 500 genérico (el detalle queda en el log) |

Otros puntos: `try/catch` donde una librería puede fallar (PDF, Excel, archivos); el chatbot nunca se cae (si un manejador falla responde un mensaje amable); valores nulos controlados en mappers y servicios.

## Consultas y ORM

- Hibernate con `ddl-auto=update` y `schema.sql` versionado.
- `JOIN FETCH` y `list_select_related` (Django) para evitar el problema N+1.
- Consultas agregadas (`COUNT`, `SUM`, `AVG` con `GROUP BY`) que llenan records directamente.

## Números del proyecto (Sprint 4)

| Indicador | Valor |
|---|---|
| Tablas | 31 (30 entidades + `mecanico_especialidad`) |
| Controladores / endpoints | 48 / 106 |
| Repositorios | 29 (55 consultas derivadas y 28 `@Query`) |
| Tests unitarios | 75 |