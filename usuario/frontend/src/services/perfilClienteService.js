import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Perfil del cliente (panel web del cliente). Backend de Farid (rama main, T05.1):
     GET /users/me -> { idUsuario, nombre, correo, telefono, rol }
     PUT /users/me { nombre (obligatorio, ≤ 100), telefono (opcional: 7 a 15 dígitos, puede empezar con +) }
          -> el perfil actualizado. Un teléfono vacío se guarda como null. El correo no se puede cambiar.
   Con VITE_DATOS_PRUEBA=true sale de datosPrueba.js. */

export const MAX_NOMBRE = 100;
export const PATRON_TELEFONO = /^\+?[0-9]{7,15}$/;

const normalizar = (p = {}) => ({
  id: p.idUsuario ?? p.id,
  nombre: p.nombre ?? "",
  correo: p.correo ?? "",
  telefono: p.telefono ?? "",
  rol: p.rol ?? "CLIENTE",
});

export async function obtenerPerfil() {
  const datos = prueba.DATOS_PRUEBA ? await prueba.obtenerPerfilCliente() : await apiFetch("/users/me");
  return normalizar(datos);
}

export async function guardarPerfil({ nombre, telefono }) {
  const cuerpo = { nombre: nombre.trim(), telefono: telefono.replace(/[\s-]/g, "") };
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.guardarPerfilCliente(cuerpo)
    : await apiFetch("/users/me", { metodo: "PUT", cuerpo });
  return normalizar(datos);
}

// "Lucía Fernández Ríos" -> "Lucía"
export const primerNombre = (nombre) => String(nombre ?? "").trim().split(/\s+/)[0] || "";
