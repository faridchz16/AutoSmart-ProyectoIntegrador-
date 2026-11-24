# Pruebas de aceptación de punta a punta – Sprint 4 (TEC4.5)

| Campo | Detalle |
|---|---|
| Proyecto | AutoSmart · Construcción y Pruebas de Software · C24-4-B · 2026-2 |
| Sprint | 4 (16/11 – 27/11/2026): pagos, calificaciones, chatbot y supervisión |
| Responsable | Jordan Abad (frontend web), con el Product Owner |
| Participan | Farid Chávez (backend) · Yony Ortiz (app móvil) |
| Versión probada | rama `main`, commit: `________` |
| Fecha de la sesión | ____/11/2026 |

Este documento sirve para que el **Product Owner** recorra el producto completo y decida si acepta las historias del Sprint 4 (HU-61 a HU-80). Cada escenario sigue el camino real de un usuario, de punta a punta: reservar, atender, presupuestar, pagar, calificar y supervisar.

Cómo marcar: ✅ pasa · ❌ falla (anotar el defecto en la sección 6) · ➖ no aplica.

---

## 1. Alcance

| Perfil | Historias | Dónde se prueban |
|---|---|---|
| Usuario (cliente) | HU-61 a HU-66, HU-75 a HU-79 | **App móvil** (Yony) y **panel web del cliente** (`/cliente`) |
| Mecánico | HU-67, HU-68 | Panel web del mecánico (`/mecanico`) |
| Administrador | HU-69 a HU-74, HU-79 (bandeja), HU-80 | Panel web del administrador (`/admin`) |

Canales en las tablas: **W** = web · **A** = app móvil. Si un paso dice **W/A**, se puede hacer en cualquiera de los dos; se recomienda alternarlos entre escenarios para cubrir ambos.

Fuera de alcance: la colección Postman del Sprint 4 (TEC4.2) y las pruebas funcionales de la app (TEC4.3), que se documentan aparte.

## 2. Preparación

### 2.1 Entorno

1. Sistema instalado según [`docs/INSTALACION.md`](../INSTALACION.md): MySQL, backend en `http://localhost:8081`, web en `http://localhost:5173` con `VITE_DATOS_PRUEBA=false` y la app apuntando al mismo backend.
2. Datos mínimos de la sección 8 de la guía de instalación (horario del taller, servicios, mecánico con horario y especialidad, repuestos con stock).

### 2.2 Cuentas y datos de la sesión

| Dato | Valor usado en la sesión |
|---|---|
| Administrador | correo: `______________________` |
| Mecánico | correo: `______________________` · especialidades: Frenos, Mecánica general |
| Cliente A | correo: `______________________` · teléfono registrado |
| Cliente B (para pruebas de seguridad) | correo: `______________________` |
| Vehículo del cliente A | placa `ABC-123` (Toyota Yaris 2019) |
| Servicio 1 | Revisión de frenos · S/ 80.00 · 60 min · especialidad Frenos |
| Servicio 2 | Cambio de aceite y filtro · S/ 120.00 · 45 min |
| Repuesto con stock | código `__________` · stock inicial `____` |

### 2.3 Datos de pago (pasarela sandbox, sin cobros reales)

| Método | Dato | Resultado esperado |
|---|---|---|
| Tarjeta | `4242 4242 4242 4242`, vence 12/30, CVV 123 | Aprobado |
| Tarjeta | `4000 0000 0000 0002` | Rechazado: "La tarjeta fue rechazada" |
| Tarjeta | `4000 0000 0000 9995` | Rechazado: fondos insuficientes |
| Yape / Plin | código de aprobación `123456` | Aprobado |
| Yape / Plin | cualquier otro código | Rechazado: código inválido |
| Efectivo | — | No se paga en línea; lo registra el administrador |

---

## 3. Escenarios de punta a punta

### E2E-01 · Servicio completo con presupuesto aprobado, pago con tarjeta y calificación

Historias: HU-61, HU-62, HU-63, HU-64, HU-65, HU-66, HU-67, HU-68, HU-70, HU-73

