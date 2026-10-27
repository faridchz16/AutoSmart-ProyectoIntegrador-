# Pruebas funcionales – Sprints 1 y 2 (TEC.4, TEC2.5)

Responsable: Jordan Abad (frontend web)
- Sprint 1 (TEC.4): 05/10 – 16/10/2026 → carpetas 1 a 5 de Postman y casos W-01 a W-39.
- Sprint 2 (TEC2.5): 19/10 – 30/10/2026 → carpetas 6 a 12 de Postman y casos W-40 a W-92.

Este documento tiene tres partes:

1. **Pruebas de la API** con la colección de Postman `AutoSmart.postman_collection.json` (Sprints 1 y 2).
2. **Pruebas de la web – Sprint 1** (panel del administrador y páginas públicas), paso a paso, para marcar el resultado.
3. **Pruebas de la web – Sprint 2** (panel del mecánico, reservas y horario del taller).

---

## 1. Pruebas de la API (Postman)

### Preparación

1. Enciende **MySQL** en XAMPP y el **backend** (puerto 8081).
2. En Postman: **Import** → elige `docs/pruebas/AutoSmart.postman_collection.json`.
3. Abre la colección → pestaña **Variables** y revisa:
   - `correoAdmin` y `passwordAdmin`: una cuenta con rol **ADMIN** en la base de datos.
   - `baseUrl`: `http://localhost:8081` (cámbiala si el backend usa otro puerto).
4. Pulsa **Run** (Collection Runner) y ejecuta todo **en orden**: la carpeta *1. Autenticación* guarda los tokens que usan las demás.

### Qué cubre

| Carpeta | Historias | Qué verifica |
|---|---|---|
| 1. Autenticación | HU-01, HU-02, HU-04, HU-12 | Registro, contraseña débil, correo repetido, login correcto e incorrecto, login de admin, recuperación |
| 2. Seguridad y roles | HU-02, HU-12 | Sin token → 401/403, cliente en el panel admin → 403, token alterado |
| 3. Admin – Usuarios | HU-15, HU-16, HU-17 | Listar por rol, buscar, crear mecánico, desactivar y bloquear el login, reactivar |
| 4. Admin – Servicios | HU-10, HU-18, HU-19, HU-20 | Crear, precio inválido, editar tarifa, deshabilitar y que no salga en el catálogo |
| 5. Vehículos | HU-06, HU-07 | Registrar, placa inválida, placa repetida, listar mis vehículos |
| 6. Taller y feriados | T40.1 | Horario del taller (7 días, validaciones), feriados: crear, repetido, sin descripción, eliminar; solo admin |
| 7. Mecánicos – perfil, horarios y bloqueos | HU-15, T13–T14, T21.1, T22.1 | Crear y entrar como mecánico, perfil (años y especialidades), horario que se cruza o con fin antes del inicio, fechas bloqueadas (pasada, repetida, eliminar), un cliente no entra al panel |
| 8. Catálogo y turnos libres | T37.1, T24.1, T25.1, TEC2.4 | Servicio con categoría y especialidad, categoría inválida, `GET /admin/services`, filtro por categoría, mecánicos por especialidad, turnos libres y fecha pasada |
| 9. Reservas del cliente | T26.1, T30.1, T31.1, T33.1 | Apartar turno 10 min, reservar (ticket `AS-AAAA-NNNNN`), turno ocupado, mis citas, reprogramar y reprogramar al mismo turno |
| 10. Citas del mecánico | T23.1, T35.1, T36.1, T32.1, T22.1 | Agenda y rango inválido, detalle (propio y ajeno), aceptar, aceptar dos veces, no bloquear un día con citas confirmadas, rechazar sin y con motivo, cancelar y cancelar dos veces |
| 11. Admin – reservas y reasignación | T38.1, T39.1 | Lista paginada con filtros (mecánico, estado, placa), estado inválido, mecánicos libres, reasignar al mismo, reasignar a otro (vuelve a PENDIENTE), no reasignar una rechazada |
| 12. Limpieza | – | Cancela la cita que quedó activa y desactiva el servicio y los mecánicos de prueba |

