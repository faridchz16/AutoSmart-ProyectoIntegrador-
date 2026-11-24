import { apiFetch, descargarArchivo, guardarBlob } from "./api";
import * as prueba from "./datosPrueba";
import { listarVehiculos } from "./vehiculosService";
import { nombreMetodo } from "./indicadoresService";

/* Pago del servicio por el cliente (panel web). Backend de Farid (rama main):
     GET  /work-orders/{idOrden}/cost -> { idOrden, idCita, ticket, estadoCita, servicio: { nombre, precio },
            adicionales: [{ tipo, descripcion, cantidad, precioUnitario, subtotal }] (del presupuesto APROBADO),
            totalAdicionales, repuestosUtilizados: [{ material, cantidad, unidad }], total, moneda: "PEN",
            puedePagar (cita FINALIZADA y sin pago aprobado), pago: { idPago, codigo, metodo, fecha } | null }
     GET  /payment-methods -> [{ codigo: TARJETA|YAPE_PLIN|EFECTIVO, nombre, enLinea }]
     POST /payments { idOrden, metodo, tokenPago (≤ 100) } -> 201 { idPago, codigo, idOrden, ticket, metodo,
            estado: APROBADO|RECHAZADO, monto, fecha, motivoRechazo, comprobanteUrl, registradoPor }
            El backend cobra el total calculado (no el que envía el navegador). EFECTIVO -> 400 (se paga en el taller).
            409 "La orden ya fue pagada" / "Solo se puede pagar una orden FINALIZADA". Si se aprueba, la cita pasa a PAGADO.
     GET  /payments/{idPago}/receipt -> PDF del comprobante (solo pagos aprobados).
   Pasarelas sandbox del backend (T63.1): la tarjeta se envía como token de prueba (el número nunca sale del navegador)
   y Yape/Plin con el código de aprobación de 6 dígitos (123456 se aprueba).
   El número de orden de una cita sale del historial del vehículo (GET /vehicles/{id}/history -> mantenimientos[].idOrden)
   o, si hubo presupuesto, de GET /appointments/{id}/quote.
   Con VITE_DATOS_PRUEBA=true todo sale de datosPrueba.js. */

const numero = (n) => (n === null || n === undefined || n === "" ? 0 : Number(n));

// Número de orden de trabajo de una cita ({ id, placa }) o null si aún no tiene
export async function ordenDeCita(cita) {
  const vehiculo = (await listarVehiculos()).find((v) => v.placa === cita.placa);
  if (vehiculo) {
    const h = prueba.DATOS_PRUEBA ? await prueba.historialVehiculoCliente(vehiculo.id) : await apiFetch(`/vehicles/${vehiculo.id}/history`);
    const m = (h?.mantenimientos ?? []).find((x) => Number(x.idCita) === Number(cita.id));
    if (m?.idOrden) return m.idOrden;
  }
  try {
    const p = prueba.DATOS_PRUEBA ? await prueba.presupuestoCita(cita.id) : await apiFetch(`/appointments/${cita.id}/quote`);
    return p?.idOrden ?? null;
  } catch (e) {
    if (e.status === 404) return null;
    throw e;
  }
}

export async function obtenerCosto(idOrden) {
  const c = prueba.DATOS_PRUEBA ? await prueba.costoOrdenCliente(idOrden) : await apiFetch(`/work-orders/${idOrden}/cost`);
  return {
    idOrden: c.idOrden,
    idCita: c.idCita,
    ticket: c.ticket ?? "",
    estadoCita: String(c.estadoCita ?? "").toUpperCase(),
    servicio: { nombre: c.servicio?.nombre ?? "", precio: numero(c.servicio?.precio) },
    adicionales: (c.adicionales ?? []).map((a) => ({
      tipo: String(a.tipo ?? "").toUpperCase(),
      descripcion: a.descripcion ?? "",
      cantidad: numero(a.cantidad) || 1,
      precioUnitario: numero(a.precioUnitario),
      subtotal: numero(a.subtotal),
    })),
    totalAdicionales: numero(c.totalAdicionales),
    repuestos: (c.repuestosUtilizados ?? []).map((r) => ({ material: r.material ?? "", cantidad: numero(r.cantidad), unidad: r.unidad ?? "" })),
    total: numero(c.total),
    puedePagar: !!c.puedePagar,
    pago: c.pago ? { id: c.pago.idPago, codigo: c.pago.codigo ?? "", metodo: c.pago.metodo ?? "", fecha: c.pago.fecha ?? "" } : null,
  };
}

