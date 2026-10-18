import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Horarios del mecánico (T21.2) – backend de Farid (T21.1, rama main):
     GET    /mechanics/me/schedule        -> [{ idHorario, diaSemana, horaInicio, horaFin }]
     POST   /mechanics/me/schedule        { diaSemana, horaInicio, horaFin }
     PUT    /mechanics/me/schedule/{id}   { diaSemana, horaInicio, horaFin }
     DELETE /mechanics/me/schedule/{id}
   diaSemana: 1 = lunes … 7 = domingo. Horas en formato "HH:mm".
   El backend rechaza rangos con fin <= inicio (400) y rangos que se cruzan el mismo día (409). */

const RUTA = "/mechanics/me/schedule";

export const DIAS = ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"];

// "08:00:00" o "08:00" -> "08:00"
const hora = (h) => String(h ?? "").slice(0, 5);

function normalizar(h = {}) {
  return {
    id: h.idHorario ?? h.id,
    diaSemana: Number(h.diaSemana),
    horaInicio: hora(h.horaInicio),
    horaFin: hora(h.horaFin),
  };
}

const ordenar = (lista) =>
  [...lista].sort((a, b) => a.diaSemana - b.diaSemana || a.horaInicio.localeCompare(b.horaInicio));

export async function listarHorarios() {
  const datos = prueba.DATOS_PRUEBA ? await prueba.listarHorarios() : await apiFetch(RUTA);
  return ordenar((Array.isArray(datos) ? datos : []).map(normalizar));
}

export async function crearHorario(horario) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.crearHorario(horario)
    : await apiFetch(RUTA, { metodo: "POST", cuerpo: horario });
  return normalizar(datos);
}

export async function editarHorario(id, horario) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.editarHorario(id, horario)
    : await apiFetch(`${RUTA}/${id}`, { metodo: "PUT", cuerpo: horario });
  return normalizar({ idHorario: id, ...horario, ...datos });
}

export async function eliminarHorario(id) {
  if (prueba.DATOS_PRUEBA) return prueba.eliminarHorario(id);
  await apiFetch(`${RUTA}/${id}`, { metodo: "DELETE" });
}

// Minutos entre dos horas "HH:mm"
export function duracionMinutos(inicio, fin) {
  const [h1, m1] = inicio.split(":").map(Number);
  const [h2, m2] = fin.split(":").map(Number);
  return h2 * 60 + m2 - (h1 * 60 + m1);
}

export function formatoHoras(minutos) {
  const h = Math.floor(minutos / 60);
  const m = minutos % 60;
  if (!h) return `${m} min`;
  return m ? `${h} h ${m} min` : `${h} h`;
}