Cada ejecución crea un cliente, un mecánico, un servicio y un vehículo nuevos (con correo, nombre y placa únicos), así que se puede correr varias veces.

**Sprint 2:** la carpeta 6 busca solo el primer día, desde 3 días adelante, en que el taller abre al menos 4 horas y no es feriado. En ese día los mecánicos de prueba A y B atienden en el horario del taller, y ahí se reservan 3 citas de 1 hora. El feriado de prueba se registra en el año **2099**, para no afectar reservas reales. Al final, la carpeta 12 deja todo cancelado o desactivado.

> La colección del Sprint 2 se ejecutó con newman (la versión de consola de Postman) contra un servidor que imita las reglas del backend: **70 peticiones y 129 verificaciones, sin fallas**. Falta registrar abajo la ejecución contra el backend real.

> Las peticiones de endpoints que el backend todavía no tiene responderán **404** y saldrán en rojo. Es lo esperado hasta que se implementen.

### Resultado de la ejecución

| Fecha | Ejecutado por | Peticiones OK | Fallidas | Observaciones |
|---|---|---|---|---|
| | | / 98 | | |

---

## 2. Pruebas de la web – Sprint 1

**Cómo ejecutar:** backend encendido, `npm run dev` en `usuario/frontend`, abrir `http://localhost:5173`.
Marca cada caso con ✅ (pasa) o ❌ (falla) y anota lo que viste.

> Por defecto la web usa el **backend real** (rama `main`). Para una demo sin backend, pon `VITE_DATOS_PRUEBA=true` en `.env` y reinicia `npm run dev`: el panel usa datos de ejemplo y el código de recuperación es `123456`.
>
> Con el backend real, el código de recuperación **no llega por correo**: aparece en la consola del backend (IntelliJ), en una línea `[RECUPERACION] Para ... codigo XXXXXX`.

### Página de inicio

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-01 | Abrir `/` | Se ve la portada, los servicios y el asistente sin errores | | |
| W-02 | Abrir el asistente y preguntar "¿Cada cuánto cambio el aceite?" | Responde con una recomendación orientativa | | |
| W-03 | Pulsar "Iniciar sesión" y "Crear cuenta" | Llevan a `/login` y `/registro` | | |

### Registro e inicio de sesión (HU-01, HU-02)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-04 | En `/registro`, enviar el formulario vacío | "Completa todos los campos." | | |
| W-05 | Registrar con contraseña `abc12345` | Pide mínimo 8 caracteres, una mayúscula y un número | | |
| W-06 | Registrar con contraseñas distintas | "Las contraseñas no coinciden." | | |
| W-07 | Registrar con datos válidos | Pantalla "¡Cuenta creada!" y botón a iniciar sesión | | |
| W-08 | Registrar el mismo correo otra vez | Mensaje de error del servidor | | |
| W-09 | Login con contraseña incorrecta | Mensaje de error, no entra | | |
| W-10 | Login de un cliente | Pantalla "¡Bienvenido!" con su rol | | |
| W-11 | Login del administrador | Entra directo a `/admin` | | |

### Rutas protegidas y cierre de sesión (HU-03, HU-12)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-12 | Sin sesión, abrir `/admin/servicios` | Redirige a `/login`; al ingresar como admin vuelve a Servicios | | |
| W-13 | Con sesión de cliente, abrir `/admin` | Pantalla "No tienes acceso" (403) | | |
| W-14 | Abrir `/pagina-que-no-existe` | Pantalla 404 | | |
| W-15 | En el panel, pulsar "Cerrar sesión" y luego "Atrás" en el navegador | Vuelve a `/login` y no deja regresar al panel | | |
| W-16 | Panel en celular (o ventana angosta): abrir el menú ☰ | El menú se abre y se cierra al elegir una opción o con Esc | | |

