# AutoSmart · Frontend web

Aplicación web de AutoSmart: página pública con el asistente (chatbot) y los paneles del **cliente**, el **mecánico** y el **administrador**.

- React 19 + Vite 8 + React Router 7 (sin librerías de interfaz: estilos propios en CSS).
- Se conecta al backend Spring Boot (`usuario/backend`, puerto 8081) con JWT.
- Responsable: Jordan Abad (frontend web), equipo C24-4-B.

> Para instalar el sistema completo (base de datos, backend, web y app), sigue [`docs/INSTALACION.md`](../../docs/INSTALACION.md).

---

## 1. Requisitos

| Programa | Versión | Para qué |
|---|---|---|
| Node.js | 20.19 o superior (recomendado 22 LTS) | Ejecutar Vite y npm |
| Backend AutoSmart | rama `main`, en `http://localhost:8081` | Datos reales e inicio de sesión |
| Navegador | Chrome, Edge o Firefox actualizados | Usar la web |

Verifica Node con `node -v`.

## 2. Instalación

Desde la raíz del repositorio:

```bash
cd usuario/frontend
npm install
```

Crea el archivo `.env` copiando el ejemplo (en Windows: `copy .env.example .env`):

```bash
cp .env.example .env
```

| Variable | Valor | Qué hace |
|---|---|---|
| `VITE_API_URL` | vacío (recomendado en desarrollo) | Vite reenvía las llamadas al backend (ver la sección 4). En producción va la URL completa del backend, p. ej. `https://api.autosmart.pe` |
| `VITE_DATOS_PRUEBA` | `false` | `false` usa el backend real. `true` usa datos de ejemplo (ver la sección 5) |

> Cada vez que cambies `.env` o `vite.config.js`, detén y vuelve a ejecutar `npm run dev`.

## 3. Ejecutar

```bash
npm run dev
```

Abre `http://localhost:5173`. Si el puerto está ocupado, Vite usa el siguiente (5174, 5175…) y lo muestra en la consola. Para fijar otro: `npm run dev -- --port 3000`.

| Comando | Qué hace |
|---|---|
| `npm run dev` | Servidor de desarrollo con recarga automática |
| `npm run build` | Genera la versión de producción en `dist/` |
| `npm run preview` | Sirve `dist/` para revisarla antes de publicar |
| `npm run lint` | Revisa el código con ESLint |

## 4. Conexión con el backend

En desarrollo, `vite.config.js` reenvía estas rutas a `http://localhost:8081`, así no hay problemas de CORS:

`/auth` · `/users` · `/vehicles` · `/services` · `/mechanics` · `/appointments` · `/quotes` · `/work-orders` · `/photos` · `/materials` · `/payments` · `/payment-methods` · `/chatbot` · `/admin`

`/admin` es a la vez una ruta de la web y de la API: si el navegador pide una página (HTML) se sirve React; si es una llamada de datos, va al backend.

Si el backend corre en otro puerto, cambia la constante `BACKEND` de `vite.config.js`.

El token JWT se guarda en el navegador al iniciar sesión y se envía en cada llamada. Si vence (60 minutos), la web cierra la sesión y vuelve a `/login`.

## 5. Modo de datos de prueba

Con `VITE_DATOS_PRUEBA=true` los paneles usan datos de ejemplo de `src/services/datosPrueba.js`. Sirve para mostrar las pantallas cuando la base de datos está vacía. En la barra superior aparece la etiqueta **Datos de prueba**.

- Los cambios (reservar, pagar, calificar…) se guardan solo en memoria y se pierden al recargar la página.
- **El inicio de sesión siempre usa el backend**: enciéndelo para entrar con una cuenta real del rol que quieras ver.
- El código de recuperación de contraseña es `123456`.

## 6. Pantallas por rol

