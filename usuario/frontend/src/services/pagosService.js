import { apiFetch, descargarArchivo, guardarBlob } from "./api";
import * as prueba from "./datosPrueba";
import { NOMBRES_METODO, nombreMetodo } from "./indicadoresService";

/* Pagos del taller para el administrador (Sprint 4). Backend de Farid (rama main):
   Supervisión (T70.1):
     GET /admin/payments?desde=&hasta=&metodo=&estado=   (todos opcionales)
       Sin fechas: del 1 del mes actual a hoy. metodo: TARJETA | YAPE_PLIN | EFECTIVO. estado: APROBADO | RECHAZADO.
       -> { desde, hasta, cantidad, totalAprobado (solo APROBADOS),
            pagos: [{ idPago, codigo "PAG-2026-00012", fecha, ticket, cliente, placa, metodo, estado, monto,
                      registradoPor (correo del admin en pagos en efectivo; null en línea) }] }  (del más nuevo al más antiguo)
       400 si "desde" es posterior a "hasta" o si el estado no es válido.
   Métodos habilitados:  GET /payment-methods -> [{ codigo, nombre, enLinea }]
   Comprobante PDF (T64.1, también para ADMIN): GET /payments/{id}/receipt
   Con VITE_DATOS_PRUEBA=true todo sale de datosPrueba.js. */

export const ESTADOS_PAGO = { APROBADO: "Aprobado", RECHAZADO: "Rechazado" };

const numero = (n) => {
  const v = Number(n);
  return Number.isFinite(v) ? v : 0;
};

function normalizarPago(p = {}) {
  const estado = String(p.estado ?? "").toUpperCase();
  return {
    id: p.idPago ?? p.id,
    codigo: p.codigo ?? "",
    fecha: p.fecha ?? "",
    ticket: p.ticket ?? "",
    cliente: p.cliente ?? "",
    placa: p.placa ?? "",
    metodo: String(p.metodo ?? "").toUpperCase(),
    estado: ESTADOS_PAGO[estado] ? estado : "APROBADO",
    monto: numero(p.monto),
    registradoPor: p.registradoPor ?? "",
  };
}

export async function listarPagos({ desde = "", hasta = "", metodo = "", estado = "" } = {}) {
  const params = new URLSearchParams();
  if (desde) params.set("desde", desde);
  if (hasta) params.set("hasta", hasta);
  if (metodo) params.set("metodo", metodo);
  if (estado) params.set("estado", estado);
  const filtros = { desde, hasta, metodo, estado };
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.listarPagosAdmin(filtros)
    : await apiFetch(`/admin/payments${params.toString() ? `?${params}` : ""}`);
  const pagos = (Array.isArray(datos?.pagos) ? datos.pagos : []).map(normalizarPago);
  return {
    desde: datos?.desde ?? desde,
    hasta: datos?.hasta ?? hasta,
    cantidad: numero(datos?.cantidad ?? pagos.length),
    totalAprobado: numero(datos?.totalAprobado),
    pagos,
  };
}

// Métodos de pago habilitados; si el servidor no responde, se usan los tres del taller
export async function listarMetodosPago() {
  try {
    const datos = prueba.DATOS_PRUEBA ? null : await apiFetch("/payment-methods");
    const lista = Array.isArray(datos) ? datos : [];
    if (lista.length) return lista.map((m) => ({ codigo: String(m.codigo).toUpperCase(), nombre: m.nombre || nombreMetodo(m.codigo) }));
  } catch {
    // se usan los métodos conocidos
  }
  return Object.entries(NOMBRES_METODO).map(([codigo, nombre]) => ({ codigo, nombre }));
}

export async function descargarComprobante(pago) {
  if (prueba.DATOS_PRUEBA) {
    guardarBlob(await prueba.comprobantePrueba(pago), `comprobante-${pago.codigo}.pdf`);
    return;
  }
  await descargarArchivo(`/payments/${pago.id}/receipt`, `comprobante-${pago.codigo}.pdf`);
}

// Totales del listado por estado y por método (solo aprobados)
export function resumirPagos(pagos) {
  const aprobados = pagos.filter((p) => p.estado === "APROBADO");
  const porMetodo = {};
  aprobados.forEach((p) => {
    porMetodo[p.metodo] = (porMetodo[p.metodo] ?? 0) + p.monto;
  });
  return {
    aprobados: aprobados.length,
    rechazados: pagos.length - aprobados.length,
    porMetodo: Object.entries(porMetodo)
      .map(([metodo, monto]) => ({ metodo, monto: Math.round(monto * 100) / 100 }))
      .sort((a, b) => b.monto - a.monto),
  };
}