### Gestión de usuarios (HU-15, HU-16, HU-17)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-17 | Buscar parte del nombre o correo de un usuario | Solo aparecen los usuarios que coinciden | | |
| W-18 | Filtrar por "Mecánicos" | Solo aparecen mecánicos | | |
| W-19 | Con más de 8 usuarios, cambiar de página | "Mostrando 9–… de …" | | |
| W-20 | Desactivar un usuario | Pide confirmación; al aceptar queda "Inactivo" y aparece un aviso | | |
| W-21 | Activar un usuario inactivo | Cambia a "Activo" sin pedir confirmación | | |
| W-22 | Intentar desactivar tu propia cuenta (fila "(tú)") | El switch está bloqueado | | |
| W-23 | "Nuevo mecánico" con campos vacíos | Errores en nombre y correo | | |
| W-24 | "Nuevo mecánico" con un correo ya registrado | "El correo ya se encuentra registrado" | | |
| W-25 | "Nuevo mecánico" con datos válidos | Muestra el correo y la contraseña temporal generada por el servidor; "Copiar datos" funciona | | |
| W-25b | Iniciar sesión con la cuenta de un usuario desactivado | "Tu cuenta esta desactivada. Contacta al administrador" | | |

### Catálogo de servicios (HU-18, HU-19, HU-20)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-26 | "Nuevo servicio" vacío | Errores en nombre, precio y duración | | |
| W-27 | Precio `-5` y duración `5` | "El precio debe ser mayor que 0." y "Entre 10 y 600 minutos" | | |
| W-28 | Nombre de un servicio existente | "Ya existe un servicio con ese nombre." | | |
| W-29 | Crear un servicio válido | Aparece en la tabla con precio en soles y duración legible | | |
| W-30 | "Editar" sin cambiar nada | "Guardar cambios" está deshabilitado | | |
| W-31 | Editar precio y duración | La fila muestra los valores nuevos y un aviso | | |
| W-32 | Desactivar un servicio | Pide confirmación; queda "Inactivo"; el contador de activos baja. Al recargar sigue en la tabla como inactivo (desde TEC2.4 existe `GET /admin/services`) | | |

### Recuperar contraseña (HU-04)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-33 | En el login, pulsar "¿Olvidaste tu contraseña?" | Abre `/recuperar` | | |
| W-34 | Enviar un correo válido | Pasa a "Crea una nueva contraseña" con el correo y el tiempo restante | | |
| W-35 | Escribir letras en el código | Solo se aceptan números (6 dígitos) | | |
| W-36 | Código incorrecto | "El código no es válido o ya venció." | | |
| W-37 | Código correcto (de la consola del backend, o `123456` en modo prueba) y contraseña válida | "¡Contraseña actualizada!"; luego el login funciona con la nueva contraseña | | |
| W-38 | Pulsar "Reenviar código" | Espera 60 s antes de habilitarse; luego avisa "Te enviamos un código nuevo." | | |

### Diseño adaptable

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-39 | Ver todas las pantallas a 390 px de ancho (F12 → modo dispositivo) | Sin desplazamiento horizontal; las tablas se ven como tarjetas | | |

---

## 3. Pruebas de la web – Sprint 2 (TEC2.5)

**Cómo ejecutar:** igual que en el Sprint 1. Para el panel del mecánico se necesita una cuenta con rol **MECANICO**: créala desde *Usuarios → Nuevo mecánico* (W-25) e inicia sesión con la contraseña temporal. Para ver citas con el backend real, un cliente debe reservar desde la app (o con la carpeta 9 de Postman); sin reservas, la agenda y las citas de hoy salen vacías, y eso es correcto.

