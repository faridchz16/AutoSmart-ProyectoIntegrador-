import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";
import ChatBot from "../components/ChatBot";
import "./Home.css";

/* ---------- Utilidades ---------- */
const prefiereMenosMovimiento = () =>
  typeof window !== "undefined" &&
  window.matchMedia("(prefers-reduced-motion: reduce)").matches;

// Da formato a los miles con un espacio fino: 48 200
const formatoMiles = (n) => String(Math.round(n)).replace(/\B(?=(\d{3})+(?!\d))/g, " ");

// Muestra el contenido con una animación suave cuando entra en pantalla
function Reveal({ children, delay = 0, as: Tag = "div", className = "" }) {
  const ref = useRef(null);
  const [visto, setVisto] = useState(
    () => typeof window === "undefined" || !("IntersectionObserver" in window) || prefiereMenosMovimiento()
  );

  useEffect(() => {
    const el = ref.current;
    if (visto || !el) return;
    const io = new IntersectionObserver(
      ([entrada]) => {
        if (entrada.isIntersecting) {
          setVisto(true);
          io.disconnect();
        }
      },
      { threshold: 0.12 }
    );
    io.observe(el);
    return () => io.disconnect();
  }, [visto]);

  return (
    <Tag ref={ref} className={`reveal ${visto ? "reveal-on" : ""} ${className}`} style={{ "--d": `${delay}ms` }}>
      {children}
    </Tag>
  );
}

// Cuenta de 0 al número indicado (odómetro)
function useContador(objetivo, duracion = 1800) {
  const [valor, setValor] = useState(() => (prefiereMenosMovimiento() ? objetivo : 0));
  useEffect(() => {
    if (prefiereMenosMovimiento()) return;
    let raf;
    const inicio = performance.now() + 500;
    const paso = (ahora) => {
      const t = Math.min(Math.max((ahora - inicio) / duracion, 0), 1);
      setValor(objetivo * (1 - Math.pow(1 - t, 3)));
      if (t < 1) raf = requestAnimationFrame(paso);
    };
    raf = requestAnimationFrame(paso);
    return () => cancelAnimationFrame(raf);
  }, [objetivo, duracion]);
  return valor;
}

/* ---------- Iconos (SVG en línea) ---------- */
const Svg = ({ children, size = 26 }) => (
  <svg viewBox="0 0 24 24" width={size} height={size} fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {children}
  </svg>
);

const ICONOS = {
  alineamiento: (
    <Svg>
      <circle cx="12" cy="12" r="8" />
      <circle cx="12" cy="12" r="2.2" />
      <path d="M12 4v5.8M12 14.2V20M4 12h5.8M14.2 12H20" />
    </Svg>
  ),
  aceite: (
    <Svg>
      <path d="M12 3s6 6.4 6 11a6 6 0 0 1-12 0c0-4.6 6-11 6-11Z" />
      <path d="M9.5 14.5a2.5 2.5 0 0 0 2.5 2.5" />
    </Svg>
  ),
  frenos: (
    <Svg>
      <circle cx="12" cy="12" r="9" />
      <circle cx="12" cy="12" r="3" />
      <path d="M12 3.5v2M12 18.5v2M3.5 12h2M18.5 12h2" />
    </Svg>
  ),
  kilometraje: (
    <Svg>
      <path d="M4 17a8 8 0 1 1 16 0" />
      <path d="m12 17 4-5" />
      <path d="M7 17h.01M17 17h.01" />
    </Svg>
  ),
  diagnostico: (
    <Svg>
      <path d="M3 12h4l2.2-5.5 4 11 2.3-5.5H21" />
    </Svg>
  ),
  bateria: (
    <Svg>
      <rect x="3" y="7" width="16" height="11" rx="2" />
      <path d="M21 11v3M7.5 12.5h4M9.5 10.5v4" />
    </Svg>
  ),
  check: (
    <Svg size={18}>
      <path d="m5 12.5 4.5 4.5L19 7.5" />
    </Svg>
  ),
  escudo: (
    <Svg>
      <path d="M12 3 5 6v5.5c0 4.4 2.9 7.8 7 9.5 4.1-1.7 7-5.1 7-9.5V6l-7-3Z" />
      <path d="m9 12 2.2 2.2L15.5 10" />
    </Svg>
  ),
  mecanico: (
    <Svg>
      <path d="M14.7 6.3a4 4 0 0 0-5.4 5.2L3.5 17.3a1.8 1.8 0 0 0 2.5 2.5l5.8-5.8a4 4 0 0 0 5.2-5.4l-2.5 2.5-2.2-.5-.5-2.2 2.4-2.1Z" />
    </Svg>
  ),
  precio: (
    <Svg>
      <path d="M12 3v18M16.5 7.5c-.8-1.2-2.4-2-4.5-2-2.6 0-4 1.2-4 2.9 0 4.2 9 1.9 9 6.2 0 1.8-1.7 3-4.5 3-2.2 0-3.9-.8-4.8-2.2" />
    </Svg>
  ),
  usuarios: (
    <Svg>
      <circle cx="9" cy="8" r="3.2" />
      <path d="M3 19c.6-3.2 3-5 6-5s5.4 1.8 6 5" />
      <path d="M16.5 5.2a3 3 0 0 1 0 5.6M18 14.3c1.7.6 2.8 2.2 3 4.7" />
    </Svg>
  ),
  auto: (
    <Svg>
      <path d="M5 16v-4l2-5h10l2 5v4" />
      <path d="M3 16h18v2.5H3z" />
      <path d="M7.5 13h.01M16.5 13h.01" />
    </Svg>
  ),
  chat: (
    <Svg>
      <path d="M21 11.5a8.4 8.4 0 0 1-8.5 8.3 8.7 8.7 0 0 1-3.6-.8L3 20l1.2-4.6A8.1 8.1 0 0 1 3.5 11.5 8.4 8.4 0 0 1 12 3.2a8.4 8.4 0 0 1 9 8.3Z" />
      <path d="M8 11h.01M12 11h.01M16 11h.01" />
    </Svg>
  ),
};

