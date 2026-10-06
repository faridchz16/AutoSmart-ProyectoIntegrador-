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