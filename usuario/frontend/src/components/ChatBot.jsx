import { useEffect, useImperativeHandle, useRef, useState } from "react";
import "./ChatBot.css";

/* ------------------------------------------------------------------
   Asistente AutoSmart
   Versión inicial basada en reglas: reconoce palabras clave y responde
   con información del catálogo. Más adelante se puede conectar a un
   servicio externo sustituyendo la función `responder`.
------------------------------------------------------------------- */

const SALUDO =
  "Hola, soy el asistente de AutoSmart. Puedo orientarte sobre los servicios, el kilometraje y cómo usar la plataforma. ¿Qué necesitas?";

const SUGERENCIAS = [
  "¿Cada cuánto cambio el aceite?",
  "¿Cuándo reviso los frenos?",
  "¿Qué servicios ofrecen?",
  "¿Cómo creo mi cuenta?",
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
    claves: ["hola", "buenas", "buenos dias", "buenas tardes", "buenas noches"],
    respuesta:
      "¡Hola! Cuéntame qué necesitas: servicios, kilometraje o tu cuenta.",
  },
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
    claves: ["servicio", "servicios", "catalogo", "ofrecen", "precio", "precios", "costo", "cuanto cuesta"],
    respuesta:
      "El catálogo incluye alineamiento vehicular, cambio de aceite, revisión de frenos, control de kilometraje, diagnóstico con escáner y revisión de batería y luces, entre otros. Cada servicio muestra su precio base y su duración estimada.",
  },
  {
    claves: ["cuenta", "registro", "registrar", "registrarme", "crear", "unirme", "inscribir"],
    respuesta:
      "Para crear tu cuenta de cliente, entra a «Crear cuenta», escribe tu nombre, tu correo y una contraseña de mínimo 8 caracteres, con una mayúscula y un número. Después podrás iniciar sesión y registrar tus vehículos.",
  },
  {
    claves: ["contrasena", "clave", "olvide", "recuperar", "restablecer"],
    respuesta:
      "Si olvidaste tu contraseña, podrás recuperarla desde el correo que registraste. Esta opción se está habilitando en la plataforma.",
  },
  {
    claves: ["iniciar sesion", "login", "ingresar", "entrar", "acceder"],
    respuesta:
      "Usa el botón «Iniciar sesión» e ingresa tu correo y tu contraseña. Si olvidaste tu contraseña, pronto podrás recuperarla desde esa misma pantalla.",
  },
  {
    claves: ["mecanico", "mecanicos", "taller", "tecnico"],
    respuesta:
      "Los mecánicos que aparecen en AutoSmart son validados antes de mostrarse en la plataforma, para que sepas que estás en buenas manos.",
  },
  {
    claves: ["cita", "agendar", "reservar", "reserva", "turno"],
    respuesta:
      "La reserva de citas en línea todavía no está disponible. Por ahora puedes registrar tus vehículos y consultar el catálogo de servicios.",
  },
  {
    claves: ["seguro", "seguridad", "confiable", "confianza"],
    respuesta:
      "Los mecánicos que aparecen en AutoSmart son validados antes de mostrarse, y el catálogo muestra el precio y la duración de cada servicio antes de que lo solicites.",
  },
  {
    claves: ["gracias", "listo", "perfecto", "genial"],
    respuesta: "¡Con gusto! Si tienes otra duda, aquí estoy.",
  },
];

const FALLBACK =
  "No estoy seguro de haberte entendido. Prueba preguntando por: aceite, frenos, alineamiento, kilometraje, servicios o cómo crear una cuenta.";

function responder(texto) {
  const limpio = ` ${normalizar(texto).replace(/\s+/g, " ").trim()} `;
  for (const regla of REGLAS) {
    if (regla.claves.some((c) => limpio.includes(` ${c} `) || (c.length > 4 && limpio.includes(c)))) {
      return regla.respuesta;
    }
  }
  return FALLBACK;
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
  const [abierto, setAbierto] = useState(false);
  const [mensajes, setMensajes] = useState([{ de: "bot", texto: SALUDO }]);
  const [texto, setTexto] = useState("");
  const [escribiendo, setEscribiendo] = useState(false);
  const listaRef = useRef(null);
  const inputRef = useRef(null);
  const temporizador = useRef(null);

  const enviar = (contenido) => {
    const limpio = contenido.trim();
    if (!limpio) return;
    setMensajes((prev) => [...prev, { de: "usuario", texto: limpio }]);
    setTexto("");
    setEscribiendo(true);
    clearTimeout(temporizador.current);
    temporizador.current = setTimeout(() => {
      setMensajes((prev) => [...prev, { de: "bot", texto: responder(limpio) }]);
      setEscribiendo(false);
    }, 650);
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

  useEffect(() => () => clearTimeout(temporizador.current), []);

  const soloSaludo = mensajes.length === 1;

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
              <p key={i} className={`chat-msg chat-msg-${m.de}`}>
                {m.texto}
              </p>
            ))}
            {escribiendo && (
              <p className="chat-msg chat-msg-bot chat-typing" aria-label="El asistente está escribiendo">
                <span /><span /><span />
              </p>
            )}
            {soloSaludo && (
              <div className="chat-chips">
                {SUGERENCIAS.map((s) => (
                  <button key={s} type="button" onClick={() => enviar(s)}>
                    {s}
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
              maxLength={200}
              autoComplete="off"
            />
            <button type="submit" disabled={!texto.trim()} aria-label="Enviar pregunta">
              <IconoEnviar />
            </button>
          </form>
          <p className="chat-note">Asistente automático. Las recomendaciones son orientativas.</p>
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
