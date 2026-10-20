import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Catálogo de servicios del panel de administración (backend de Farid, rama main):
     POST  /admin/services                 -> crea un servicio (T18.1)
     PUT   /admin/services/{id}            -> edita datos y tarifa (T19.1)
     PATCH /admin/services/{id}/estado     -> activa o desactiva (T19.1)
     GET   /services                       -> catálogo: SOLO servicios activos (T10.1)
   Cuerpo: { nombre, descripcion, precioBase, duracionMinutos, categoria }
   Respuesta: { idServicio, nombre, descripcion, precioBase, duracionMinutos, activo, categoria, especialidad }
   categoria (T37.1): "PREVENTIVO" | "CORRECTIVO". Los servicios antiguos quedan como PREVENTIVO.

   PENDIENTE (backend): falta GET /admin/services con activos e inactivos.
   Mientras no exista, el panel usa GET /services y los servicios desactivados
   desaparecen de la tabla al recargar. */

// Categorías del catálogo (T37.1 / T37.2)
export const CATEGORIAS = {
  PREVENTIVO: { texto: "Preventivo", descripcion: "Mantenimiento programado para evitar fallas." },
  CORRECTIVO: { texto: "Correctivo", descripcion: "Reparación de una falla o avería ya presente." },
};

const categoriaValida = (c) => {
  const valor = String(c ?? "").toUpperCase();
  return CATEGORIAS[valor] ? valor : "PREVENTIVO";
};

// Adapta lo que devuelva el backend a un formato único para la pantalla
function normalizarServicio(s = {}) {
  return {
    id: s.id ?? s.idServicio ?? s.id_servicio,
    nombre: s.nombre ?? "",
    descripcion: s.descripcion ?? "",
    precioBase: Number(s.precioBase ?? s.precio_base ?? s.precio ?? 0),
    duracionMinutos: Number(s.duracionMinutos ?? s.duracion_minutos ?? s.duracion ?? 0),
    activo: s.activo ?? true,
    categoria: categoriaValida(s.categoria),
  };
}

const aLista = (datos) => (Array.isArray(datos) ? datos : datos.contenido ?? datos.content ?? []);

// Devuelve { servicios, soloActivos }. soloActivos = true cuando el backend no permite ver los inactivos.
export async function listarServicios() {
  if (prueba.DATOS_PRUEBA) {
    return { servicios: (await prueba.listarServicios()).map(normalizarServicio), soloActivos: false };
  }
  try {
    const datos = await apiFetch("/admin/services");
    return { servicios: aLista(datos).map(normalizarServicio), soloActivos: false };
  } catch (e) {
    // 404/405: el backend aún no tiene GET /admin/services -> se usa el catálogo público
    if (e.status !== 404 && e.status !== 405) throw e;
    const datos = await apiFetch("/services");
    return { servicios: aLista(datos).map(normalizarServicio), soloActivos: true };
  }
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