| # | Rol | Canal | Acción | Resultado esperado | ✅/❌ |
|---|---|---|---|---|---|
| 1 | Cliente A | W/A | Reservar cita → Revisión de frenos | Solo aparecen mecánicos con la especialidad Frenos, ordenados de mejor a peor calificado, con estrellas y número de reseñas (HU-66) | |
| 2 | Cliente A | W/A | Elegir mecánico, día y hora; elegir `ABC-123` y confirmar | Ticket `AS-AAAA-NNNNN` en estado **Por confirmar**; el turno se separa 10 min mientras confirma | |
| 3 | Mecánico | W | Agenda semanal / Citas de hoy → abrir la cita → **Aceptar cita** | La cita pasa a **Confirmada**; el cliente lo ve en Mis citas | |
| 4 | Mecánico | W | **Iniciar atención** → Orden de trabajo | Se crea la orden; el seguimiento del cliente marca "Vehículo recibido en el taller" | |
| 5 | Mecánico | W | Llenar el checklist, registrar una deficiencia **crítica** con foto y el diagnóstico | Quedan guardados; el diagnóstico es obligatorio para seguir | |
| 6 | Mecánico | W | Presupuesto: 1 repuesto + 1 mano de obra → enviar | La cita pasa a **Presupuesto por aprobar** | |
| 7 | Cliente A | W/A | Abrir el seguimiento de la cita | Ve la línea de tiempo, la deficiencia con su foto, el detalle del presupuesto y el total | |
| 8 | Cliente A | W/A | **Aprobar presupuesto** | Queda Aprobado; la cita vuelve a **En atención** | |
| 9 | Mecánico | W | Registrar tareas, marcarlas hechas, registrar el repuesto usado y **Completar mantenimiento** | La cita pasa a **Lista para pagar**; el stock del repuesto baja | |
| 10 | Cliente A | W/A | Abrir el pago de la cita | Resumen del costo: servicio (S/ 80.00) + adicionales aprobados = total; lista de repuestos usados (HU-61) | |
| 11 | Cliente A | W/A | Ver métodos de pago | Tarjeta, Yape/Plin y Efectivo en el taller (solo los habilitados) (HU-62) | |
| 12 | Cliente A | W/A | Pagar con `4000 0000 0000 0002` | Mensaje de rechazo con el motivo; no se cobra; la cita sigue **Lista para pagar** y puede reintentar (HU-63) | |
| 13 | Cliente A | W/A | Pagar con `4242 4242 4242 4242` | Pago **aprobado** con código `PAG-AAAA-NNNNN`; la cita pasa a **Pagada** (HU-63) | |
| 14 | Cliente A | W/A | **Descargar comprobante** | Se descarga un PDF con código, fecha, detalle y total (HU-64) | |
| 15 | Cliente A | W/A | Calificar al mecánico: 5 estrellas y un comentario | Se registra y se muestra la calificación (HU-65) | |
| 16 | Cliente A | W/A | Intentar calificar el mismo servicio otra vez | No lo permite: "Ya calificaste este servicio" (una vez por servicio) | |
| 17 | Mecánico | W | Mis calificaciones | Aparece la nueva calificación con su comentario y el promedio actualizado (HU-67) | |
| 18 | Mecánico | W | Mi historial, filtrando por las fechas de hoy | Aparece la orden finalizada con su duración (HU-68) | |
| 19 | Administrador | W | Pagos, filtro de hoy | Aparece el pago aprobado y el rechazado; el total del periodo suma solo los aprobados (HU-70) | |
| 20 | Administrador | W | Indicadores del mes actual | Citas por estado, ingresos, tiempo promedio y calificación promedio incluyen este servicio (HU-73) | |

### E2E-02 · Pago en efectivo en el taller

Historias: HU-62, HU-64, HU-69, HU-70

| # | Rol | Canal | Acción | Resultado esperado | ✅/❌ |
|---|---|---|---|---|---|
| 1 | Cliente A, Mecánico | W/A | Repetir los pasos 1 a 4 y 9 de E2E-01 con el servicio Cambio de aceite (sin presupuesto) | La cita queda **Lista para pagar** por S/ 120.00 | |
| 2 | Cliente A | W/A | En el pago, elegir **Efectivo en el taller** | Indica que se paga en caja; no hay botón de pago en línea | |
| 3 | Administrador | W | Pagos → cobro en efectivo → buscar la cita por ticket, cliente o placa | Aparece entre las citas por cobrar con un monto de referencia | |
| 4 | Administrador | W | Registrar un monto **menor** al total | No lo registra: indica el total real y ofrece usarlo | |
| 5 | Administrador | W | Registrar el cobro con el total | Pago aprobado con método Efectivo, el vuelto y el usuario que cobró; la cita pasa a **Pagada** (HU-69) | |
| 6 | Administrador | W | Buscar otra vez la misma cita para cobrarla | Ya no aparece entre las citas por cobrar | |
| 7 | Cliente A | W/A | Abrir la cita | Figura **Pagada** y puede descargar su comprobante (HU-64) | |