### Panel del mecánico (TEC2.1)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-40 | Iniciar sesión con una cuenta de mecánico | Entra directo a `/mecanico`, con el menú "Mi taller" | | |
| W-41 | Con sesión de mecánico, abrir `/admin` | Pantalla "No tienes acceso" (403) | | |
| W-42 | Con sesión de administrador, abrir `/mecanico/agenda` | Pantalla "No tienes acceso" (403) | | |
| W-43 | En Inicio, pulsar cada tarjeta | Abre la sección correspondiente | | |

### Perfil profesional (T13.2, T14.1)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-44 | Abrir "Mi perfil" con un mecánico nuevo | Aviso amarillo "Aún no registras especialidades"; 0 años | | |
| W-45 | Escribir `75` en años | "Debe estar entre 0 y 60." y "Guardar perfil" deshabilitado | | |
| W-46 | Agregar "Frenos" con Enter y "Motor" desde las sugerencias | Aparecen como etiquetas; la vista previa se actualiza | | |
| W-47 | Agregar "FRENOS" otra vez | "«Frenos» ya está en tu lista." | | |
| W-48 | Guardar y recargar la página (F5) | Los años y las especialidades siguen ahí | | |
| W-49 | Cambiar algo y pulsar "Descartar cambios" | Vuelve a lo guardado | | |

### Mis horarios (T21.2)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-50 | Agregar lunes 08:00–12:00 | Aparece en la tarjeta del lunes con su duración | | |
| W-51 | Agregar lunes 11:00–13:00 | Avisa que se cruza con otro rango y no lo guarda | | |
| W-52 | Agregar un rango con fin antes del inicio | Error en el formulario | | |
| W-53 | Editar y luego eliminar un rango | Se actualiza; al eliminar pide confirmación | | |

### Fechas bloqueadas (T22.2)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-54 | Abrir el calendario | Los días con horario se ven resaltados; no deja ir a meses anteriores | | |
| W-55 | Bloquear un día futuro con motivo | Queda en rojo con candado y aparece en "Próximas fechas bloqueadas" | | |
| W-56 | Bloquear un día con una cita confirmada | "No puedes bloquear una fecha con citas confirmadas" | | |
| W-57 | Desbloquear el día | Vuelve a la normalidad y sale de la lista | | |

### Agenda semanal (T23.2)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-58 | Abrir "Agenda semanal" | Semana de lunes a domingo con "Esta semana" y el día de hoy resaltado | | |
| W-59 | Pulsar ‹ y › y luego "Hoy" | Cambia de semana y vuelve a la actual | | |
| W-60 | Marcar "Mostrar canceladas y rechazadas" | Aparecen tachadas | | |
| W-61 | Ver un día bloqueado y un día sin horario | "Día bloqueado" y "Sin horario" | | |

### Citas de hoy (T34.1)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-62 | Abrir "Citas de hoy" con citas en el día | Lista por hora con la línea roja "Ahora" y la tarjeta "En curso" o "Siguiente cita" | | |
| W-63 | Sin citas en el día | "No tienes citas para hoy" (o "Hoy es un día bloqueado" / "Hoy no atiendes") | | |
| W-64 | Pulsar "Actualizar" | Cambia la hora de "Actualizado a las …" | | |
| W-65 | Apagar el backend y pulsar "Actualizar" | Sigue mostrando las citas y avisa "No se pudo actualizar" | | |

### Detalle de la cita y aceptar / rechazar (T35.2, T36.2)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-66 | Clic en una cita (agenda o citas de hoy) | Ventana con cliente, teléfono, vehículo, kilometraje y precio | | |
| W-67 | En una cita con teléfono de 9 dígitos | Botones "Llamar" y "WhatsApp" | | |
| W-68 | Abrir una cita confirmada | Solo el botón "Cerrar" | | |
| W-69 | Aceptar una cita pendiente | Se cierra la ventana, la cita queda "Confirmada" y sale un aviso | | |
| W-70 | Rechazar sin escribir motivo | "Rechazar cita" está deshabilitado | | |
| W-71 | Rechazar con un motivo frecuente | La cita queda "Rechazada" y sale un aviso | | |
| W-72 | Abrir una cita pendiente, cancelarla desde la app del cliente y luego pulsar "Aceptar" | "Esta cita ya no está pendiente…" y se actualiza su estado | | |

