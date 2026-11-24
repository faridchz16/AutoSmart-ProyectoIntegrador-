import { useCallback, useEffect, useRef, useState } from "react";
import {
  listarDerivaciones,
  atenderDerivacion,
  leerConversacion,
  enlaceContacto,
  avisarCambioDerivaciones,
  ESTADOS_DERIVACION,
  MAX_NOTA,
} from "../../services/chatbotAdminService";
import { fechaHora, hoyLima } from "../../utils/fechas";
import Modal from "../../components/admin/Modal";
import Aviso from "../../components/admin/Aviso";
import "../../components/admin/admin-ui.css";
import "./Inventario.css";
import "./Derivaciones.css";

/* T79.3 – Bandeja de derivaciones del chatbot (API de T79.1).
   Cuando un usuario pide "hablar con un asesor", el chatbot registra una derivación con los últimos mensajes.
   - Pendientes (de la más antigua a la más nueva), Atendidas o Todas; se actualiza sola cada minuto
   - Cada derivación muestra quién escribió, cómo contactarlo y la conversación como chat
   - "Marcar como atendida" pide una nota de lo que se hizo (obligatoria) y queda registrado quién la atendió */

const PESTANAS = [
  { valor: "PENDIENTE", texto: "Pendientes" },
  { valor: "ATENDIDA", texto: "Atendidas" },
  { valor: "", texto: "Todas" },
];
const CADA_ACTUALIZACION = 60 * 1000;
const MENSAJES_VISIBLES = 4;

const cuando = (f) => (!f ? "—" : f.slice(0, 10) === hoyLima() ? `hoy, ${f.slice(11, 16)}` : fechaHora(f));

function Conversacion({ resumen }) {
  const [completa, setCompleta] = useState(false);
  const mensajes = leerConversacion(resumen);
  const ocultos = Math.max(0, mensajes.length - MENSAJES_VISIBLES);
  const visibles = completa ? mensajes : mensajes.slice(-MENSAJES_VISIBLES);
  if (!mensajes.length) return <p className="der-sin-resumen">Sin mensajes registrados.</p>;
  return (
    <div className="der-chat">
      {ocultos > 0 && (
        <button type="button" className="der-ver-mas" onClick={() => setCompleta(!completa)} aria-expanded={completa}>
          {completa ? "Ver solo los últimos mensajes" : `Ver ${ocultos} ${ocultos === 1 ? "mensaje anterior" : "mensajes anteriores"}`}
        </button>
      )}
      {visibles.map((m, i) => (
        <p key={i} className={m.emisor === "BOT" ? "der-msg bot" : "der-msg usuario"}>
          <span className="sr-only">{m.emisor === "BOT" ? "Chatbot: " : "Usuario: "}</span>
          {m.texto}
        </p>
      ))}
    </div>
  );
}

