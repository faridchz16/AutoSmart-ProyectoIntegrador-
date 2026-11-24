/* Fechas en la hora de Lima para los filtros del panel (Sprint 4). */

// "AAAA-MM-DD" de hoy en Lima
export const hoyLima = () => new Intl.DateTimeFormat("sv-SE", { timeZone: "America/Lima" }).format(new Date());

// Suma días a una fecha "AAAA-MM-DD"
export function sumarDias(fecha, dias) {
  const [a, m, d] = fecha.split("-").map(Number);
  return new Date(Date.UTC(a, m - 1, d + dias)).toISOString().slice(0, 10);
}

// Primer día del mes, "meses" meses atrás (0 = este mes)
export function inicioDeMes(fecha, meses = 0) {
  const [a, m] = fecha.split("-").map(Number);
  return new Date(Date.UTC(a, m - 1 - meses, 1)).toISOString().slice(0, 10);
}

// Último día del mes anterior a "fecha"
export const finDelMesAnterior = (fecha) => sumarDias(inicioDeMes(fecha, 0), -1);

// "2026-11-20T10:58:00" -> "20/11/2026 10:58"; "2026-11-20" -> "20/11/2026"
export const fechaHora = (f) => (f ? `${f.slice(8, 10)}/${f.slice(5, 7)}/${f.slice(0, 4)}${f.length > 10 ? ` ${f.slice(11, 16)}` : ""}` : "—");
export const fechaCorta = (f) => (f ? `${f.slice(8, 10)}/${f.slice(5, 7)}/${f.slice(0, 4)}` : "—");

// Valida un rango de fechas de un filtro: "" si está bien
export function validarRango({ desde, hasta }, hoy = hoyLima()) {
  if (desde && hasta && desde > hasta) return "La fecha «desde» no puede ser posterior a «hasta».";
  if (hasta && hasta > hoy) return "La fecha «hasta» no puede ser futura.";
  if (desde && desde > hoy) return "La fecha «desde» no puede ser futura.";
  return "";
}
