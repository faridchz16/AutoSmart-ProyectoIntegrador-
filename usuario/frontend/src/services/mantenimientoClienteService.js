import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";
import { obtenerUsuario } from "./sesion";
import { SEVERIDADES, ELEMENTOS_CHECKLIST, textoCategoria } from "./ordenesService";

/* Detalle del mantenimiento y calificación del servicio (cliente). Backend de Farid (rama main):
     GET  /work-orders/{idOrden}/detail -> { idOrden, idCita, ticket, estado, servicio, precioServicio, placa, vehiculo,
            mecanico, fechaInicio, fechaFin, diagnostico, checklist: [{ elemento, estado, observacion }],
            deficiencias: [{ idDeficiencia, categoria, severidad, descripcion, fotos: [{ idFoto, url }] }],
            tareas: { totalTareas, completadas, porcentajeAvance, tareas: [{ descripcion, completada, fechaCompletada }] },
            presupuesto: { idPresupuesto, estado, descripcion, total, items } | null,
            repuestosConsumidos: [{ codigoMaterial, cantidad, costoUnitario, referencia, fecha }], costoRepuestos, totalEstimado }
            Solo el cliente dueño o el mecánico asignado (403).
     POST /work-orders/{idOrden}/rating { estrellas (1 a 5), comentario (opcional, ≤ 500) } -> 201
            { idCalificacion, idOrden, mecanico, estrellas, comentario, fecha }
            Solo servicios FINALIZADA o PAGADO; una sola vez por orden (409 "Este servicio ya fue calificado").
   El backend no tiene un GET de la calificación del cliente: la que se envía desde este navegador se recuerda
   (por número de cita) para mostrarla, y si el backend responde 409 se marca como ya calificada.
   Con VITE_DATOS_PRUEBA=true todo sale de datosPrueba.js. */

export const MAX_COMENTARIO = 500;
export const TEXTO_ESTRELLAS = { 1: "Muy malo", 2: "Malo", 3: "Regular", 4: "Bueno", 5: "Excelente" };

const numero = (n) => (n === null || n === undefined || n === "" ? 0 : Number(n));
const codigo = (t) =>
  String(t ?? "")
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .trim()
    .toUpperCase()
    .replace(/\s+/g, "_");
const textoElemento = (e) => ELEMENTOS_CHECKLIST.find((x) => x.clave === codigo(e))?.texto ?? textoCategoria(e);

export async function obtenerDetalle(idOrden) {
  const d = prueba.DATOS_PRUEBA ? await prueba.detalleMantenimientoCliente(idOrden) : await apiFetch(`/work-orders/${idOrden}/detail`);
  const tareas = d.tareas ?? {};
  return {
    idOrden: d.idOrden,
    idCita: d.idCita,
    ticket: d.ticket ?? "",
    estado: codigo(d.estado),
    servicio: d.servicio ?? "",
    precioServicio: numero(d.precioServicio),
    placa: d.placa ?? "",
    vehiculo: d.vehiculo ?? "",
    mecanico: d.mecanico ?? "",
    fechaInicio: d.fechaInicio ?? "",
    fechaFin: d.fechaFin ?? "",
    diagnostico: d.diagnostico ?? "",
    checklist: (d.checklist ?? []).map((i) => ({ elemento: textoElemento(i.elemento), ok: codigo(i.estado) === "OK", observacion: i.observacion ?? "" })),
    deficiencias: (d.deficiencias ?? [])
      .map((x) => {
        const sev = SEVERIDADES[codigo(x.severidad)] ? codigo(x.severidad) : "LEVE";
        return {
          id: x.idDeficiencia ?? x.id,
          categoria: textoCategoria(x.categoria),
          severidad: sev,
          descripcion: x.descripcion ?? "",
          fotos: (x.fotos ?? []).map((f) => {
            const idFoto = f.idFoto ?? f.id ?? null;
            return { clave: String(idFoto ?? f.url ?? f.src), idFoto, url: f.url ?? (idFoto !== null ? `/photos/${idFoto}` : ""), src: f.src ?? "" };
          }),
        };
      })
      .sort((a, b) => SEVERIDADES[a.severidad].orden - SEVERIDADES[b.severidad].orden),
    tareas: {
      total: numero(tareas.totalTareas),
      completadas: numero(tareas.completadas),
      avance: numero(tareas.porcentajeAvance),
      lista: (tareas.tareas ?? []).map((t) => ({ descripcion: t.descripcion ?? "", completada: !!t.completada, fecha: t.fechaCompletada ?? "" })),
    },
    presupuesto: d.presupuesto
      ? { estado: codigo(d.presupuesto.estado), descripcion: d.presupuesto.descripcion ?? "", total: numero(d.presupuesto.total), items: d.presupuesto.items ?? [] }
      : null,
    repuestos: (d.repuestosConsumidos ?? []).map((r) => ({ codigo: r.codigoMaterial ?? "", cantidad: numero(r.cantidad), referencia: r.referencia ?? "" })),
  };
}

/* ---------- Calificación ---------- */

const claveLocal = () => `autosmart:calificaciones:${obtenerUsuario()?.correo ?? "cliente"}`;
function leerLocal() {
  try {
    return JSON.parse(localStorage.getItem(claveLocal()) ?? "{}") ?? {};
  } catch {
    return {};
  }
}
function guardarLocal(idCita, valor) {
  try {
    localStorage.setItem(claveLocal(), JSON.stringify({ ...leerLocal(), [idCita]: valor }));
  } catch {
    // sin almacenamiento disponible: solo no se recuerda
  }
}

// { estrellas, comentario, fecha } si se calificó desde este navegador; { estrellas: null } si el backend dijo que ya estaba calificada
export const calificacionGuardada = (idCita) => leerLocal()[idCita] ?? null;

export async function calificar({ idOrden, idCita }, { estrellas, comentario }) {
  const cuerpo = { estrellas, comentario: comentario.trim() || null };
  try {
    const r = prueba.DATOS_PRUEBA ? await prueba.calificarServicio(idOrden, cuerpo) : await apiFetch(`/work-orders/${idOrden}/rating`, { metodo: "POST", cuerpo });
    const guardada = { estrellas: r.estrellas ?? estrellas, comentario: r.comentario ?? "", fecha: r.fecha ?? "" };
    guardarLocal(idCita, guardada);
    return guardada;
  } catch (e) {
    if (e.status === 409 && /calificado/i.test(e.message)) guardarLocal(idCita, { estrellas: null, comentario: "", fecha: "" });
    throw e;
  }
}