/* ---------- Contenido ---------- */
const PALABRAS = ["al día.", "con historial.", "bajo control.", "en buenas manos."];

const SERVICIOS = [
  {
    icono: "alineamiento",
    color: "#1150e8",
    nombre: "Alineamiento y balanceo",
    detalle: "Corrige la dirección y reduce el desgaste irregular de las llantas.",
    referencia: "Cada 10 000 km",
  },
  {
    icono: "aceite",
    color: "#d97706",
    nombre: "Cambio de aceite y filtro",
    detalle: "Mantiene el motor lubricado y protegido frente al desgaste.",
    referencia: "Cada 5 000 a 10 000 km",
  },
  {
    icono: "frenos",
    color: "#e11d48",
    nombre: "Revisión de frenos",
    detalle: "Controla pastillas, discos y líquido para frenar con seguridad.",
    referencia: "Una vez al año o cada 10 000 km",
  },
  {
    icono: "kilometraje",
    color: "#0891b2",
    nombre: "Control de kilometraje",
    detalle: "Registra el recorrido de tu vehículo para anticipar cada mantenimiento.",
    referencia: "Siempre activo",
  },
  {
    icono: "diagnostico",
    color: "#7c3aed",
    nombre: "Diagnóstico con escáner",
    detalle: "Lee los códigos de falla del auto y detecta problemas a tiempo.",
    referencia: "Si se enciende una luz de alerta",
  },
  {
    icono: "bateria",
    color: "#059669",
    nombre: "Batería y luces",
    detalle: "Verifica la carga de la batería y el estado de las luces.",
    referencia: "Cada 6 meses",
  },
];

const PASOS = [
  { titulo: "Crea tu cuenta", texto: "Regístrate con tu correo en menos de un minuto." },
  { titulo: "Registra tu vehículo", texto: "Añade tu auto con su placa y su kilometraje." },
  { titulo: "Consulta los servicios", texto: "Revisa el catálogo con el precio y la duración de cada servicio." },
];

