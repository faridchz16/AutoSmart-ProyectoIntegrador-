import { API_URL } from "./api";
import * as prueba from "./datosPrueba";

// Envía correo y contraseña al backend y devuelve { token, tipo, correo, rol }
export async function login(correo, password) {
  let respuesta;

  try {
    respuesta = await fetch(`${API_URL}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ correo, password }),
    });
  } catch {
    throw new Error("No hay conexión con el servidor.");
  }

  // Intentamos leer la respuesta como JSON (si viene vacía, usamos {})
  const datos = await respuesta.json().catch(() => ({}));

  if (!respuesta.ok) {
    if (respuesta.status >= 500) {
      throw new Error("No se pudo conectar con el servidor. ¿Está encendido el backend?");
    }
    throw new Error(datos.error || "No se pudo iniciar sesión.");
  }

  return datos;
}
// Envía nombre, correo y contraseña al backend para crear una cuenta nueva
export async function registrar(nombre, correo, password) {
  let respuesta;

  try {
    respuesta = await fetch(`${API_URL}/auth/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ nombre, correo, password }),
    });
  } catch {
    throw new Error("No hay conexión con el servidor.");
  }

  const datos = await respuesta.json().catch(() => ({}));

  if (!respuesta.ok) {
    if (respuesta.status >= 500) {
      throw new Error("No se pudo conectar con el servidor. ¿Está encendido el backend?");
    }
    throw new Error(datos.error || "No se pudo completar el registro. Revisa los datos.");
  }

  return datos;
}

/* ---------- Recuperación de contraseña (T04.3) ----------
   Endpoints según el backlog (T04.1, Farid):
     POST /auth/forgot-password  { correo }                    -> envía un código de 6 dígitos (vence en 15 min)
     POST /auth/reset-password   { correo, codigo, password }  -> guarda la nueva contraseña
   Con VITE_DATOS_PRUEBA activo se simulan (código de prueba: 123456). */

async function postAuth(ruta, cuerpo) {
  let respuesta;
  try {
    respuesta = await fetch(`${API_URL}${ruta}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(cuerpo),
    });
  } catch {
    throw new Error("No hay conexión con el servidor.");
  }
  const datos = await respuesta.json().catch(() => ({}));
  return { respuesta, datos };
}

export async function solicitarCodigo(correo) {
  if (prueba.DATOS_PRUEBA) return prueba.solicitarCodigo(correo);

  const { respuesta, datos } = await postAuth("/auth/forgot-password", { correo });
  // Por seguridad no se revela si el correo existe: un 404 se trata igual que un envío correcto
  if (respuesta.ok || respuesta.status === 404) return datos;
  if (respuesta.status >= 500) {
    throw new Error("No se pudo enviar el código. Inténtalo en unos minutos.");
  }
  throw new Error(datos.error || "No se pudo enviar el código.");
}

export async function restablecerPassword(correo, codigo, password) {
  if (prueba.DATOS_PRUEBA) return prueba.restablecerPassword(correo, codigo, password);

  const { respuesta, datos } = await postAuth("/auth/reset-password", { correo, codigo, password });
  if (respuesta.ok) return datos;
  if (respuesta.status >= 500) {
    throw new Error("No se pudo actualizar la contraseña. Inténtalo en unos minutos.");
  }
  throw new Error(datos.error || "El código no es válido o ya venció.");
}
