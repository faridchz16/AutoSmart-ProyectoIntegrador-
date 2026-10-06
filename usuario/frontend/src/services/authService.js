// Envía correo y contraseña al backend y devuelve { token, tipo, correo, rol }
export async function login(correo, password) {
  let respuesta;

  try {
    respuesta = await fetch("/auth/login", {
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
    respuesta = await fetch("/auth/register", {
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