const SENALES = [
  {
    sintoma: "Chilla o vibra al frenar",
    servicio: "Revisión de frenos",
    icono: "frenos",
    color: "#e11d48",
    urgencia: "Conviene revisarlo cuanto antes",
    texto: "Puede indicar pastillas gastadas o discos con desgaste. Los frenos son clave para tu seguridad, así que no esperes al próximo mantenimiento.",
    revisa: ["Pastillas y discos", "Líquido de frenos", "Respuesta del pedal"],
    pregunta: "¿Cuándo reviso los frenos?",
  },
  {
    sintoma: "El volante vibra o el auto se desvía",
    servicio: "Alineamiento y balanceo",
    icono: "alineamiento",
    color: "#1150e8",
    urgencia: "Conviene revisarlo pronto",
    texto: "Suele deberse a llantas desalineadas o desbalanceadas. Corregirlo a tiempo evita un desgaste irregular de las llantas.",
    revisa: ["Ángulos de las ruedas", "Balanceo de las llantas", "Desgaste de las llantas"],
    pregunta: "¿Cada cuánto alineo y balanceo?",
  },
  {
    sintoma: "Se enciende la luz del aceite",
    servicio: "Cambio de aceite y filtro",
    icono: "aceite",
    color: "#d97706",
    urgencia: "Conviene revisarlo cuanto antes",
    texto: "Revisa el nivel de aceite y evita manejar por mucho tiempo con la luz encendida. Un cambio a tiempo protege el motor.",
    revisa: ["Nivel y estado del aceite", "Filtro de aceite", "Fugas visibles"],
    pregunta: "¿Cada cuánto cambio el aceite?",
  },
  {
    sintoma: "Una luz de alerta en el tablero",
    servicio: "Diagnóstico con escáner",
    icono: "diagnostico",
    color: "#7c3aed",
    urgencia: "Conviene revisarlo pronto",
    texto: "El escáner lee los códigos de falla y ubica el problema antes de que se agrande.",
    revisa: ["Lectura de códigos de falla", "Sensores principales", "Informe del resultado"],
    pregunta: "¿Qué hace el diagnóstico con escáner?",
  },
  {
    sintoma: "Cuesta arrancar o las luces se ven débiles",
    servicio: "Batería y luces",
    icono: "bateria",
    color: "#059669",
    urgencia: "Conviene revisarlo pronto",
    texto: "Puede ser una batería con poca carga o un problema en las conexiones. Revisarla evita quedarte sin arranque.",
    revisa: ["Carga de la batería", "Bornes y conexiones", "Luces delanteras y traseras"],
    pregunta: "¿Cada cuánto reviso la batería?",
  },
  {
    sintoma: "No recuerdo cuándo toca el mantenimiento",
    servicio: "Control de kilometraje",
    icono: "kilometraje",
    color: "#0891b2",
    urgencia: "Puedes empezar cuando quieras",
    texto: "Registra el recorrido de tu auto y lleva el control de cada servicio, sin depender de la memoria.",
    revisa: ["Registro del recorrido", "Servicios pendientes", "Ficha de tu vehículo"],
    pregunta: "¿Cómo funciona el control de kilometraje?",
  },
];

const CONFIANZA = [
  {
    icono: "mecanico",
    titulo: "Mecánicos verificados",
    texto: "Cada profesional fue validado antes de aparecer en la plataforma.",
  },
  {
    icono: "chat",
    titulo: "Un asistente a tu lado",
    texto: "Resuelve tus dudas sobre servicios y kilometraje a cualquier hora, sin esperar.",
  },
  {
    icono: "precio",
    titulo: "Precios y tiempos claros",
    texto: "El catálogo muestra el precio base y la duración de cada servicio antes de que lo solicites.",
  },
  {
    icono: "auto",
    titulo: "Tu información, ordenada",
    texto: "Tus vehículos quedan registrados con su placa, para que no tengas que repetir datos.",
  },
];

const PREGUNTAS = [
  "¿Cada cuánto cambio el aceite?",
  "¿Cuándo reviso los frenos?",
  "¿Cada cuánto alineo y balanceo?",
  "¿Cómo creo mi cuenta?",
];