function Derivaciones() {
  const [pestana, setPestana] = useState("PENDIENTE");
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, lista: [], error: "" });
  const [porAtender, setPorAtender] = useState(null);
  const [nota, setNota] = useState("");
  const [errorNota, setErrorNota] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [aviso, setAviso] = useState(null);

  const contadorAvisos = useRef(0);
  const avisar = (tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  };
  const cerrarAviso = useCallback(() => setAviso(null), []);

  const clave = `${pestana}|${recarga}`;
  const cargando = resultado.clave !== clave;
  const primeraCarga = resultado.clave === null;

  useEffect(() => {
    let vigente = true;
    listarDerivaciones(pestana)
      .then((lista) => vigente && setResultado({ clave, lista, error: "" }))
      .catch((e) => vigente && setResultado({ clave, lista: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [clave, pestana]);

  useEffect(() => {
    if (pestana !== "PENDIENTE") return undefined;
    const id = setInterval(() => setRecarga((n) => n + 1), CADA_ACTUALIZACION);
    return () => clearInterval(id);
  }, [pestana]);

  const abrir = (d) => {
    setPorAtender(d);
    setNota("");
    setErrorNota("");
  };

  const confirmar = async (e) => {
    e.preventDefault();
    if (!nota.trim()) return setErrorNota("Anota qué respuesta o solución le diste al usuario.");
    if (nota.trim().length > MAX_NOTA) return setErrorNota(`Máximo ${MAX_NOTA} caracteres.`);
    setEnviando(true);
    setErrorNota("");
    try {
      const d = await atenderDerivacion(porAtender.id, nota);
      setResultado((r) => ({
        ...r,
        lista: pestana === "PENDIENTE" ? r.lista.filter((x) => x.id !== d.id) : r.lista.map((x) => (x.id === d.id ? d : x)),
      }));
      setPorAtender(null);
      avisarCambioDerivaciones();
      avisar("ok", `Marcaste como atendida la derivación N.º ${d.id} de ${d.usuario}.`);
    } catch (err) {
      setErrorNota(err.message);
      if (err.status === 409) {
        setRecarga((n) => n + 1);
        avisarCambioDerivaciones();
      }
    } finally {
      setEnviando(false);
    }
  };

  const { lista } = resultado;

  return (
    <section className="gestion der sol">
      <div className="gestion-cabecera">
        <div>
          <h2>Derivaciones del chatbot</h2>
          <p>
            Usuarios que pidieron hablar con un asesor.
            {!primeraCarga && !resultado.error && pestana === "PENDIENTE" && (
              <>
                {" "}
                <strong>{lista.length}</strong> {lista.length === 1 ? "pendiente" : "pendientes"} de atención.
              </>
            )}
          </p>
        </div>
        <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)} disabled={cargando}>
          Actualizar
        </button>
      </div>

      <div className="filtros">
        <div className="segmentos" role="group" aria-label="Mostrar derivaciones">
          {PESTANAS.map((p) => (
            <button
              key={p.valor || "todas"}
              type="button"
              className={pestana === p.valor ? "activo" : ""}
              aria-pressed={pestana === p.valor}
              onClick={() => setPestana(p.valor)}
            >
              {p.texto}
            </button>
          ))}
        </div>
      </div>

      <div className={cargando && !primeraCarga ? "tabla-caja actualizando" : "tabla-caja"} aria-busy={cargando}>
        {resultado.error ? (
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudieron cargar las derivaciones</p>
            <p>{resultado.error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        ) : primeraCarga ? (
          <div className="tabla-estado">
            <p>Cargando derivaciones...</p>
          </div>
        ) : lista.length === 0 ? (
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">{pestana === "PENDIENTE" ? "No hay derivaciones pendientes" : "Aún no hay derivaciones"}</p>
            <p>Cuando alguien pida hablar con un asesor en el chatbot de la web o de la app, aparecerá aquí.</p>
          </div>
        ) : (
          <ul className="der-lista">
            {lista.map((d) => {
              const enlace = enlaceContacto(d.contacto);
              return (
                <li key={d.id} className={`der-item der-${d.estado}`}>
                  <div className="der-cab">
                    <span className="der-num">N.º {d.id}</span>
                    <strong>{d.usuario}</strong>
                    <span className="der-fecha">{cuando(d.fecha)}</span>
                    {pestana !== "PENDIENTE" && (
                      <span className={`sol-estado sol-${d.estado === "ATENDIDA" ? "APROBADA" : "PENDIENTE"}`}>{ESTADOS_DERIVACION[d.estado]}</span>
                    )}
                  </div>
                  <p className="der-contacto">
                    Contacto:{" "}
                    {d.contacto ? (
                      enlace ? (
                        <a href={enlace}>{d.contacto}</a>
                      ) : (
                        d.contacto
                      )
                    ) : (
                      <span className="der-sin-contacto">no dejó datos; espera que vuelva a escribir al chat</span>
                    )}
                  </p>
                  <Conversacion resumen={d.resumen} />
                  {d.estado === "PENDIENTE" ? (
                    <div className="der-pie">
                      <button type="button" className="btn btn-pri" onClick={() => abrir(d)}>
                        Marcar como atendida
                      </button>
                    </div>
                  ) : (
                    <p className="der-atendida">
                      <strong>Atendida por {d.atendidoPor || "—"}</strong> · {cuando(d.fechaAtencion)}
                      {d.nota && <span>«{d.nota}»</span>}
                    </p>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </div>

      <Modal abierto={!!porAtender} alCerrar={() => !enviando && setPorAtender(null)} titulo="Marcar como atendida" pequeno>
        {porAtender && (
          <form className="confirmar" onSubmit={confirmar} noValidate>
            <p>
              Derivación N.º {porAtender.id} de <strong>{porAtender.usuario}</strong>
              {porAtender.contacto && ` (${porAtender.contacto})`}. Anota cómo la resolviste: quedará registrado con tu usuario.
            </p>
            <div className="campo">
              <label htmlFor="der-nota">Nota de atención</label>
              <textarea
                id="der-nota"
                rows={3}
                maxLength={MAX_NOTA}
                autoFocus
                placeholder="Ej. Lo llamé y reprogramamos su cita para el jueves a las 10:00."
                value={nota}
                aria-invalid={!!errorNota}
                onChange={(e) => {
                  setNota(e.target.value);
                  setErrorNota("");
                }}
              />
              <span className="campo-ayuda">
                {nota.length}/{MAX_NOTA}
              </span>
            </div>
            {errorNota && (
              <p className="form-error-general" role="alert">
                {errorNota}
              </p>
            )}
            <div className="form-acciones">
              <button type="button" className="btn btn-sec" onClick={() => setPorAtender(null)} disabled={enviando}>
                Cancelar
              </button>
              <button type="submit" className="btn btn-pri" disabled={enviando}>
                {enviando && <span className="btn-spinner" aria-hidden="true" />}
                {enviando ? "Guardando..." : "Marcar como atendida"}
              </button>
            </div>
          </form>
        )}
      </Modal>

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Derivaciones;
