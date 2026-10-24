import { apiFetch } from "./api";
import { listarUsuarios } from "./usuariosService";
import * as prueba from "./datosPrueba";

/* Reservas del taller para el administrador (T38.2).
   Contrato propuesto a Farid para T38.1 (aún no está en main):
     GET /admin/appointments?desde=&hasta=&estado=&idMecanico=&q=&page=&size=
       -> { contenido: [{ idCita, ticket, fecha, horaInicio, horaFin, estado, servicio, precio,
                          cliente, placa, idMecanico, mecanico }],
            pagina, tamano, totalElementos, totalPaginas }
   q busca por ticket, cliente o placa. Todos los filtros son opcionales.
   Si el backend usa otros nombres, solo hay que ajustar este archivo. */

const RUTA = "/admin/appointments";

const hora = (h) => String(h ?? "").slice(0, 5);

function normalizar(r = {}) {
  return {
    id: r.idCita ?? r.id,
    ticket: r.ticket ?? "",
    fecha: String(r.fecha ?? "").slice(0, 10),
    horaInicio: hora(r.horaInicio),
    horaFin: hora(r.horaFin),
    estado: String(r.estado ?? "PENDIENTE").toUpperCase(),
    servicio: r.servicio ?? r.nombreServicio ?? "",
    precio: r.precio === null || r.precio === undefined ? null : Number(r.precio),
    cliente: r.cliente ?? r.nombreCliente ?? "",
    placa: r.placa ?? "",
    idMecanico: r.idMecanico ?? null,
    mecanico: r.mecanico ?? r.nombreMecanico ?? "",
  };
}

// Acepta la página del backend ({ contenido, ... }), una página de Spring ({ content, ... }) o una lista
function leerPagina(datos, tamano) {
  const lista = Array.isArray(datos) ? datos : datos.contenido ?? datos.content ?? [];
  const reservas = lista.map(normalizar);
  const total = Array.isArray(datos) ? reservas.length : datos.totalElementos ?? datos.totalElements ?? reservas.length;
  return {
    reservas,
    total,
    paginas: Math.max(1, Array.isArray(datos) ? Math.ceil(total / tamano) : datos.totalPaginas ?? datos.totalPages ?? 1),
  };
}

export async function listarReservas({ desde, hasta, estado = "", idMecanico = "", q = "", pagina = 0, tamano = 10 }) {
  if (prueba.DATOS_PRUEBA) {
    return leerPagina(await prueba.listarReservas({ desde, hasta, estado, idMecanico, q, pagina, tamano }), tamano);
  }
  const params = new URLSearchParams({ page: pagina, size: tamano });
  if (desde) params.set("desde", desde);
  if (hasta) params.set("hasta", hasta);
  if (estado) params.set("estado", estado);
  if (idMecanico) params.set("idMecanico", idMecanico);
  if (q.trim()) params.set("q", q.trim());
  try {
    return leerPagina(await apiFetch(`${RUTA}?${params}`), tamano);
  } catch (e) {
    // 404/405: el backend todavía no tiene el endpoint (T38.1)
    if (e.status === 404 || e.status === 405) {
      const error = new Error("El backend todavía no tiene la lista de reservas (GET /admin/appointments, tarea T38.1).");
      error.pendiente = true;
      throw error;
    }
    throw e;
  }
}

// Mecánicos para el filtro (usa la gestión de usuarios, que ya existe en el backend)
export async function listarMecanicos() {
  const { usuarios } = await listarUsuarios({ rol: "MECANICO", tamano: 100 });
  return usuarios.map((u) => ({ id: u.id, nombre: u.nombre })).sort((a, b) => a.nombre.localeCompare(b.nombre, "es"));
}