### Categorías de servicios (T37.2)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-73 | "Nuevo servicio" sin elegir categoría | "Elige la categoría del servicio." | | |
| W-74 | Crear un servicio "Correctivo" | Etiqueta naranja "Correctivo" en la tabla | | |
| W-75 | Filtrar por "Correctivo" y luego "Limpiar filtros" | Solo correctivos; luego todos | | |

### Reservas del taller (T38.2)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-76 | Abrir "Reservas" | Reservas de esta semana con total, ticket, cliente, placa, mecánico, precio y estado | | |
| W-77 | Cambiar a "Hoy", "Este mes" y "Rango" | El encabezado muestra el periodo y el total cambia | | |
| W-78 | "Rango" con inicio después del fin | Aviso y no se consulta | | |
| W-79 | Filtrar por estado y por mecánico | Solo las reservas que cumplen ambos filtros | | |
| W-80 | Buscar por placa o ticket | Solo las que coinciden | | |
| W-81 | Con más de 10 reservas, cambiar de página | "Mostrando 11–… de …" | | |

### Reasignar una reserva (T39.2)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-82 | Ver una reserva cancelada, rechazada o pasada | No tiene botón "Reasignar" | | |
| W-83 | "Reasignar" en una reserva pendiente | Ventana con la cita, el mecánico actual y los mecánicos libres con sus especialidades | | |
| W-84 | Elegir un mecánico y confirmar | La fila cambia de mecánico, queda "Pendiente" y sale un aviso | | |
| W-85 | Sin otros mecánicos libres a esa hora | "No hay otros mecánicos libres en ese horario" | | |

### Horario del taller y feriados (T40.2)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-86 | Abrir "Horario del taller" | 7 días con su estado, horas y total semanal | | |
| W-87 | Poner un cierre antes de la apertura | Mensaje en el día y "Guardar horario" deshabilitado | | |
| W-88 | "Copiar el lunes a martes–viernes", guardar y recargar | Los días copiados conservan el horario | | |
| W-89 | Agregar un feriado con fecha y descripción | Aparece en la lista del año y sale un aviso | | |
| W-90 | Agregar un feriado en una fecha que ya tiene uno | "Ya registraste un feriado en esa fecha." | | |
| W-91 | Eliminar un feriado | Pide confirmación y sale de la lista | | |

### Diseño adaptable (Sprint 2)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-92 | Ver todas las pantallas nuevas a 390 px de ancho | Sin desplazamiento horizontal; las reservas se ven como tarjetas y las ventanas caben en la pantalla | | |

---

## Pendientes del backend detectados

| Pendiente | Efecto en la web | Responsable |
|---|---|---|
| ~~Falta `GET /admin/services` (activos e inactivos)~~ | **Resuelto en el Sprint 2 (TEC2.4).** El panel ya muestra los servicios inactivos y se pueden reactivar | Farid |
| `GET /admin/users` no envía la fecha de registro (`creadoEn`) | La columna "Registro" no se muestra | Farid (opcional) |
| `GET /appointments/{id}` no envía el motivo de rechazo (`motivoRechazo`) | El detalle de una cita rechazada no muestra el motivo, aunque el backend lo guarda | Farid (opcional) |
| El administrador no tiene un detalle de cita (teléfono, vehículo) | La tabla de reservas muestra solo los datos de la lista | Farid (opcional) |

## Defectos encontrados

| ID | Caso | Descripción | Severidad | Responsable | Estado |
|---|---|---|---|---|---|
| | | | | | |