function Home() {
  const chatRef = useRef(null);
  const heroRef = useRef(null);
  const fichaRef = useRef(null);
  const progresoRef = useRef(null);
  const [senal, setSenal] = useState(0);
  const [palabra, setPalabra] = useState(0);
  const [scrolled, setScrolled] = useState(false);
  const km = useContador(48200);
  const actual = SENALES[senal];

  // Palabra que rota en el titular
  useEffect(() => {
    if (prefiereMenosMovimiento()) return;
    const id = setInterval(() => setPalabra((p) => (p + 1) % PALABRAS.length), 2800);
    return () => clearInterval(id);
  }, []);

  // Barra de progreso de lectura y encabezado compacto al hacer scroll
  useEffect(() => {
    const alScroll = () => {
      const max = document.documentElement.scrollHeight - window.innerHeight;
      const avance = max > 0 ? window.scrollY / max : 0;
      if (progresoRef.current) progresoRef.current.style.transform = `scaleX(${avance})`;
      setScrolled(window.scrollY > 24);
    };
    alScroll();
    window.addEventListener("scroll", alScroll, { passive: true });
    return () => window.removeEventListener("scroll", alScroll);
  }, []);

  // Luz que sigue al mouse en el hero y ficha que se inclina
  const alMoverHero = (e) => {
    if (prefiereMenosMovimiento()) return;
    const hero = heroRef.current;
    const caja = hero.getBoundingClientRect();
    hero.style.setProperty("--mx", `${e.clientX - caja.left}px`);
    hero.style.setProperty("--my", `${e.clientY - caja.top}px`);

    const ficha = fichaRef.current;
    if (!ficha) return;
    const f = ficha.getBoundingClientRect();
    const dx = (e.clientX - (f.left + f.width / 2)) / (window.innerWidth / 2);
    const dy = (e.clientY - (f.top + f.height / 2)) / (window.innerHeight / 2);
    ficha.style.transform = `perspective(900px) rotateY(${dx * 7}deg) rotateX(${-dy * 6}deg)`;
  };
  const alSalirHero = () => {
    if (fichaRef.current) fichaRef.current.style.transform = "";
  };

  return (
    <div className="home">
      <a className="home-skip" href="#contenido">Saltar al contenido</a>
      <div className="progreso" aria-hidden="true"><i ref={progresoRef} /></div>

      {/* ---------- Encabezado ---------- */}
      <header className={scrolled ? "home-header home-header-compacto" : "home-header"}>
        <div className="wrap home-header-in">
          <Link to="/" className="home-brand" aria-label="AutoSmart, inicio">
            <img src="/logo.png" alt="" width="40" height="40" />
            <span>AutoSmart</span>
          </Link>

          <nav className="home-nav" aria-label="Principal">
            <a href="#servicios">Servicios</a>
            <a href="#como-funciona">Cómo funciona</a>
            <a href="#senales">Señales</a>
            <a href="#confianza">Confianza</a>
            <a href="#asistente">Asistente</a>
          </nav>

          <div className="home-actions">
            <Link to="/login" className="btn btn-ghost">Iniciar sesión</Link>
            <Link to="/registro" className="btn btn-solid">Crear cuenta</Link>
          </div>
        </div>
      </header>

      <main id="contenido">
        {/* ---------- Hero ---------- */}
        <section className="hero" ref={heroRef} onMouseMove={alMoverHero} onMouseLeave={alSalirHero}>
          <div className="hero-luz" aria-hidden="true" />
          <span className="blob blob-a" aria-hidden="true" />
          <span className="blob blob-b" aria-hidden="true" />

          <div className="wrap hero-in">
            <div className="hero-copy">
              <h1>
                <span className="h1-linea">Tu auto, siempre</span>
                <span className="h1-rot" key={palabra}>{PALABRAS[palabra]}</span>
              </h1>
              <p className="hero-lead">
                AutoSmart reúne tus vehículos, los servicios y el control de kilometraje en un solo lugar.
                Con mecánicos verificados y un asistente integrado que te responde al instante.
              </p>

              <div className="hero-cta">
                <Link to="/registro" className="btn btn-solid btn-lg">Crear cuenta</Link>
                <button type="button" className="btn btn-outline btn-lg" onClick={() => chatRef.current?.abrir()}>
                  {ICONOS.chat} Hablar con el asistente
                </button>
              </div>

              <ul className="hero-checks">
                <li>{ICONOS.check} Mecánicos verificados</li>
                <li>{ICONOS.check} Catálogo con precios claros</li>
                <li>{ICONOS.check} Asistente integrado</li>
              </ul>
            </div>

            {/* Ficha de ejemplo del vehículo */}
            <div className="ficha-wrap">
              <aside className="ficha" ref={fichaRef} aria-label="Ejemplo de ficha de un vehículo">
                <div className="ficha-top">
                  <span className="placa" aria-label="Placa ABC-123">
                    <small>PERÚ</small>
                    ABC-123
                  </span>
                  <div className="ficha-auto">
                    <strong>Sedán</strong>
                    <span className="odometro">{formatoMiles(km)} km</span>
                  </div>
                </div>

                <ul className="ficha-lista">
                  <li>
                    <div className="ficha-fila">
                      <span>Cambio de aceite</span>
                      <b className="est-aviso">Faltan 1 800 km</b>
                    </div>
                    <div className="barra"><i style={{ "--w": "82%" }} className="barra-aviso" /></div>
                  </li>
                  <li>
                    <div className="ficha-fila">
                      <span>Revisión de frenos</span>
                      <b className="est-ok">Al día</b>
                    </div>
                    <div className="barra"><i style={{ "--w": "30%", "--delay": "0.45s" }} className="barra-ok" /></div>
                  </li>
                  <li>
                    <div className="ficha-fila">
                      <span>Alineamiento</span>
                      <b className="est-ok">Al día</b>
                    </div>
                    <div className="barra"><i style={{ "--w": "45%", "--delay": "0.65s" }} className="barra-ok" /></div>
                  </li>
                </ul>
                <p className="ficha-nota">Vista de ejemplo</p>
              </aside>
            </div>
          </div>

          {/* Línea de pulso del logo */}
          <svg className="pulso" viewBox="0 0 1200 120" preserveAspectRatio="none" aria-hidden="true">
            <path className="pulso-base" d="M0 70 H330 l22 -48 l34 92 l28 -70 l16 26 H760 l22 -48 l34 92 l28 -70 l16 26 H1200" />
            <path className="pulso-mov" d="M0 70 H330 l22 -48 l34 92 l28 -70 l16 26 H760 l22 -48 l34 92 l28 -70 l16 26 H1200" />
          </svg>
        </section>

        {/* ---------- Cinta de servicios ---------- */}
        <div className="cinta" aria-hidden="true">
          <div className="cinta-pista">
            {[...SERVICIOS, ...SERVICIOS, ...SERVICIOS, ...SERVICIOS].map((s, i) => (
              <span key={i} className="cinta-item">
                {ICONOS[s.icono]} {s.nombre}
              </span>
            ))}
          </div>
        </div>

        {/* ---------- Servicios ---------- */}
        <section className="section" id="servicios">
          <div className="wrap">
            <Reveal className="section-cab">
              <h2>Los servicios que tu auto necesita</h2>
              <p className="section-lead">
                Desde el cambio de aceite hasta el control de kilometraje. Cada servicio muestra su
                precio base y su duración.
              </p>
            </Reveal>

            <ul className="servicios">
              {SERVICIOS.map((s, i) => (
                <Reveal as="li" key={s.nombre} delay={i * 70}>
                  <article className="serv" style={{ "--acc": s.color }}>
                    <span className="serv-icono">{ICONOS[s.icono]}</span>
                    <h3>{s.nombre}</h3>
                    <p>{s.detalle}</p>
                    <span className="serv-ref">{s.referencia}</span>
                  </article>
                </Reveal>
              ))}
            </ul>
            <p className="muted nota-servicios">Los intervalos son una referencia general. Sigue siempre el manual de tu vehículo.</p>
          </div>
        </section>

        {/* ---------- Cómo funciona ---------- */}
        <section className="section section-sky" id="como-funciona">
          <div className="wrap">
            <Reveal><h2 className="center">Empieza en tres pasos</h2></Reveal>
            <ol className="pasos">
              {PASOS.map((p, i) => (
                <Reveal as="li" key={p.titulo} delay={i * 120} className="paso">
                  <h3>{p.titulo}</h3>
                  <p>{p.texto}</p>
                </Reveal>
              ))}
            </ol>
          </div>
        </section>

        {/* ---------- Señales del auto ---------- */}
        <section className="section" id="senales">
          <div className="wrap">
            <Reveal className="section-cab">
              <h2>¿Tu auto te está avisando algo?</h2>
              <p className="section-lead">Elige lo que notas y te mostramos qué servicio le corresponde.</p>
            </Reveal>

            <Reveal className="senales">
              <div className="sen-lista" role="tablist" aria-label="Señales de tu auto" aria-orientation="vertical">
                {SENALES.map((s, i) => (
                  <button
                    key={s.sintoma}
                    type="button"
                    role="tab"
                    id={`sen-${i}`}
                    aria-selected={senal === i}
                    aria-controls="sen-panel"
                    className={senal === i ? "sen-btn sen-on" : "sen-btn"}
                    style={{ "--acc": s.color }}
                    onClick={() => setSenal(i)}
                  >
                    <span className="sen-ico">{ICONOS[s.icono]}</span>
                    <span>{s.sintoma}</span>
                  </button>
                ))}
              </div>

              <div
                className="sen-panel"
                key={senal}
                role="tabpanel"
                id="sen-panel"
                aria-labelledby={`sen-${senal}`}
                style={{ "--acc": actual.color }}
              >
                <span className="sen-marca" aria-hidden="true">{ICONOS[actual.icono]}</span>
                <span className="sen-urgencia">{actual.urgencia}</span>
                <h3>{actual.servicio}</h3>
                <p className="sen-texto">{actual.texto}</p>
                <p className="sen-sub">Qué revisamos:</p>
                <ul className="check-lista">
                  {actual.revisa.map((t) => (
                    <li key={t}>{ICONOS.check}<span>{t}</span></li>
                  ))}
                </ul>
                <div className="sen-acciones">
                  <button type="button" className="btn btn-blanco" onClick={() => chatRef.current?.preguntar(actual.pregunta)}>
                    {ICONOS.chat} Preguntar al asistente
                  </button>
                  <a href="#servicios" className="btn btn-outline">Ver servicios</a>
                </div>
              </div>
            </Reveal>
            <p className="muted nota-servicios">Si sientes que tu seguridad está en riesgo, detén el auto en un lugar seguro y pide asistencia.</p>
          </div>
        </section>

        {/* ---------- Confianza ---------- */}
        <section className="section section-ink" id="confianza">
          <img className="marca-agua" src="/logo-white.png" alt="" aria-hidden="true" />
          <div className="wrap">
            <Reveal className="section-cab">
              <h2>Una plataforma en la que puedes confiar</h2>
              <p className="section-lead">Así cuidamos tu experiencia en cada paso.</p>
            </Reveal>
            <ul className="confianza">
              {CONFIANZA.map((c, i) => (
                <Reveal as="li" key={c.titulo} delay={i * 90}>
                  <article className="conf">
                    <span className="conf-icono">{ICONOS[c.icono]}</span>
                    <h3>{c.titulo}</h3>
                    <p>{c.texto}</p>
                  </article>
                </Reveal>
              ))}
            </ul>
          </div>
        </section>

        {/* ---------- Asistente ---------- */}
        <section className="section" id="asistente">
          <div className="wrap">
            <Reveal className="asistente">
              <div className="asistente-texto">
                <span className="asistente-icono">{ICONOS.chat}</span>
                <h2>Resuelve tus dudas sin esperar</h2>
                <p className="section-lead">
                  El asistente integrado responde sobre servicios, kilometraje y el uso de la plataforma, a cualquier hora.
                  Elige una pregunta o escribe la tuya.
                </p>
              </div>
              <ul className="asistente-chips">
                {PREGUNTAS.map((q) => (
                  <li key={q}>
                    <button type="button" onClick={() => chatRef.current?.preguntar(q)}>{q}</button>
                  </li>
                ))}
              </ul>
            </Reveal>
          </div>
        </section>

        {/* ---------- Llamado final ---------- */}
        <section className="final">
          <Reveal className="wrap final-in">
            <h2>Cuida tu auto desde hoy</h2>
            <p>Crea tu cuenta, registra tu vehículo y lleva el control de cada servicio.</p>
            <div className="hero-cta final-cta">
              <Link to="/registro" className="btn btn-blanco btn-lg">Crear cuenta</Link>
              <Link to="/login" className="btn btn-outline btn-lg">Iniciar sesión</Link>
            </div>
          </Reveal>
        </section>
      </main>

      {/* ---------- Pie ---------- */}
      <footer className="home-footer">
        <div className="wrap footer-in">
          <div className="footer-brand">
            <img src="/logo-white.png" alt="" width="36" height="36" />
            <span>AutoSmart</span>
          </div>
          <p>Mantenimiento vehicular con mecánicos verificados.</p>
          <nav aria-label="Pie de página">
            <Link to="/login">Iniciar sesión</Link>
            <Link to="/registro">Crear cuenta</Link>
          </nav>
        </div>
        <p className="footer-copy">© 2026 AutoSmart. Todos los derechos reservados.</p>
      </footer>

      <ChatBot ref={chatRef} />
    </div>
  );
}

export default Home;