| Rol | Ruta de inicio | Pantallas |
|---|---|---|
| Público | `/` | Inicio con servicios y asistente, `/login`, `/registro`, `/recuperar` |
| Cliente | `/cliente` | Inicio, Reservar cita, Mis citas (seguimiento, presupuesto, pago, detalle y calificación), Mis vehículos (historial por vehículo), Mi perfil |
| Mecánico | `/mecanico` | Agenda semanal, Citas de hoy, Orden de trabajo (checklist, deficiencias con fotos, diagnóstico, presupuesto, tareas, repuestos y finalización), Mis horarios, Fechas bloqueadas, Mi historial, Mis calificaciones, Mi perfil |
| Administrador | `/admin` | Resumen, Indicadores, Reservas, Pagos (cobro en efectivo), Historial por placa, Calificaciones, Derivaciones del chatbot, Preguntas del chatbot, Usuarios, Servicios, Inventario, Solicitudes de repuestos, Horario del taller |

Cada panel está protegido por rol: sin sesión lleva a `/login` y con otro rol muestra "No tienes acceso". Después de iniciar sesión, cada usuario entra a su panel.

### Pagos en el panel del cliente (modo de pruebas)

El backend usa una pasarela de pruebas (sandbox) y no se hacen cobros reales:

| Método | Dato de prueba | Resultado |
|---|---|---|
| Tarjeta | `4242 4242 4242 4242` (Visa) o `5555 5555 5555 4444` (Mastercard) | Aprobado |
| Tarjeta | `4000 0000 0000 0002` / `…9995` / `…0069` | Rechazada / fondos insuficientes / vencida |
| Yape / Plin | Código de aprobación `123456` | Aprobado (otro código se rechaza) |
| Efectivo | — | Se paga en el taller; lo registra el administrador en **Pagos** |

Cualquier fecha de vencimiento futura y cualquier CVV de 3 dígitos sirven. El número de tarjeta no se envía al servidor: la web manda el token de prueba que le corresponde.

## 7. Estructura del código

```
src/
├── App.jsx              Rutas de la aplicación y protección por rol
├── components/          Piezas compartidas (ChatBot, Estrellas, RutaProtegida, admin/Modal, Aviso…)
├── layouts/             Estructura de cada panel (menú lateral, encabezado) y sus enlaces
├── pages/
│   ├── admin/           Pantallas del administrador
│   ├── mecanico/        Pantallas del mecánico
│   └── cliente/         Pantallas del cliente
├── services/            Llamadas a la API (una por módulo) y datosPrueba.js
└── utils/fechas.js      Fechas en hora de Lima
```

Cada archivo de `services/` documenta al inicio los endpoints que usa, con su cuerpo y sus respuestas.

## 8. Problemas frecuentes

| Problema | Solución |
|---|---|
| "No hay conexión con el servidor" o "¿Está encendido el backend?" | Enciende MySQL y el backend (`http://localhost:8081`) |
| Una pantalla queda vacía o sale un error de JSON | Reinicia `npm run dev`: los cambios de `vite.config.js` se leen solo al arrancar |
| Al recargar `/admin/...` sale un JSON del backend | Usa `npm run dev` (no abras `index.html` directo); el proxy de `/admin` necesita Vite |
| "Tu sesión venció" | El token dura 60 minutos; vuelve a iniciar sesión |
| Las fotos de las deficiencias no cargan | Revisa que el backend tenga acceso a su carpeta de fotos (`FOTOS_DIR`) |
| `npm install` falla | Actualiza Node a 20.19+ y borra `node_modules` antes de reintentar |

## 9. Pruebas

- Casos funcionales de los Sprints 1 y 2: [`docs/pruebas/PRUEBAS_FUNCIONALES.md`](../../docs/pruebas/PRUEBAS_FUNCIONALES.md).
- Pruebas de aceptación de punta a punta del Sprint 4: [`docs/pruebas/PRUEBAS_ACEPTACION.md`](../../docs/pruebas/PRUEBAS_ACEPTACION.md).
