// Manejo de la sesión del usuario en el navegador (token + datos básicos).
// Todas las pantallas leen la sesión desde aquí para no repetir código.

const CLAVE_TOKEN = "token";
const CLAVE_USUARIO = "usuario";

// Guarda lo que devuelve el login: { token, correo, rol }
export function guardarSesion(datos) {
  localStorage.setItem(CLAVE_TOKEN, datos.token);
  localStorage.setItem(
    CLAVE_USUARIO,
    JSON.stringify({ correo: datos.correo, rol: datos.rol })
  );
}

export function obtenerToken() {
  return localStorage.getItem(CLAVE_TOKEN);
}

export function obtenerUsuario() {
  try {
    return JSON.parse(localStorage.getItem(CLAVE_USUARIO));
  } catch {
    return null;
  }
}

export function cerrarSesion() {
  localStorage.removeItem(CLAVE_TOKEN);
  localStorage.removeItem(CLAVE_USUARIO);
}

// "ROLE_ADMIN", "admin" o "ADMIN" se tratan igual: ADMIN
export function normalizarRol(rol) {
  return String(rol ?? "").toUpperCase().replace(/^ROLE_/, "");
}

// Fecha de expiración del JWT en milisegundos (null si no se puede leer)
export function expiracionSesion() {
  try {
    const payload = obtenerToken()?.split(".")[1];
    if (!payload) return null;
    const json = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
    const { exp } = JSON.parse(json);
    return typeof exp === "number" ? exp * 1000 : null;
  } catch {
    return null;
  }
}

// ¿Hay una sesión válida? Si el token ya venció, limpia todo.
export function sesionActiva() {
  if (!obtenerToken() || !obtenerUsuario()) return false;
  const exp = expiracionSesion();
  if (exp !== null && exp <= Date.now()) {
    cerrarSesion();
    return false;
  }
  return true;
}

// Panel web de cada rol (null = el rol no tiene panel web; el cliente usa la app móvil)
const PANEL_POR_ROL = { ADMIN: "/admin", MECANICO: "/mecanico" };

export function panelDeRol(rol) {
  return PANEL_POR_ROL[normalizarRol(rol)] ?? null;
}
