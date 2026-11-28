import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";
import { minutosEntre } from "./ordenesService";

/* Historial de servicios del mecánico (T68.2). Backend de Farid (rama main, T68.1):
     GET /mechanics/me/history?desde=AAAA-MM-DD&hasta=AAAA-MM-DD  (ambos opcionales)
       -> [{ idOrden, idCita, ticket, servicio, placa, cliente, fechaInicio, fechaFin,
             estado FINALIZADA|PAGADO, estrellas (null si el cliente aún no califica) }]
       Solo órdenes terminadas del mecánico, filtradas por la fecha de fin. Sin "desde" trae todo;
       sin "hasta" llega hasta hoy. 400 si "desde" es posterior a "hasta".
   Con VITE_DATOS_PRUEBA=true sale de datosPrueba.js. */

export const ESTADOS_HISTORIAL = { FINALIZADA: "Por cobrar", PAGADO: "Pagado" };

function normalizar(o = {}) {
  const estado = String(o.estado ?? "").toUpperCase();
  const estrellas = o.estrellas === null || o.estrellas === undefined ? null : Number(o.estrellas);
  return {
    idOrden: o.idOrden,
    idCita: o.idCita,
    ticket: o.ticket ?? "",
    servicio: o.servicio ?? "Servicio",
    placa: o.placa ?? "",
    cliente: o.cliente ?? "",
    fechaInicio: o.fechaInicio ?? "",
    fechaFin: o.fechaFin ?? "",
    estado: ESTADOS_HISTORIAL[estado] ? estado : "FINALIZADA",
    estrellas: Number.isFinite(estrellas) ? estrellas : null,
    minutos: o.fechaInicio && o.fechaFin ? minutosEntre(o.fechaInicio, o.fechaFin) : null,
  };
}

// desde / hasta en formato AAAA-MM-DD; vacíos = sin límite
export async function listarHistorial({ desde = "", hasta = "" } = {}) {
  const params = new URLSearchParams();
  if (desde) params.set("desde", desde);
  if (hasta) params.set("hasta", hasta);
  const consulta = params.toString();
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.listarHistorialMecanico({ desde, hasta })
    : await apiFetch(`/mechanics/me/history${consulta ? `?${consulta}` : ""}`);
  return (Array.isArray(datos) ? datos : []).map(normalizar).sort((a, b) => b.fechaFin.localeCompare(a.fechaFin));
}

// Resumen del periodo: servicios, tiempo promedio, calificación promedio y cuántos ya se cobraron
export function resumirHistorial(lista) {
  const conTiempo = lista.filter((o) => o.minutos !== null && o.minutos >= 0);
  const calificados = lista.filter((o) => o.estrellas !== null);
  return {
    servicios: lista.length,
    minutosPromedio: conTiempo.length ? Math.round(conTiempo.reduce((s, o) => s + o.minutos, 0) / conTiempo.length) : null,
    promedioEstrellas: calificados.length
      ? Math.round((calificados.reduce((s, o) => s + o.estrellas, 0) / calificados.length) * 10) / 10
      : null,
    calificados: calificados.length,
    pagados: lista.filter((o) => o.estado === "PAGADO").length,
  };
}

// 80 -> "1 h 20 min"
export function textoDuracion(minutos) {
  if (minutos === null || minutos === undefined) return "—";
  if (minutos < 60) return `${minutos} min`;
  const h = Math.floor(minutos / 60);
  const m = minutos % 60;
  return m ? `${h} h ${m} min` : `${h} h`;
}

/* ---------- Fechas del filtro (hora de Lima) ---------- */

export const hoyLima = () => new Intl.DateTimeFormat("sv-SE", { timeZone: "America/Lima" }).format(new Date());

// Primer día del mes, "meses" meses atrás (0 = este mes)
function inicioDeMes(hoy, meses) {
  const [a, m] = hoy.split("-").map(Number);
  const d = new Date(Date.UTC(a, m - 1 - meses, 1));
  return d.toISOString().slice(0, 10);
}

export const PERIODOS = [
  { valor: "mes", texto: "Este mes", rango: (hoy) => ({ desde: inicioDeMes(hoy, 0), hasta: hoy }) },
  { valor: "3meses", texto: "Últimos 3 meses", rango: (hoy) => ({ desde: inicioDeMes(hoy, 2), hasta: hoy }) },
  { valor: "anio", texto: "Este año", rango: (hoy) => ({ desde: `${hoy.slice(0, 4)}-01-01`, hasta: hoy }) },
  { valor: "todo", texto: "Todo", rango: () => ({ desde: "", hasta: "" }) },
];

// "2026-11-20T10:58:00" -> "20/11/2026 10:58"
export const fechaHora = (f) => (f ? `${f.slice(8, 10)}/${f.slice(5, 7)}/${f.slice(0, 4)} ${f.slice(11, 16)}` : "—");
export const fechaCorta = (f) => (f ? `${f.slice(8, 10)}/${f.slice(5, 7)}/${f.slice(0, 4)}` : "");
