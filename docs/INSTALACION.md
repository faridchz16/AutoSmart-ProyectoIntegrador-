# Instalación de AutoSmart

Guía para instalar y ejecutar el sistema completo en una computadora con Windows (también sirve en macOS y Linux cambiando los comandos de consola).

AutoSmart tiene cuatro partes que comparten la misma base de datos:

| Parte | Carpeta | Tecnología | Dirección local |
|---|---|---|---|
| Base de datos | `database/` | MySQL / MariaDB (XAMPP) | `localhost:3306`, base `autosmart_db` |
| Backend (API) | `usuario/backend/` | Spring Boot 4.1 · Java 21 · JWT | `http://localhost:8081` |
| Web | `usuario/frontend/` | React 19 · Vite 8 | `http://localhost:5173` |
| App móvil | `mobile/android/` | Android (Kotlin, Jetpack Compose) | Emulador o celular |
| Panel Django (opcional) | `usuario/backend-django/` | Django 4.2 | `http://localhost:8000/admin` |

Tiempo estimado: 20 a 30 minutos la primera vez.

---

## 1. Requisitos

| Programa | Versión | Descarga |
|---|---|---|
| Git | cualquiera reciente | https://git-scm.com |
| XAMPP (MySQL/MariaDB) | 8.x | https://www.apachefriends.org |
| JDK | 21 | https://adoptium.net (Temurin 21) |
| Node.js | 20.19 o superior (recomendado 22 LTS) | https://nodejs.org |
| IntelliJ IDEA (opcional) | Community o Ultimate | Para ejecutar el backend con un clic |
| Android Studio (solo para la app) | reciente | https://developer.android.com/studio |

No hace falta instalar Maven: el backend trae su propio Maven (`mvnw`).

## 2. Descargar el proyecto

```bash
git clone https://github.com/faridchz16/AutoSmart-ProyectoIntegrador-.git
cd AutoSmart-ProyectoIntegrador-
```

Si ya lo tienes, actualízalo con `git checkout main` y `git pull`.

## 3. Base de datos

1. Abre el **XAMPP Control Panel** y pulsa **Start** en **MySQL**.
2. Crea la base y sus tablas con el script del repositorio. Elige una opción:
   - **phpMyAdmin** (`http://localhost/phpmyadmin`, requiere encender Apache): pestaña **Importar** → `database/schema.sql` → **Continuar**.
   - **SQLyog / HeidiSQL / Workbench**: abre `database/schema.sql` y ejecútalo completo.
   - **Consola**: `C:\xampp\mysql\bin\mysql -u root < database/schema.sql`
3. Comprueba que existe la base `autosmart_db` con sus tablas, los 3 roles (`CLIENTE`, `MECANICO`, `ADMIN`), los métodos de pago y las preguntas iniciales del chatbot.

El script se puede volver a ejecutar sin perder datos (usa `IF NOT EXISTS` e `INSERT IGNORE`). Además, el backend crea o actualiza las tablas que falten al arrancar (`ddl-auto=update`).

> El backend se conecta con el usuario `root` sin contraseña (lo normal en XAMPP). Si tu MySQL tiene otra, cámbiala en `usuario/backend/src/main/resources/application.properties` (`spring.datasource.password`) **sin subir ese cambio al repositorio**.

## 4. Backend (Spring Boot)

### 4.1 Ejecutar

Con MySQL encendido:

```bash
cd usuario/backend
mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run          # macOS / Linux
```

La primera vez descarga las dependencias (unos minutos). Está listo cuando la consola muestra `Tomcat started on port 8081`.

Con IntelliJ: **File → Open** → carpeta `usuario/backend` → espera a que cargue Maven → ejecuta la clase principal (botón ▶).

### 4.2 Variables de entorno (opcionales)

El backend funciona sin configurar nada. Estas variables activan funciones extra:

| Variable | Para qué | Si no se configura |
|---|---|---|
| `JWT_SECRET` | Clave para firmar los tokens (mínimo 32 caracteres) | Usa una clave de desarrollo |
| `MAIL_USERNAME`, `MAIL_PASSWORD` (y `MAIL_HOST`, `MAIL_PORT`) | Enviar correos de confirmación de reserva | No se envía el correo; queda una línea `[CORREO] No configurado` en la consola |
| `FOTOS_DIR` | Carpeta donde se guardan las fotos de las deficiencias | `uploads/fotos` dentro de `usuario/backend` |
| `FIREBASE_CREDENTIALS` | Ruta al JSON de Firebase para las notificaciones push de la app | No se envían notificaciones push |

El **código de recuperación de contraseña** no llega por correo: aparece en la consola del backend en una línea `[RECUPERACION] Para ... codigo XXXXXX`.

### 4.3 Comprobar

