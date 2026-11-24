import { useEffect, useImperativeHandle, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { enviarMensaje, formatoSoles } from "../services/chatbotService";
import { ESTADOS_CITA } from "../services/agendaService";
import "./ChatBot.css";

/* ------------------------------------------------------------------
   Asistente AutoSmart (TEC4.1)
   Conversa con el chatbot del backend (POST /chatbot/messages), el mismo que usa la app:
   servicios, horario, preguntas frecuentes, cotización referencial, estado del vehículo,
   agendar una cita y hablar con un asesor. Muestra las opciones rápidas que devuelve el servidor
   y tarjetas para la cotización, el estado del vehículo y el resumen de la cita.
   Los consejos de mantenimiento del Sprint 1 se mantienen como respaldo: se usan cuando el
   servidor no entiende la pregunta o no responde.
------------------------------------------------------------------- */

const SALUDO =
  "Hola, soy el asistente de AutoSmart. Puedo contarte sobre nuestros servicios y precios, el horario, las preguntas frecuentes o el estado de tu vehículo. ¿Qué necesitas?";

const SUGERENCIAS = [
  "¿Qué servicios ofrecen?",
  "¿Cuál es su horario?",
  "¿Cuánto cuesta un cambio de aceite?",
  "Preguntas frecuentes",
  "¿Cada cuánto cambio el aceite?",
];

// Quita tildes y signos para comparar palabras clave sin errores de ortografía
const normalizar = (texto) =>
  texto
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9ñ\s]/g, " ");

const REGLAS = [
  {
    claves: ["aceite", "filtro", "lubricante"],
    respuesta:
      "Como referencia general, el aceite y el filtro se cambian cada 5 000 a 10 000 km, según el tipo de aceite y el uso del auto. Lo más confiable es seguir el manual de tu vehículo. Si registras tu kilometraje en AutoSmart, podrás llevar el control de cuándo toca el siguiente cambio.",
  },
  {
    claves: ["freno", "frenos", "pastilla", "pastillas", "disco"],
    respuesta:
      "Conviene revisar los frenos al menos una vez al año o cada 10 000 km. Si escuchas chillidos, sientes vibración al frenar o el pedal baja más de lo normal, hazlo revisar cuanto antes.",
  },
  {
    claves: ["alinea", "alineamiento", "alineacion", "balanceo", "direccion", "volante"],
    respuesta:
      "Se recomienda alinear y balancear cada 10 000 km, o antes si el auto se desvía hacia un lado, el volante vibra o las llantas se desgastan de forma despareja.",
  },
  {
    claves: ["kilometraje", "kilometros", "km", "recorrido", "odometro"],
    respuesta:
      "El control de kilometraje te permite saber cuándo corresponde cada mantenimiento. Registras el recorrido de tu vehículo y AutoSmart te ayuda a anticipar los servicios pendientes.",
  },
  {
    claves: ["escaner", "diagnostico", "falla", "tablero", "check engine"],
    respuesta:
      "El diagnóstico con escáner lee los códigos de falla de tu auto y ayuda a ubicar el problema. Conviene hacerlo cuando se enciende una luz de alerta en el tablero.",
  },
  {
    claves: ["bateria", "arrancar", "arranque", "luces"],
    respuesta:
      "Se recomienda revisar la batería y las luces cada 6 meses. Si el auto cuesta arrancar o las luces se ven débiles, conviene hacerla revisar pronto.",
  },
  {
    claves: ["cuenta", "registro", "registrar", "registrarme", "crear", "unirme", "inscribir"],
    respuesta:
      "Para crear tu cuenta de cliente, entra a «Crear cuenta», escribe tu nombre, tu correo y una contraseña de mínimo 8 caracteres, con una mayúscula y un número. Después podrás iniciar sesión y registrar tus vehículos.",
  },
  {
    claves: ["contrasena", "clave", "olvide", "recuperar", "restablecer"],
    respuesta:
      "Si olvidaste tu contraseña, entra a «Iniciar sesión» y elige «¿Olvidaste tu contraseña?». Te enviaremos un código a tu correo para crear una nueva.",
  },
  {
    claves: ["iniciar sesion", "login", "ingresar", "entrar", "acceder"],
    respuesta:
      "Usa el botón «Iniciar sesión» e ingresa tu correo y tu contraseña. Si la olvidaste, puedes recuperarla desde esa misma pantalla.",
  },
];

