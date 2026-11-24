import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Indicadores del taller para el administrador (T73.2). Backend de Farid (rama main, T73.1):
     GET /admin/metrics?mes=AAAA-MM   (sin mes = mes actual)
       -> { mes "AAAA-MM", totalCitas, citasPorEstado { PENDIENTE: n, CONFIRMADA: n, ... PAGADO: n },
            ingresos (S/ de pagos APROBADOS), ingresosPorMetodo { TARJETA: S/, YAPE_PLIN: S/, EFECTIVO: S/ },
            tiempoPromedioMinutos (inicio → fin de las órdenes terminadas en el mes),
            calificacionPromedio (null si no hubo), totalCalificaciones }
   Con VITE_DATOS_PRUEBA=true sale de datosPrueba.js. */

export const NOMBRES_METODO = { TARJETA: "Tarjeta", YAPE_PLIN: "Yape / Plin", EFECTIVO: "Efectivo" };
export const nombreMetodo = (codigo) => NOMBRES_METODO[codigo] ?? String(codigo ?? "").replaceAll("_", " ");

const numero = (n) => {
  const v = Number(n);
  return Number.isFinite(v) ? v : 0;
};

function normalizar(m = {}, mes) {
  return {
    mes: m.mes ?? mes,
    totalCitas: numero(m.totalCitas),
    citasPorEstado: Object.entries(m.citasPorEstado ?? {}).map(([estado, total]) => ({ estado, total: numero(total) })),
    ingresos: numero(m.ingresos),
    ingresosPorMetodo: Object.entries(m.ingresosPorMetodo ?? {}).map(([metodo, monto]) => ({ metodo, monto: numero(monto) })),
    tiempoPromedioMinutos: numero(m.tiempoPromedioMinutos),
    calificacionPromedio: m.calificacionPromedio === null || m.calificacionPromedio === undefined ? null : numero(m.calificacionPromedio),
    totalCalificaciones: numero(m.totalCalificaciones),
  };
}

export async function obtenerMetricas(mes) {
  const datos = prueba.DATOS_PRUEBA ? await prueba.obtenerMetricas(mes) : await apiFetch(`/admin/metrics?mes=${mes}`);
  return normalizar(datos, mes);
}

/* ---------- Meses ---------- */

export const mesActual = () =>
  new Intl.DateTimeFormat("sv-SE", { timeZone: "America/Lima" }).format(new Date()).slice(0, 7);

export function sumarMeses(mes, n) {
  const [a, m] = mes.split("-").map(Number);
  const d = new Date(Date.UTC(a, m - 1 + n, 1));
  return d.toISOString().slice(0, 7);
}

const MESES = ["enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"];

// "2026-11" -> "noviembre de 2026"; corto: "nov."
export function nombreMes(mes, corto = false) {
  const [a, m] = mes.split("-").map(Number);
  const nombre = MESES[m - 1] ?? mes;
  return corto ? `${nombre.slice(0, 3)}.` : `${nombre} de ${a}`;
}

/* ---------- Formatos ---------- */

const SOLES = new Intl.NumberFormat("es-PE", { style: "currency", currency: "PEN" });
const SOLES_CORTO = new Intl.NumberFormat("es-PE", { style: "currency", currency: "PEN", maximumFractionDigits: 0 });
export const formatoSoles = (n) => SOLES.format(n);
export const formatoSolesCorto = (n) => SOLES_CORTO.format(n);

// Variación porcentual frente al mes anterior; null si no se puede comparar
export function variacion(actual, anterior) {
  if (!anterior) return null;
  return Math.round(((actual - anterior) / anterior) * 100);
}
