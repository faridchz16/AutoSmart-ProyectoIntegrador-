import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Calificaciones de los servicios (Sprint 4). Backend de Farid (rama main):
   Del mecánico (T67.1):
     GET /mechanics/me/ratings -> { promedio (null si no tiene), total,
          calificaciones: [{ idCalificacion, ticket, cliente ("Lucía F."), estrellas 1..5, comentario, fecha }] }
          De la más nueva a la más antigua. Si el administrador ocultó el comentario, llega comentario = null
          (la nota sí cuenta en el promedio).
   Con VITE_DATOS_PRUEBA=true todo sale de datosPrueba.js. */

const numero = (n) => {
  const v = Number(n);
  return Number.isFinite(v) ? v : 0;
};

function normalizarCalificacion(c = {}) {
  return {
    id: c.idCalificacion ?? c.id,
    ticket: c.ticket ?? "",
    cliente: c.cliente ?? "Cliente",
    estrellas: Math.max(1, Math.min(5, Math.round(numero(c.estrellas)))),
    comentario: (c.comentario ?? "").trim(),
    fecha: c.fecha ?? "",
  };
}

// Cuántas calificaciones hay de cada nota: { 5: n, 4: n, 3: n, 2: n, 1: n }
export function distribucion(calificaciones) {
  const conteo = { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
  calificaciones.forEach((c) => {
    conteo[c.estrellas] += 1;
  });
  return conteo;
}

export async function obtenerMisCalificaciones() {
  const datos = prueba.DATOS_PRUEBA ? await prueba.obtenerMisCalificaciones() : await apiFetch("/mechanics/me/ratings");
  const calificaciones = (Array.isArray(datos?.calificaciones) ? datos.calificaciones : []).map(normalizarCalificacion);
  return {
    promedio: datos?.promedio === null || datos?.promedio === undefined ? null : numero(datos.promedio),
    total: numero(datos?.total ?? calificaciones.length),
    calificaciones,
  };
}

// "2026-11-20T15:30:12" -> "20/11/2026"
export const fechaCorta = (f) => (f ? `${f.slice(8, 10)}/${f.slice(5, 7)}/${f.slice(0, 4)}` : "—");

// 4.6 -> "4,6"
export const formatoPromedio = (n) =>
  n === null || n === undefined ? "—" : Number(n).toLocaleString("es-PE", { minimumFractionDigits: 1, maximumFractionDigits: 1 });