const SIN_CONEXION =
  "No pude conectarme con el asistente en este momento. Inténtalo en unos minutos o escríbenos desde la app.";

// Consejo de mantenimiento que coincide con la pregunta (null si ninguno)
function consejoLocal(texto) {
  const limpio = ` ${normalizar(texto).replace(/\s+/g, " ").trim()} `;
  const regla = REGLAS.find((r) => r.claves.some((c) => limpio.includes(` ${c} `) || (c.length > 4 && limpio.includes(c))));
  return regla?.respuesta ?? null;
}

const OPCION_LOGIN = "Iniciar sesión";

/* Tarjetas con los datos que acompañan algunas respuestas */
function TarjetaDatos({ intencion, datos }) {
  if (!datos) return null;
  if (intencion === "cotizar" && datos.precioDesde !== undefined) {
    return (
      <div className="chat-card">
        <span className="chat-card-etq">{datos.servicio}</span>
        <strong>
          {formatoSoles(datos.precioDesde)}
          {datos.precioHasta && datos.precioHasta !== datos.precioDesde && ` – ${formatoSoles(datos.precioHasta)}`}
        </strong>
        {datos.referencial && <small>Precio referencial: el monto final depende del diagnóstico.</small>}
      </div>
    );
  }
  if (intencion === "consultar_estado" && datos.placa) {
    const avance = Math.max(0, Math.min(100, Number(datos.avance) || 0));
    return (
      <div className="chat-card">
        <span className="chat-card-etq">
          {datos.placa} · {datos.ticket}
        </span>
        <strong>{ESTADOS_CITA[datos.estado] ?? datos.estado}</strong>
        <span className="chat-barra" role="progressbar" aria-valuenow={avance} aria-valuemin={0} aria-valuemax={100} aria-label="Avance">
          <span style={{ width: `${avance}%` }} />
        </span>
        <small>{avance} % de avance</small>
      </div>
    );
  }
  if (intencion === "agendar" && datos.fecha) {
    return (
      <dl className="chat-card chat-resumen">
        <dt>Servicio</dt>
        <dd>{datos.servicio}</dd>
        <dt>Vehículo</dt>
        <dd>{datos.placa}</dd>
        <dt>Fecha</dt>
        <dd>
          {String(datos.fecha).split("-").reverse().join("/")} · {String(datos.hora ?? "").slice(0, 5)}
        </dd>
        {datos.mecanico && (
          <>
            <dt>Mecánico</dt>
            <dd>{datos.mecanico}</dd>
          </>
        )}
      </dl>
    );
  }
  if (intencion === "agendar" && datos.ticket) {
    return (
      <div className="chat-card chat-card-ok">
        <span className="chat-card-etq">Cita registrada</span>
        <strong>{datos.ticket}</strong>
        <small>{ESTADOS_CITA[datos.estado] ?? datos.estado}</small>
      </div>
    );
  }
  return null;
}

const IconoChat = () => (
  <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M21 11.5a8.4 8.4 0 0 1-8.5 8.3 8.7 8.7 0 0 1-3.6-.8L3 20l1.2-4.6A8.1 8.1 0 0 1 3.5 11.5 8.4 8.4 0 0 1 12 3.2a8.4 8.4 0 0 1 9 8.3Z" />
    <path d="M8 11h.01M12 11h.01M16 11h.01" />
  </svg>
);

const IconoCerrar = () => (
  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
    <path d="M6 6l12 12M18 6 6 18" />
  </svg>
);

const IconoEnviar = () => (
  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M5 12h14M13 6l6 6-6 6" />
  </svg>
);

