# Pruebas funcionales – Sprint 1 (TEC.4)

Responsable: Jordan Abad (frontend web) · Sprint 1: 05/10 – 16/10/2026

Este documento tiene dos partes:

1. **Pruebas de la API** con la colección de Postman `AutoSmart.postman_collection.json`.
2. **Pruebas de la web** (panel y páginas públicas), paso a paso, para marcar el resultado.

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

Cada ejecución crea un cliente, un mecánico, un servicio y un vehículo nuevos (con correo, nombre y placa únicos), así que se puede correr varias veces.

> Las peticiones de endpoints que el backend todavía no tiene responderán **404** y saldrán en rojo. Es lo esperado hasta que se implementen.

### Resultado de la ejecución

| Fecha | Ejecutado por | Peticiones OK | Fallidas | Observaciones |
|---|---|---|---|---|
| | | / 28 | | |

---

## 2. Pruebas de la web

**Cómo ejecutar:** backend encendido, `npm run dev` en `usuario/frontend`, abrir `http://localhost:5173`.
Marca cada caso con ✅ (pasa) o ❌ (falla) y anota lo que viste.

> Las pantallas de **Usuarios**, **Servicios** y **Recuperar contraseña** usan datos de prueba mientras `VITE_DATOS_PRUEBA` no sea `false` en `.env`. Repite esos casos con el backend real cuando estén sus endpoints.

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
| W-17 | Buscar "lucia" | Aparece "Lucía Fernández" (sin importar tildes) | | |
| W-18 | Filtrar por "Mecánicos" | Solo aparecen mecánicos | | |
| W-19 | Cambiar de página | "Mostrando 9–16 de …" | | |
| W-20 | Desactivar un usuario | Pide confirmación; al aceptar queda "Inactivo" y aparece un aviso | | |
| W-21 | Activar un usuario inactivo | Cambia a "Activo" sin pedir confirmación | | |
| W-22 | Intentar desactivar tu propia cuenta | El switch está bloqueado | | |
| W-23 | "Nuevo mecánico" con campos vacíos | Muestra los errores de cada campo | | |
| W-24 | "Nuevo mecánico" con un correo ya registrado | "Ya existe una cuenta con ese correo." | | |
| W-25 | "Nuevo mecánico" con datos válidos | Muestra correo y contraseña temporal; "Copiar datos" funciona; aparece primero en la lista | | |

### Catálogo de servicios (HU-18, HU-19, HU-20)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-26 | "Nuevo servicio" vacío | Errores en nombre, precio y duración | | |
| W-27 | Precio `-5` y duración `5` | "El precio debe ser mayor que 0." y "Entre 10 y 600 minutos" | | |
| W-28 | Nombre de un servicio existente | "Ya existe un servicio con ese nombre." | | |
| W-29 | Crear un servicio válido | Aparece en la tabla con precio en soles y duración legible | | |
| W-30 | "Editar" sin cambiar nada | "Guardar cambios" está deshabilitado | | |
| W-31 | Editar precio y duración | La fila muestra los valores nuevos y un aviso | | |
| W-32 | Desactivar un servicio | Pide confirmación; queda "Inactivo"; el contador de activos baja | | |

### Recuperar contraseña (HU-04)

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-33 | En el login, pulsar "¿Olvidaste tu contraseña?" | Abre `/recuperar` | | |
| W-34 | Enviar un correo válido | Pasa a "Crea una nueva contraseña" con el correo y el tiempo restante | | |
| W-35 | Escribir letras en el código | Solo se aceptan números (6 dígitos) | | |
| W-36 | Código incorrecto | "El código no es válido o ya venció." | | |
| W-37 | Código correcto (`123456` en modo prueba) y contraseña válida | "¡Contraseña actualizada!" y botón a iniciar sesión | | |
| W-38 | Pulsar "Reenviar código" | Espera 60 s antes de habilitarse; luego avisa "Te enviamos un código nuevo." | | |

### Diseño adaptable

| ID | Pasos | Resultado esperado | Resultado | Observaciones |
|---|---|---|---|---|
| W-39 | Ver todas las pantallas a 390 px de ancho (F12 → modo dispositivo) | Sin desplazamiento horizontal; las tablas se ven como tarjetas | | |

---

## Defectos encontrados

| ID | Caso | Descripción | Severidad | Responsable | Estado |
|---|---|---|---|---|---|
| | | | | | |
