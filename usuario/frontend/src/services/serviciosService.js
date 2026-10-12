import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Catálogo de servicios del panel de administración.
   Endpoints según el backlog del Sprint 1 (Farid):
     GET   /admin/services                 -> todos los servicios, activos e inactivos
     POST  /admin/services                 -> crea un servicio (T18.1)
     PUT   /admin/services/{id}            -> edita datos y tarifa (T19.1)
     PATCH /admin/services/{id}/estado     -> activa o desactiva (T19.1)
   Cuerpo: { nombre, descripcion, precioBase, duracionMinutos }
   Si el backend usa otros nombres, solo hay que cambiarlos aquí. */

// Adapta lo que devuelva el backend a un formato único para la pantalla
function normalizarServicio(s = {}) {
  return {
    id: s.id ?? s.idServicio ?? s.id_servicio,
    nombre: s.nombre ?? "",
    descripcion: s.descripcion ?? "",
    precioBase: Number(s.precioBase ?? s.precio_base ?? s.precio ?? 0),
    duracionMinutos: Number(s.duracionMinutos ?? s.duracion_minutos ?? s.duracion ?? 0),
    activo: s.activo ?? true,
  };
}

export async function listarServicios() {
  const datos = prueba.DATOS_PRUEBA ? await prueba.listarServicios() : await apiFetch("/admin/services");
  const lista = Array.isArray(datos) ? datos : datos.content ?? [];
  return lista.map(normalizarServicio);
}

export async function crearServicio(servicio) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.crearServicio(servicio)
    : await apiFetch("/admin/services", { metodo: "POST", cuerpo: servicio });
  return normalizarServicio(datos);
}

export async function editarServicio(id, servicio) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.editarServicio(id, servicio)
    : await apiFetch(`/admin/services/${id}`, { metodo: "PUT", cuerpo: servicio });
  return normalizarServicio({ id, ...servicio, ...datos });
}

export async function cambiarEstadoServicio(id, activo) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.cambiarEstadoServicio(id, activo)
    : await apiFetch(`/admin/services/${id}/estado`, { metodo: "PATCH", cuerpo: { activo } });
  return { activo: datos?.activo ?? activo };
}

// Formatos para mostrar
const SOLES = new Intl.NumberFormat("es-PE", { style: "currency", currency: "PEN" });
export const formatoPrecio = (n) => SOLES.format(n);

export function formatoDuracion(minutos) {
  if (!minutos) return "—";
  const h = Math.floor(minutos / 60);
  const m = minutos % 60;
  if (!h) return `${m} min`;
  return m ? `${h} h ${m} min` : `${h} h`;
}
