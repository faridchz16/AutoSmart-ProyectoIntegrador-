import { API_URL } from "./api";
import { obtenerToken } from "./sesion";
import * as prueba from "./datosPrueba";

/* Chatbot de la página de inicio (TEC4.1). Backend de Farid (rama main, T75.1 a T79.1):
     POST /chatbot/messages { mensaje (≤ 500), idConversacion (opcional) }   — público
       -> { idConversacion, intencion, respuesta, opciones: [texto], datos, requiereLogin }
       Intenciones: saludo, servicios, horario, faq, cotizar, consultar_estado, agendar, hablar_asesor, no_entendido.
       Si hay sesión, se envía el token para que el chatbot reconozca al usuario (estado de su vehículo, agendar).
       datos según la intención:
         cotizar          { servicio, precioDesde, precioHasta, referencial }
         consultar_estado { ticket, placa, estado, avance }
         agendar          resumen { servicio, placa, fecha, hora, mecanico } o cita creada { idCita, ticket, estado }
         hablar_asesor    { idDerivacion }
   Se mantiene el mismo idConversacion para que el chatbot recuerde el contexto (por ejemplo, al agendar por pasos).
   Con VITE_DATOS_PRUEBA=true responde datosPrueba.js. */

const normalizar = (d = {}) => ({
  idConversacion: d.idConversacion ?? null,
  intencion: d.intencion ?? "",
  respuesta: d.respuesta ?? "",
  opciones: Array.isArray(d.opciones) ? d.opciones.filter(Boolean) : [],
  datos: d.datos && typeof d.datos === "object" ? d.datos : null,
  requiereLogin: !!d.requiereLogin,
});

async function llamar(cuerpo, conToken) {
  const token = conToken ? obtenerToken() : null;
  const respuesta = await fetch(`${API_URL}/chatbot/messages`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: JSON.stringify(cuerpo),
  });
  return respuesta;
}

export async function enviarMensaje(mensaje, idConversacion) {
  const cuerpo = { mensaje: mensaje.trim().slice(0, 500), ...(idConversacion ? { idConversacion } : {}) };
  if (prueba.DATOS_PRUEBA) return normalizar(await prueba.mensajeChatbot(cuerpo));

  let respuesta;
  try {
    respuesta = await llamar(cuerpo, true);
    // Si la sesión guardada venció, el chat sigue funcionando como visitante (sin cerrar la página)
    if (respuesta.status === 401 || respuesta.status === 403) respuesta = await llamar(cuerpo, false);
  } catch {
    throw new Error("No hay conexión con el servidor.");
  }
  const datos = await respuesta.json().catch(() => ({}));
  if (!respuesta.ok) {
    const error = new Error(respuesta.status >= 500 ? "El asistente no está disponible en este momento." : datos.error || "No se pudo enviar el mensaje.");
    error.status = respuesta.status;
    throw error;
  }
  return normalizar(datos);
}

export const formatoSoles = (n) => new Intl.NumberFormat("es-PE", { style: "currency", currency: "PEN" }).format(Number(n) || 0);
