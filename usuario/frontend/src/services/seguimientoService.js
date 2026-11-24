import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";
import { SEVERIDADES } from "./ordenesService";

/* Seguimiento de la atención y presupuesto adicional (cliente). Backend de Farid (rama main):
     GET /appointments/{id}/tracking -> { idCita, ticket, estado, servicio, placa, mecanico, fecha, horaInicio,
          porcentajeAvance (0-100, de las tareas de la orden),
          lineaDeTiempo: [{ clave, titulo, completado, fecha ("AAAA-MM-DDTHH:mm:ss" o null), detalle }] }
          claves: RESERVADA, CONFIRMADA, RECIBIDO, DIAGNOSTICO, PRESUPUESTO (si hay), EN_REPARACION, FINALIZADA, PAGADO
          (o RESERVADA + CANCELADA / RECHAZADA, con el motivo en detalle). 403 si la cita no es del cliente.
     GET /appointments/{idCita}/quote -> último presupuesto de la orden. 404 "La cita aun no tiene presupuesto".
          { idPresupuesto, idOrden, idCita, estado: PENDIENTE|APROBADO|RECHAZADO, descripcion, fechaCreacion, fechaDecision,
            items: [{ tipo: MANO_OBRA|REPUESTO, descripcion, cantidad, precioUnitario, subtotal }], total,
            deficiencias: [{ idDeficiencia, categoria, severidad, descripcion, fechaRegistro, fotos: [{ idFoto, url }] }] }
     PUT /quotes/{idPresupuesto}/decision { decision: APROBADO|RECHAZADO } -> el presupuesto respondido.
          La cita vuelve a EN_PROCESO. 409 si ya fue respondido. Si se aprueba, el total se suma al costo de la orden.
     GET /photos/{idFoto} -> la imagen (con token; el cliente dueño puede verla).
   Con VITE_DATOS_PRUEBA=true todo sale de datosPrueba.js. */

export const ESTADOS_PRESUPUESTO = {
  PENDIENTE: "Esperando tu respuesta",
  APROBADO: "Aprobado",
  RECHAZADO: "Rechazado",
};
export const TIPOS_ITEM = { MANO_OBRA: "Mano de obra", REPUESTO: "Repuesto" };

const hora = (h) => String(h ?? "").slice(0, 5);
const numero = (n) => (n === null || n === undefined || n === "" ? 0 : Number(n));
const codigo = (t) =>
  String(t ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .trim()
    .toUpperCase();

function normalizarSeguimiento(s = {}) {
  return {
    id: s.idCita,
    ticket: s.ticket ?? "",
    estado: codigo(s.estado) || "PENDIENTE",
    servicio: s.servicio ?? "",
    placa: s.placa ?? "",
    mecanico: s.mecanico ?? "",
    fecha: String(s.fecha ?? "").slice(0, 10),
    inicio: hora(s.horaInicio),
    avance: Math.max(0, Math.min(100, Math.round(numero(s.porcentajeAvance)))),
    hitos: (Array.isArray(s.lineaDeTiempo) ? s.lineaDeTiempo : []).map((h) => ({
      clave: codigo(h.clave),
      titulo: h.titulo ?? "",
      completado: !!h.completado,
      fecha: h.fecha ?? "",
      detalle: h.detalle ?? "",
    })),
  };
}

const normalizarFoto = (f = {}) => {
  const idFoto = f.idFoto ?? f.id ?? null;
  return { clave: String(idFoto ?? f.url ?? f.src), idFoto, url: f.url ?? (idFoto !== null ? `/photos/${idFoto}` : ""), src: f.src ?? "" };
};

function normalizarPresupuesto(p = {}) {
  const estado = codigo(p.estado);
  return {
    id: p.idPresupuesto ?? p.id,
    idOrden: p.idOrden ?? null,
    idCita: p.idCita ?? null,
    estado: ESTADOS_PRESUPUESTO[estado] ? estado : "PENDIENTE",
    descripcion: p.descripcion ?? "",
    fechaCreacion: p.fechaCreacion ?? "",
    fechaDecision: p.fechaDecision ?? "",
    items: (Array.isArray(p.items) ? p.items : []).map((i) => ({
      tipo: TIPOS_ITEM[codigo(i.tipo)] ? codigo(i.tipo) : "MANO_OBRA",
      descripcion: i.descripcion ?? "",
      cantidad: numero(i.cantidad) || 1,
      precioUnitario: numero(i.precioUnitario),
      subtotal: numero(i.subtotal ?? numero(i.precioUnitario) * (numero(i.cantidad) || 1)),
    })),
    total: numero(p.total),
    deficiencias: (Array.isArray(p.deficiencias) ? p.deficiencias : [])
      .map((d) => ({
        id: d.idDeficiencia ?? d.id,
        categoria: d.categoria ?? "",
        severidad: SEVERIDADES[codigo(d.severidad)] ? codigo(d.severidad) : "LEVE",
        descripcion: d.descripcion ?? "",
        fotos: (d.fotos ?? []).map(normalizarFoto),
      }))
      .sort((a, b) => SEVERIDADES[a.severidad].orden - SEVERIDADES[b.severidad].orden),
  };
}

export async function obtenerSeguimiento(idCita) {
  const datos = prueba.DATOS_PRUEBA ? await prueba.seguimientoCita(idCita) : await apiFetch(`/appointments/${idCita}/tracking`);
  return normalizarSeguimiento(datos);
}

// null si la cita aún no tiene presupuesto
export async function obtenerPresupuesto(idCita) {
  try {
    const datos = prueba.DATOS_PRUEBA ? await prueba.presupuestoCita(idCita) : await apiFetch(`/appointments/${idCita}/quote`);
    return normalizarPresupuesto(datos);
  } catch (e) {
    if (e.status === 404) return null;
    throw e;
  }
}

export async function decidirPresupuesto(idPresupuesto, decision) {
  const cuerpo = { decision };
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.decidirPresupuesto(idPresupuesto, cuerpo)
    : await apiFetch(`/quotes/${idPresupuesto}/decision`, { metodo: "PUT", cuerpo });
  return normalizarPresupuesto(datos);
}

// Estados en los que ya existe una orden de trabajo (puede haber presupuesto)
export const CON_ORDEN = ["EN_PROCESO", "ESPERA_APROBACION", "FINALIZADA", "PAGADO"];
// Estados que siguen cambiando: la pantalla se actualiza sola
export const EN_CURSO = ["PENDIENTE", "CONFIRMADA", "EN_PROCESO", "ESPERA_APROBACION"];
