import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Horario del taller y feriados (T40.2) – backend de Farid (T40.1, rama main):
     GET  /admin/shop-hours  -> [{ diaSemana (1 = lunes … 7 = domingo), abierto, horaApertura "HH:mm", horaCierre "HH:mm" }]
          Siempre devuelve los 7 días (los no configurados vienen con el horario por defecto).
     PUT  /admin/shop-hours  [los 7 días, mismo formato]  -> los 7 días guardados
          Las horas son obligatorias aunque el día esté cerrado; si está abierto, apertura < cierre.
     GET  /admin/holidays?anio=2026  -> [{ idFeriado, fecha "AAAA-MM-DD", descripcion }]
     POST /admin/holidays  { fecha, descripcion }  (descripción obligatoria, máx. 100; 409 si ya hay un feriado ese día)
     DELETE /admin/holidays/{id}
   En los feriados y los días cerrados el taller no ofrece turnos para reservar. */

export const MAX_DESCRIPCION_FERIADO = 100;

const hora = (h) => String(h ?? "").slice(0, 5);

function normalizarDia(d = {}) {
  return {
    diaSemana: Number(d.diaSemana),
    abierto: !!d.abierto,
    horaApertura: hora(d.horaApertura) || "08:00",
    horaCierre: hora(d.horaCierre) || "18:00",
  };
}

const normalizarFeriado = (f = {}) => ({
  id: f.idFeriado ?? f.id,
  fecha: String(f.fecha ?? "").slice(0, 10),
  descripcion: f.descripcion ?? "",
});

const ordenarDias = (lista) => (Array.isArray(lista) ? lista : []).map(normalizarDia).sort((a, b) => a.diaSemana - b.diaSemana);

export async function obtenerHorarioTaller() {
  return ordenarDias(prueba.DATOS_PRUEBA ? await prueba.obtenerHorarioTaller() : await apiFetch("/admin/shop-hours"));
}

export async function guardarHorarioTaller(dias) {
  const cuerpo = dias.map(({ diaSemana, abierto, horaApertura, horaCierre }) => ({
    diaSemana,
    abierto,
    horaApertura,
    horaCierre,
  }));
  return ordenarDias(
    prueba.DATOS_PRUEBA
      ? await prueba.guardarHorarioTaller(cuerpo)
      : await apiFetch("/admin/shop-hours", { metodo: "PUT", cuerpo })
  );
}

export async function listarFeriados(anio) {
  const datos = prueba.DATOS_PRUEBA ? await prueba.listarFeriados(anio) : await apiFetch(`/admin/holidays?anio=${anio}`);
  return (Array.isArray(datos) ? datos : []).map(normalizarFeriado).sort((a, b) => a.fecha.localeCompare(b.fecha));
}

export async function crearFeriado({ fecha, descripcion }) {
  const cuerpo = { fecha, descripcion: descripcion.trim() };
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.crearFeriado(cuerpo)
    : await apiFetch("/admin/holidays", { metodo: "POST", cuerpo });
  return normalizarFeriado({ ...cuerpo, ...datos });
}

export async function eliminarFeriado(id) {
  if (prueba.DATOS_PRUEBA) return prueba.eliminarFeriado(id);
  await apiFetch(`/admin/holidays/${id}`, { metodo: "DELETE" });
}

// Minutos entre dos horas "HH:mm" (negativo si el cierre es antes de la apertura)
export function minutosEntre(inicio, fin) {
  const [h1, m1] = inicio.split(":").map(Number);
  const [h2, m2] = fin.split(":").map(Number);
  return h2 * 60 + m2 - (h1 * 60 + m1);
}