function ChatBot({ ref }) {
  const navigate = useNavigate();
  const [abierto, setAbierto] = useState(false);
  const [mensajes, setMensajes] = useState([{ de: "bot", texto: SALUDO }]);
  const [texto, setTexto] = useState("");
  const [escribiendo, setEscribiendo] = useState(false);
  const listaRef = useRef(null);
  const inputRef = useRef(null);
  const conversacion = useRef(null);

  const enviar = async (contenido) => {
    const limpio = contenido.trim();
    if (!limpio || escribiendo) return;
    if (limpio === OPCION_LOGIN) {
      navigate("/login");
      return;
    }
    setMensajes((prev) => [...prev, { de: "usuario", texto: limpio }]);
    setTexto("");
    setEscribiendo(true);
    let respuesta;
    try {
      const r = await enviarMensaje(limpio, conversacion.current);
      conversacion.current = r.idConversacion ?? conversacion.current;
      const consejo = r.intencion === "no_entendido" ? consejoLocal(limpio) : null;
      respuesta = consejo
        ? { de: "bot", texto: consejo, opciones: r.opciones }
        : { de: "bot", texto: r.respuesta, intencion: r.intencion, datos: r.datos, opciones: r.opciones };
    } catch {
      const consejo = consejoLocal(limpio);
      respuesta = consejo ? { de: "bot", texto: consejo } : { de: "bot", texto: SIN_CONEXION, error: true };
    }
    setMensajes((prev) => [...prev, respuesta]);
    setEscribiendo(false);
  };

  // Permite que la página abra el asistente o le haga una pregunta
  useImperativeHandle(ref, () => ({
    abrir: () => setAbierto(true),
    preguntar: (pregunta) => {
      setAbierto(true);
      enviar(pregunta);
    },
  }));

  // Desplaza la conversación al último mensaje
  useEffect(() => {
    const lista = listaRef.current;
    if (lista) lista.scrollTop = lista.scrollHeight;
  }, [mensajes, escribiendo, abierto]);

  // Enfoca el campo al abrir y cierra con Escape
  useEffect(() => {
    if (!abierto) return;
    inputRef.current?.focus();
    const alTeclear = (e) => e.key === "Escape" && setAbierto(false);
    window.addEventListener("keydown", alTeclear);
    return () => window.removeEventListener("keydown", alTeclear);
  }, [abierto]);

  // Opciones rápidas: las sugerencias al inicio y luego las que devuelve el último mensaje del asistente
  const ultimo = mensajes[mensajes.length - 1];
  const opciones = mensajes.length === 1 ? SUGERENCIAS : ultimo.de === "bot" ? ultimo.opciones ?? [] : [];

  return (
    <>
      {abierto && (
        <section className="chat-panel" role="dialog" aria-label="Asistente de AutoSmart">
          <header className="chat-head">
            <span className="chat-avatar" aria-hidden="true">
              <img src="/logo-white.png" alt="" />
            </span>
            <div className="chat-title">
              <strong>Asistente AutoSmart</strong>
              <span>
                <i className="chat-dot" aria-hidden="true" /> En línea
              </span>
            </div>
            <button type="button" className="chat-close" onClick={() => setAbierto(false)} aria-label="Cerrar el asistente">
              <IconoCerrar />
            </button>
          </header>

          <div className="chat-list" ref={listaRef} aria-live="polite">
            {mensajes.map((m, i) => (
              <div key={i} className={`chat-fila chat-fila-${m.de}`}>
                <p className={`chat-msg chat-msg-${m.de}${m.error ? " chat-msg-error" : ""}`}>{m.texto}</p>
                <TarjetaDatos intencion={m.intencion} datos={m.datos} />
              </div>
            ))}
            {escribiendo && (
              <p className="chat-msg chat-msg-bot chat-typing" aria-label="El asistente está escribiendo">
                <span /><span /><span />
              </p>
            )}
            {!escribiendo && opciones.length > 0 && (
              <div className="chat-chips">
                {opciones.map((o) => (
                  <button key={o} type="button" className={o === OPCION_LOGIN ? "chat-chip-login" : undefined} onClick={() => enviar(o)}>
                    {o}
                  </button>
                ))}
              </div>
            )}
          </div>

          <form
            className="chat-form"
            onSubmit={(e) => {
              e.preventDefault();
              enviar(texto);
            }}
          >
            <input
              ref={inputRef}
              type="text"
              value={texto}
              onChange={(e) => setTexto(e.target.value)}
              placeholder="Escribe tu pregunta"
              aria-label="Escribe tu pregunta"
              maxLength={500}
              autoComplete="off"
            />
            <button type="submit" disabled={!texto.trim() || escribiendo} aria-label="Enviar pregunta">
              <IconoEnviar />
            </button>
          </form>
          <p className="chat-note">Asistente automático. Los precios son referenciales y los consejos, orientativos.</p>
        </section>
      )}

      {!abierto && (
        <div className="chat-launcher-wrap">
          <span className="chat-hint">¿Necesitas ayuda?</span>
          <button type="button" className="chat-launcher" onClick={() => setAbierto(true)} aria-label="Abrir el asistente de AutoSmart">
            <IconoChat />
          </button>
        </div>
      )}
    </>
  );
}

export default ChatBot;