### E2E-03 · Presupuesto rechazado y pago con Yape/Plin

Historias: HU-61, HU-63

| # | Rol | Canal | Acción | Resultado esperado | ✅/❌ |
|---|---|---|---|---|---|
| 1 | Cliente A, Mecánico | W/A | Nueva cita hasta que el mecánico envíe un presupuesto | La cita queda **Presupuesto por aprobar** | |
| 2 | Cliente A | W/A | **Rechazar** el presupuesto | Queda Rechazado; la cita vuelve a **En atención** | |
| 3 | Mecánico | W | Completar el mantenimiento | La cita queda **Lista para pagar** | |
| 4 | Cliente A | W/A | Abrir el pago | El total es **solo el precio del servicio**: el presupuesto rechazado no se suma | |
| 5 | Cliente A | W/A | Yape/Plin con un código distinto de `123456` | Rechazado con motivo; puede reintentar | |
| 6 | Cliente A | W/A | Yape/Plin con `123456` | Aprobado; la cita pasa a **Pagada** | |

### E2E-04 · Chatbot y derivación a un asesor

Historias: HU-75, HU-76, HU-77, HU-78, HU-79, HU-80

| # | Rol | Canal | Acción | Resultado esperado | ✅/❌ |
|---|---|---|---|---|---|
| 1 | Administrador | W | Preguntas del chatbot → crear "¿Tienen estacionamiento?" con palabras clave `estacionamiento, parqueo` | Queda activa; la prueba en vivo del mismo panel responde con ella (HU-80) | |
| 2 | Visitante | W | Asistente de la página de inicio: "¿tienen parqueo?" | Responde con la pregunta recién creada (HU-75) | |
| 3 | Visitante | W/A | Preguntar por horarios y precios | Responde con el horario del taller y el catálogo; ofrece opciones rápidas (HU-75) | |
| 4 | Administrador | W | Editar la respuesta y luego **desactivar** la pregunta | El chatbot usa la respuesta editada y, desactivada, deja de usarla (HU-80) | |
| 5 | Cliente A (con sesión) | W/A | "¿Cómo va mi auto?" | Muestra solo **sus** vehículos y el estado de su atención (HU-76) | |
| 6 | Visitante (sin sesión) | W/A | "¿Cómo va mi auto?" | Pide iniciar sesión; no muestra datos de nadie | |
| 7 | Cliente A | W/A | "¿Cuánto cuesta un cambio de aceite?" | Precio base marcado como **referencial** (HU-77) | |
| 8 | Cliente A | A | Agendar conversando: servicio, vehículo, fecha y turno | Muestra un resumen y **solo reserva al confirmar**; la cita aparece en Mis citas (HU-78) | |
| 9 | Cliente A | W/A | "Quiero hablar con un asesor" | Confirma que un asesor lo contactará (HU-79) | |
| 10 | Administrador | W | Derivaciones | Aparece la derivación pendiente con el resumen de la conversación y el contacto; el menú muestra el contador | |
| 11 | Administrador | W | **Marcar como atendida** con una nota | Pasa a Atendida con la nota, quién y cuándo; el contador baja | |

### E2E-05 · Supervisión del administrador

Historias: HU-70, HU-71, HU-72, HU-73, HU-74

| # | Rol | Canal | Acción | Resultado esperado | ✅/❌ |
|---|---|---|---|---|---|
| 1 | Administrador | W | Pagos: filtrar por rango de fechas, por método (Tarjeta) y por estado (Aprobado) | La tabla y el total respetan los tres filtros; un rango inválido muestra aviso (HU-70) | |
| 2 | Administrador | W | Exportar Pagos a **Excel** y a **PDF** con esos filtros | Se descargan los dos archivos y contienen solo lo filtrado (HU-74) | |
| 3 | Administrador | W | Calificaciones → **Ocultar** un comentario con motivo | El comentario queda oculto, la nota (estrellas) se mantiene y queda registrado quién y por qué (HU-71) | |
| 4 | Mecánico | W | Mis calificaciones | El comentario oculto ya no aparece; el promedio no cambia (HU-71) | |
| 5 | Administrador | W | Historial por placa → `ABC-123` | Lista todos los mantenimientos del vehículo con diagnóstico, mecánico y fechas (HU-72) | |
| 6 | Administrador | W | Historial por placa → una placa que no existe | Mensaje claro de que no hay un vehículo con esa placa | |
| 7 | Administrador | W | Indicadores: cambiar al mes anterior | Los indicadores y gráficos cambian de mes; compara con el mes previo (HU-73) | |
| 8 | Administrador | W | Exportar Indicadores y Reservas a Excel y PDF | Se descargan con el mes o los filtros elegidos (HU-74) | |