Abre `http://localhost:8081/services` en el navegador. Si responde un JSON con el mensaje `Token ausente, inválido o expirado`, el backend está funcionando (la API pide iniciar sesión; desde la web se envía el token automáticamente).

## 5. Crear la cuenta del administrador

El registro público crea cuentas con rol **CLIENTE**. Para tener un administrador:

1. Con la web encendida (sección 6), regístrate en `http://localhost:5173/registro`, por ejemplo con `admin@autosmart.com`.
2. En la base de datos, cambia su rol a ADMIN:

```sql
UPDATE autosmart_db.usuarios
SET id_rol = (SELECT id_rol FROM autosmart_db.roles WHERE nombre = 'ADMIN')
WHERE correo = 'admin@autosmart.com';
```

3. Cierra sesión y vuelve a entrar: ahora abre el panel `/admin`.

Los **mecánicos** los crea el administrador desde **Usuarios → Nuevo mecánico**; la web muestra una contraseña temporal para entregársela.

## 6. Web (React)

En otra consola:

```bash
cd usuario/frontend
npm install
copy .env.example .env          # Windows (en macOS / Linux: cp .env.example .env)
npm run dev
```

Abre `http://localhost:5173`. Deja `VITE_DATOS_PRUEBA=false` en `.env` para trabajar con el backend real. Detalles, modo de datos de prueba y problemas frecuentes: [`usuario/frontend/README.md`](../usuario/frontend/README.md).

## 7. App móvil (Android)

1. Abre la carpeta `mobile/android` en **Android Studio** y espera a que termine Gradle.
2. Indica dónde está el backend en `mobile/android/local.properties` (este archivo no se sube al repositorio):

```properties
# Emulador de Android Studio (10.0.2.2 es la computadora)
autosmart.baseUrl=http://10.0.2.2:8081/
# Celular físico en la misma red Wi-Fi: usa la IP de la computadora (ipconfig)
# autosmart.baseUrl=http://192.168.1.63:8081/
```

3. Ejecuta la app (▶) en el emulador o en el celular. Con un celular físico, permite el puerto 8081 en el firewall de Windows.

Más detalles en [`mobile/android/README.md`](../mobile/android/README.md).

## 8. Primer uso: datos mínimos para probar

Con la base vacía, deja el taller listo en este orden:

| # | Quién | Dónde | Qué hacer |
|---|---|---|---|
| 1 | Administrador | Horario del taller | Revisar los días y horas de atención (y los feriados) |
| 2 | Administrador | Servicios | Crear 2 o 3 servicios con precio, duración y categoría |
| 3 | Administrador | Usuarios → Nuevo mecánico | Crear al menos un mecánico y anotar su contraseña temporal |
| 4 | Mecánico | Mi perfil y Mis horarios | Registrar sus especialidades y su horario semanal |
| 5 | Administrador | Inventario | Registrar algunos repuestos con stock (para el presupuesto y los consumos) |
| 6 | Cliente | Registro → Mis vehículos | Crear la cuenta y registrar un vehículo (placa `ABC-123`) |

Con eso ya se puede hacer el recorrido completo: el cliente reserva, el mecánico atiende y el cliente paga y califica. Los pasos están en [`docs/pruebas/PRUEBAS_ACEPTACION.md`](pruebas/PRUEBAS_ACEPTACION.md).

Pagos de prueba (pasarela sandbox, sin cobros reales): tarjeta `4242 4242 4242 4242` o Yape/Plin con el código `123456`.

## 9. Panel Django (opcional)

Panel de consulta sobre la misma base, fuera del Backlog. Instrucciones en [`usuario/backend-django/README.md`](../usuario/backend-django/README.md).

## 10. Problemas frecuentes

| Problema | Causa probable | Solución |
|---|---|---|
| El backend no arranca: `Communications link failure` | MySQL apagado | Enciende MySQL en XAMPP |
| `Unknown database 'autosmart_db'` | Falta el script | Ejecuta `database/schema.sql` (sección 3) |
| `Access denied for user 'root'` | MySQL tiene contraseña | Ajusta `spring.datasource.password` solo en tu copia local |
| `Port 8081 was already in use` | Otro backend abierto | Cierra la otra ventana o el proceso de Java |
| `invalid target release: 21` | JDK antiguo | Instala JDK 21 y configúralo en `JAVA_HOME` o en IntelliJ |
| La web dice "No hay conexión con el servidor" | Backend apagado | Enciende el backend (sección 4) |
| Una pantalla de la web queda vacía tras actualizar el proyecto | Vite no leyó el nuevo `vite.config.js` | Detén y vuelve a ejecutar `npm run dev` |
| La app no conecta | URL incorrecta o firewall | Revisa `autosmart.baseUrl` y el puerto 8081 en el firewall |
| No hay turnos para reservar | Falta configuración | El taller debe abrir ese día y el mecánico tener horario y la especialidad del servicio |