// Métodos habilitados. Si no responde, los tres del taller.
export async function listarMetodos() {
  try {
    const datos = prueba.DATOS_PRUEBA ? await prueba.metodosPagoCliente() : await apiFetch("/payment-methods");
    if (Array.isArray(datos) && datos.length) {
      return datos.map((m) => ({ codigo: String(m.codigo).toUpperCase(), nombre: m.nombre || nombreMetodo(m.codigo), enLinea: !!m.enLinea }));
    }
  } catch {
    // se usan los conocidos
  }
  return [
    { codigo: "TARJETA", nombre: "Tarjeta de crédito o débito", enLinea: true },
    { codigo: "YAPE_PLIN", nombre: "Yape / Plin", enLinea: true },
    { codigo: "EFECTIVO", nombre: "Efectivo en el taller", enLinea: false },
  ];
}

export async function pagar({ idOrden, metodo, tokenPago }) {
  const cuerpo = { idOrden, metodo, tokenPago };
  const p = prueba.DATOS_PRUEBA ? await prueba.pagarOrdenCliente(cuerpo) : await apiFetch("/payments", { metodo: "POST", cuerpo });
  return {
    id: p.idPago,
    codigo: p.codigo ?? "",
    metodo: p.metodo ?? metodo,
    estado: String(p.estado ?? "").toUpperCase(),
    monto: numero(p.monto),
    fecha: p.fecha ?? "",
    motivoRechazo: p.motivoRechazo ?? "",
  };
}

export async function descargarComprobante({ id, codigo, monto = 0 }) {
  if (prueba.DATOS_PRUEBA) {
    guardarBlob(await prueba.comprobantePrueba({ codigo, cliente: "Lucía Fernández", monto }), `comprobante-${codigo}.pdf`);
    return;
  }
  await descargarArchivo(`/payments/${id}/receipt`, `comprobante-${codigo}.pdf`);
}

/* ---------- Tarjeta (sandbox) ---------- */

// Tarjetas de prueba de la pasarela sandbox -> token que espera el backend
export const TARJETAS_PRUEBA = [
  { numero: "4242424242424242", token: "pm_card_visa", texto: "Visa: se aprueba" },
  { numero: "5555555555554444", token: "pm_card_mastercard", texto: "Mastercard: se aprueba" },
  { numero: "4000000000000002", token: "pm_card_chargeDeclined", texto: "Rechazada por el banco" },
  { numero: "4000000000009995", token: "pm_card_chargeDeclinedInsufficientFunds", texto: "Fondos insuficientes" },
  { numero: "4000000000000069", token: "pm_card_chargeDeclinedExpiredCard", texto: "Tarjeta vencida" },
];
export const CODIGO_YAPE_PRUEBA = "123456";

export const soloDigitos = (t) => String(t ?? "").replace(/\D/g, "");
// "4242424242424242" -> "4242 4242 4242 4242" (Amex: 4-6-5)
export function formatearTarjeta(texto) {
  const d = soloDigitos(texto).slice(0, 19);
  if (/^3[47]/.test(d)) return [d.slice(0, 4), d.slice(4, 10), d.slice(10, 15)].filter(Boolean).join(" ");
  return d.replace(/(\d{4})(?=\d)/g, "$1 ");
}
export function marcaTarjeta(texto) {
  const d = soloDigitos(texto);
  if (/^4/.test(d)) return "Visa";
  if (/^(5[1-5]|2[2-7])/.test(d)) return "Mastercard";
  if (/^3[47]/.test(d)) return "American Express";
  if (/^3(0[0-5]|[68])/.test(d)) return "Diners Club";
  return "";
}
export function luhnValido(texto) {
  const d = soloDigitos(texto);
  if (d.length < 13) return false;
  let suma = 0;
  for (let i = 0; i < d.length; i++) {
    let n = Number(d[d.length - 1 - i]);
    if (i % 2 === 1) n = n * 2 > 9 ? n * 2 - 9 : n * 2;
    suma += n;
  }
  return suma % 10 === 0;
}
export const tokenDeTarjeta = (texto) => TARJETAS_PRUEBA.find((t) => t.numero === soloDigitos(texto))?.token ?? null;