### E2E-06 · Reglas del negocio y seguridad

| # | Rol | Canal | Acción | Resultado esperado | ✅/❌ |
|---|---|---|---|---|---|
| 1 | Cliente A | W/A | Abrir el pago de una cita que aún está **En atención** | No permite pagar: "El servicio aún no termina" | |
| 2 | Cliente B | W | Abrir la URL del seguimiento o del pago de una cita del Cliente A | "No encontramos esta cita"; no ve datos ajenos | |
| 3 | Cliente A | W/A | Cancelar una cita que empieza en menos de 2 horas | No lo permite y explica la regla | |
| 4 | Cliente A | W/A | Cambiar de horario una cita que empieza en menos de 24 horas | No lo permite y explica la regla | |
| 5 | Cliente A | W | Abrir `/admin` y `/mecanico` | Pantalla "No tienes acceso" | |
| 6 | Mecánico | W | Abrir `/admin/pagos` | Pantalla "No tienes acceso" | |
| 7 | Sin sesión | W | Abrir `/cliente/citas` | Redirige a `/login` y, al entrar, vuelve a la pantalla pedida | |
| 8 | Cliente A | W | Pagar dos veces la misma cita (dos pestañas) | La segunda vez responde que la orden ya fue pagada | |

---

## 4. Criterios de aceptación por historia

Una historia se **acepta** cuando todos sus pasos en los escenarios pasan y no quedan defectos de severidad alta abiertos.

| HU | Criterio de aceptación (Dado / Cuando / Entonces) | Escenarios | Aceptada (Sí/No) |
|---|---|---|---|
| HU-61 | Dado un servicio finalizado, cuando el usuario abre el pago, entonces ve el precio del servicio, los adicionales aprobados, los repuestos usados y el total | E2E-01 (10), E2E-03 (4) | |
| HU-62 | Dado el pago de un servicio, cuando el usuario elige cómo pagar, entonces ve solo los métodos habilitados: tarjeta, Yape/Plin y efectivo en el taller | E2E-01 (11), E2E-02 (2) | |
| HU-63 | Dado un servicio por pagar, cuando el pago en línea se aprueba la cita pasa a Pagada; si se rechaza, se muestra el motivo y puede reintentar | E2E-01 (12–13), E2E-03 (5–6) | |
| HU-64 | Dado un pago aprobado, cuando el usuario descarga el comprobante, entonces obtiene un PDF con código, fecha, detalle y total | E2E-01 (14), E2E-02 (7) | |
| HU-65 | Dado un servicio terminado, cuando el usuario califica con 1 a 5 estrellas y un comentario, entonces se guarda una sola vez por servicio | E2E-01 (15–16) | |
| HU-66 | Dado que el usuario reserva, cuando elige mecánico, entonces ve su promedio y número de reseñas, ordenados por calificación | E2E-01 (1) | |
| HU-67 | Dado un mecánico calificado, cuando abre Mis calificaciones, entonces ve sus notas y comentarios (sin los ocultos) y su promedio | E2E-01 (17), E2E-05 (4) | |
| HU-68 | Dado un mecánico con órdenes finalizadas, cuando filtra por fechas, entonces ve su historial de servicios | E2E-01 (18) | |
| HU-69 | Dado un servicio por pagar, cuando el administrador registra el cobro en efectivo, entonces queda el monto, la fecha y quién cobró, y la orden pasa a Pagada | E2E-02 (3–6) | |
| HU-70 | Dado el panel de pagos, cuando el administrador filtra por fecha, método y estado, entonces ve los pagos y el total del periodo | E2E-01 (19), E2E-05 (1) | |
| HU-71 | Dado un comentario ofensivo, cuando el administrador lo oculta, entonces deja de mostrarse sin borrar la nota y queda registrado | E2E-05 (3–4) | |
| HU-72 | Dada una placa, cuando el administrador la busca, entonces ve el historial de mantenimiento del vehículo | E2E-05 (5–6) | |
| HU-73 | Dado un mes, cuando el administrador abre Indicadores, entonces ve citas por estado, ingresos, tiempo promedio y satisfacción | E2E-01 (20), E2E-05 (7) | |
| HU-74 | Dados unos filtros, cuando el administrador exporta, entonces obtiene el reporte en Excel o PDF con esos filtros | E2E-05 (2, 8) | |
| HU-75 | Dada una duda frecuente, cuando el usuario la escribe en el chatbot, entonces responde con la FAQ, el catálogo o el horario | E2E-04 (2–3) | |
| HU-76 | Dado un usuario con sesión, cuando pregunta por su vehículo, entonces ve solo el estado de sus vehículos | E2E-04 (5–6) | |
| HU-77 | Dado un servicio, cuando el usuario pide una cotización, entonces recibe el precio marcado como referencial | E2E-04 (7) | |
| HU-78 | Dado un usuario con sesión, cuando agenda conversando, entonces la cita solo se crea tras su confirmación explícita | E2E-04 (8) | |
| HU-79 | Dado que el bot no resuelve, cuando el usuario pide un asesor, entonces se crea una derivación que el administrador atiende con una nota | E2E-04 (9–11) | |
| HU-80 | Dada la base de conocimiento, cuando el administrador crea, edita o desactiva una pregunta, entonces el chatbot usa la información vigente | E2E-04 (1, 4) | |

