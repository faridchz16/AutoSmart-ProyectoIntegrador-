import { obtenerToken, cerrarSesion } from "./sesion";

// URL base del backend. Se define en el archivo .env (VITE_API_URL).
// Vacía = se usa el proxy de Vite en desarrollo (ver vite.config.js).
export const API_URL = import.meta.env.VITE_API_URL ?? "";

// fetch con el token JWT incluido, para las pantallas del panel.
// Uso: const usuarios = await apiFetch("/users");
//      await apiFetch("/services", { metodo: "POST", cuerpo: { nombre: "..." } });
export async function apiFetch(ruta, { metodo = "GET", cuerpo, ...resto } = {}) {
  const token = obtenerToken();
  let respuesta;

  try {
    respuesta = await fetch(`${API_URL}${ruta}`, {
      method: metodo,
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
      ...resto,
    });
  } catch {
    throw new Error("No hay conexión con el servidor.");
  }

  // Token vencido o inválido: se cierra la sesión y se vuelve al login
  if (respuesta.status === 401) {
    cerrarSesion();
    window.location.assign("/login");
    throw new Error("Tu sesión venció. Inicia sesión de nuevo.");
  }

  const datos = await respuesta.json().catch(() => ({}));

  if (!respuesta.ok) {
    if (respuesta.status === 403) {
      throw new Error("No tienes permiso para realizar esta acción.");
    }
    if (respuesta.status >= 500) {
      throw new Error("El servidor tuvo un problema. Inténtalo de nuevo.");
    }
    throw new Error(datos.error || "No se pudo completar la operación.");
  }

  return datos;
}
