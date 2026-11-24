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
   Base de conocimiento (T80.1):
     GET  /admin/faq                 -> [{ idFaq, pregunta, respuesta, palabrasClave ("pago, yape"), activa, fechaCreacion, fechaActualizacion }]
     POST /admin/faq                 { pregunta (≤ 255), respuesta (≤ 1000), palabrasClave (opcional, ≤ 255) } -> 201, activa
     PUT  /admin/faq/{id}            mismo cuerpo -> editada (no cambia si está activa o no)
     PUT  /admin/faq/{id}/deactivate -> desactivada; 409 si ya lo estaba. No hay forma de reactivarla:
          para volver a usarla se crea otra igual.
     El chatbot responde con una pregunta activa cuando el mensaje es igual a la pregunta o contiene
     alguna de sus palabras clave (gana la que tenga más coincidencias). El backend guarda las palabras en minúsculas.
   Prueba del chatbot: POST /chatbot/messages { mensaje, idConversacion? } (público)
     -> { idConversacion, intencion, respuesta, opciones[], datos, requiereLogin }
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

/* ---------- Base de conocimiento del chatbot (T80.2) ---------- */

export const MAX_PREGUNTA = 255;
export const MAX_RESPUESTA = 1000;
export const MAX_PALABRAS = 255;

function normalizarFaq(f = {}) {
  return {
    id: f.idFaq ?? f.id,
    pregunta: f.pregunta ?? "",
    respuesta: f.respuesta ?? "",
    palabrasClave: f.palabrasClave ?? "",
    activa: f.activa !== false,
    fechaCreacion: f.fechaCreacion ?? "",
    fechaActualizacion: f.fechaActualizacion ?? "",
  };
}

// "Pago, YAPE ,, plin" -> ["pago", "yape", "plin"] (igual que las guarda el backend)
export function separarPalabras(texto) {
  const vistas = new Set();
  return String(texto ?? "")
    .split(",")
    .map((p) => p.trim().toLowerCase())
    .filter((p) => p && !vistas.has(p) && vistas.add(p));
}

export async function listarFaq() {
  const datos = prueba.DATOS_PRUEBA ? await prueba.listarFaq() : await apiFetch("/admin/faq");
  return (Array.isArray(datos) ? datos : []).map(normalizarFaq);
}

function cuerpoFaq({ pregunta, respuesta, palabrasClave }) {
  const palabras = separarPalabras(palabrasClave).join(", ");
  return { pregunta: pregunta.trim(), respuesta: respuesta.trim(), palabrasClave: palabras || null };
}

export async function crearFaq(faq) {
  const cuerpo = cuerpoFaq(faq);
  const datos = prueba.DATOS_PRUEBA ? await prueba.crearFaq(cuerpo) : await apiFetch("/admin/faq", { metodo: "POST", cuerpo });
  return normalizarFaq(datos);
}

export async function editarFaq(id, faq) {
  const cuerpo = cuerpoFaq(faq);
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.editarFaq(id, cuerpo)
    : await apiFetch(`/admin/faq/${id}`, { metodo: "PUT", cuerpo });
  return normalizarFaq(datos);
}

export async function desactivarFaq(id) {
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.desactivarFaq(id)
    : await apiFetch(`/admin/faq/${id}/deactivate`, { metodo: "PUT" });
  return normalizarFaq(datos);
}

// Envía un mensaje de prueba al chatbot (cada prueba es una conversación nueva)
export async function probarChatbot(mensaje) {
  const cuerpo = { mensaje: mensaje.trim() };
  const datos = prueba.DATOS_PRUEBA
    ? await prueba.mensajeChatbot(cuerpo)
    : await apiFetch("/chatbot/messages", { metodo: "POST", cuerpo });
  return {
    intencion: datos?.intencion ?? "",
    respuesta: datos?.respuesta ?? "",
    opciones: Array.isArray(datos?.opciones) ? datos.opciones : [],
    idFaq: datos?.datos?.idFaq ?? null,
  };
}