---

## 5. Panel web del cliente (complemento)

El cliente también puede hacer todo su recorrido desde la web (`/cliente`), además de la app. Comprobación rápida:

| ID | Pantalla | Qué verificar | ✅/❌ |
|---|---|---|---|
| C-01 | Inicio | Saludo, aviso si falta el teléfono, próxima cita y avisos de "presupuesto por aprobar" y "listo para pagar" | |
| C-02 | Mi perfil | Edita nombre y teléfono (7 a 15 dígitos); el correo no se puede cambiar | |
| C-03 | Mis vehículos | Registra (placa `ABC-123`), edita (la placa no cambia y el km no baja) y elimina con confirmación | |
| C-04 | Reservar cita | 4 pasos con resumen lateral y cuenta regresiva del turno separado; si se vence, pide elegir otra hora | |
| C-05 | Mis citas | Próximas e Historial; cancelar (2 h antes) y cambiar de horario (24 h antes) con los turnos del mismo mecánico | |
| C-06 | Seguimiento | Línea de tiempo, avance del trabajo y presupuesto con fotos; se actualiza solo cada 30 s | |
| C-07 | Pago | Detalle de la cuenta, tarjeta o Yape/Plin, rechazo con motivo y comprobante PDF | |
| C-08 | Detalle y calificación | Diagnóstico, revisión de 5 puntos, problemas, trabajos y calificación con estrellas | |
| C-09 | Historial del vehículo | Mantenimientos por año, enlaces al detalle y al comprobante, e impresión | |
| C-10 | Celular | Todas las pantallas anteriores se usan bien en un celular (menú ☰, sin desplazamiento horizontal) | |

---

## 6. Registro de defectos

| N.º | Escenario y paso | Descripción | Severidad (Alta/Media/Baja) | Responsable | Estado |
|---|---|---|---|---|---|
| 1 | | | | | |
| 2 | | | | | |
| 3 | | | | | |

Severidad **alta**: impide terminar el recorrido o muestra datos de otro usuario. **Media**: hay una forma de seguir, pero el resultado no es el esperado. **Baja**: textos, estilos u orden de la información.

## 7. Resumen de resultados

| Escenario | Pasos | Pasan | Fallan | Observaciones |
|---|---|---|---|---|
| E2E-01 Servicio completo con tarjeta | 20 | | | |
| E2E-02 Pago en efectivo | 7 | | | |
| E2E-03 Presupuesto rechazado y Yape/Plin | 6 | | | |
| E2E-04 Chatbot y asesor | 11 | | | |
| E2E-05 Supervisión del administrador | 8 | | | |
| E2E-06 Reglas y seguridad | 8 | | | |
| Panel web del cliente | 10 | | | |
| **Total** | **70** | | | |

## 8. Acta de aceptación

| Campo | Detalle |
|---|---|
| Product Owner | |
| Fecha | ____/11/2026 |
| Historias aceptadas | ____ de 20 |
| Decisión | ☐ Aceptado · ☐ Aceptado con observaciones · ☐ No aceptado |
| Observaciones | |

| Rol | Nombre | Firma |
|---|---|---|
| Product Owner | | |
| Frontend web | Jordan Abad | |
| Backend | Farid Chávez | |
| App móvil | Yony Ortiz | |
