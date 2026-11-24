import { apiFetch } from "./api";
import * as prueba from "./datosPrueba";

/* Administración del chatbot (Sprint 4). Backend de Farid (rama main):
   Derivaciones a un asesor (T79.1):
     GET /admin/chatbot/handoffs[?estado=PENDIENTE|ATENDIDA] -> [{ idDerivacion, idConversacion, fecha,
          usuario ("Visitante sin cuenta" si no inició sesión), contacto (correo o teléfono), resumen,
          estado, nota, atendidoPor, fechaAtencion }]   (de la más nueva a la más antigua)
          El resumen son los últimos mensajes de la conversación, uno por línea: "USUARIO: ..." / "BOT: ...".
     PUT /admin/chatbot/handoffs/{id}/attend { nota (obligatoria, máx. 500) } -> la derivación ATENDIDA.
          409 si ya fue atendida.
   Con VITE_DATOS_PRUEBA=true todo sale de datosPrueba.js. */

export const ESTADOS_DERIVACION = { PENDIENTE: "Pendiente", ATENDIDA: "Atendida" };
export const MAX_NOTA = 500;

function normalizarDerivacion(d = {}) {
  const estado = String(d.estado ?? "").toUpperCase();
  return {
    id: d.idDerivacion ?? d.id,
    idConversacion: d.idConversacion ?? "",
    fecha: d.fecha ?? "",
    usuario: d.usuario || "Visitante sin cuenta",
    contacto: d.contacto ?? "",
    resumen: d.resumen ?? "",
    estado: ESTADOS_DERIVACION[estado] ? estado : "PENDIENTE",
    nota: d.nota ?? "",
    atendidoPor: d.atendidoPor ?? "",
    fechaAtencion: d.fechaAtencion ?? "",
  };
}

// estado = "" para todas. Las pendientes se ordenan de la más antigua a la más nueva (orden de atención).
export async function listarDerivaciones(estado = "") {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.listarDerivaciones(estado)
    : await apiFetch(`/admin/chatbot/handoffs${estado ? `?estado=${estado}` : ""}`);
  const lista = (Array.isArray(datos) ? datos : []).map(normalizarDerivacion);
  return estado === "PENDIENTE" ? lista.sort((a, b) => a.fecha.localeCompare(b.fecha)) : lista;
}

export async function atenderDerivacion(id, nota) {
  const cuerpo = { nota: nota.trim() };
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.atenderDerivacion(id, cuerpo)
    : await apiFetch(`/admin/chatbot/handoffs/${id}/attend`, { metodo: "PUT", cuerpo });
  return normalizarDerivacion(datos);
}

export async function contarDerivacionesPendientes() {
  const lista = await listarDerivaciones("PENDIENTE");
  return lista.length;
}

// Avisa al menú que cambiaron las derivaciones pendientes
export const EVENTO_DERIVACIONES = "autosmart:derivaciones";
export const avisarCambioDerivaciones = () => window.dispatchEvent(new Event(EVENTO_DERIVACIONES));

// "USUARIO: hola\nBOT: Horario:\nLunes..." -> [{ emisor: "USUARIO", texto }, { emisor: "BOT", texto: "Horario:\nLunes..." }]
export function leerConversacion(resumen) {
  const mensajes = [];
  String(resumen ?? "")
    .split("\n")
    .forEach((linea) => {
      const m = linea.match(/^(USUARIO|BOT):\s?(.*)$/);
      if (m) mensajes.push({ emisor: m[1], texto: m[2] });
      else if (mensajes.length) mensajes[mensajes.length - 1].texto += `\n${linea}`;
      else if (linea.trim()) mensajes.push({ emisor: "USUARIO", texto: linea });
    });
  return mensajes;
}

// Enlace para contactar: correo -> mailto, teléfono -> tel
export function enlaceContacto(contacto) {
  const c = String(contacto ?? "").trim();
  if (!c) return null;
  if (c.includes("@")) return `mailto:${c}`;
  const digitos = c.replace(/[^\d+]/g, "");
  return digitos.length >= 6 ? `tel:${digitos}` : null;
}
