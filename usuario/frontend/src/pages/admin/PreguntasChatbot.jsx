import { useCallback, useEffect, useRef, useState } from "react";
import {
  listarFaq,
  crearFaq,
  editarFaq,
  desactivarFaq,
  probarChatbot,
  separarPalabras,
  MAX_PREGUNTA,
  MAX_RESPUESTA,
  MAX_PALABRAS,
} from "../../services/chatbotAdminService";
import { fechaHora } from "../../utils/fechas";
import Modal from "../../components/admin/Modal";
import Aviso from "../../components/admin/Aviso";
import "../../components/admin/admin-ui.css";
import "./Derivaciones.css";
import "./PreguntasChatbot.css";

/* T80.2 – Base de conocimiento del chatbot (API de T80.1).
   - Preguntas frecuentes activas o desactivadas, con búsqueda en pregunta, respuesta o palabras clave
   - Crear y editar: pregunta, respuesta y palabras clave (separadas por comas), con vista previa de la respuesta
   - Desactivar con confirmación; como el backend no permite reactivar, "Usar de nuevo" la copia como pregunta nueva
   - "Probar el chatbot": envía un mensaje y muestra qué responde, para comprobar las palabras clave */

const VISTAS = [
  { valor: "activas", texto: "Activas" },
  { valor: "inactivas", texto: "Desactivadas" },
  { valor: "todas", texto: "Todas" },
];

const VACIA = { pregunta: "", respuesta: "", palabrasClave: "" };
const sinTildes = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();

function FormFaq({ faq, copia, alGuardar, alCerrar }) {
  const [datos, setDatos] = useState(() => {
    const origen = faq ?? copia;
    return origen ? { pregunta: origen.pregunta, respuesta: origen.respuesta, palabrasClave: origen.palabrasClave } : VACIA;
  });
  const [errores, setErrores] = useState({});
  const [errorGeneral, setErrorGeneral] = useState("");
  const [enviando, setEnviando] = useState(false);

  const cambiar = (campo) => (e) => {
    setDatos((d) => ({ ...d, [campo]: e.target.value }));
    setErrores((x) => ({ ...x, [campo]: undefined }));
  };

  const validar = () => {
    const e = {};
    if (!datos.pregunta.trim()) e.pregunta = "Escribe la pregunta tal como la haría un cliente.";
    else if (datos.pregunta.trim().length > MAX_PREGUNTA) e.pregunta = `Máximo ${MAX_PREGUNTA} caracteres.`;
    if (!datos.respuesta.trim()) e.respuesta = "Escribe la respuesta que dará el chatbot.";
    else if (datos.respuesta.trim().length > MAX_RESPUESTA) e.respuesta = `Máximo ${MAX_RESPUESTA} caracteres.`;
    if (separarPalabras(datos.palabrasClave).join(", ").length > MAX_PALABRAS) e.palabrasClave = `Máximo ${MAX_PALABRAS} caracteres en total.`;
    return e;
  };

  const enviar = async (ev) => {
    ev.preventDefault();
    const e = validar();
    setErrores(e);
    setErrorGeneral("");
    if (Object.keys(e).length) return;
    setEnviando(true);
    try {
      const guardada = faq ? await editarFaq(faq.id, datos) : await crearFaq(datos);
      alGuardar(guardada, !faq);
    } catch (err) {
      setErrorGeneral(err.message);
    } finally {
      setEnviando(false);
    }
  };

  const palabras = separarPalabras(datos.palabrasClave);

  return (
    <form className="adm-form faq-form" onSubmit={enviar} noValidate>
      {faq && !faq.activa && (
        <p className="aviso-fijo">Esta pregunta está desactivada: puedes corregirla, pero el chatbot no la usará.</p>
      )}
      <div className="campo">
        <label htmlFor="faq-pregunta">Pregunta</label>
        <input
          id="faq-pregunta"
          autoFocus
          maxLength={MAX_PREGUNTA}
          placeholder="Ej. ¿Aceptan Yape o Plin?"
          value={datos.pregunta}
          aria-invalid={!!errores.pregunta}
          onChange={cambiar("pregunta")}
        />
        {errores.pregunta && <p className="campo-error">{errores.pregunta}</p>}
      </div>

      <div className="campo">
        <label htmlFor="faq-respuesta">Respuesta del chatbot</label>
        <textarea
          id="faq-respuesta"
          rows={4}
          maxLength={MAX_RESPUESTA}
          placeholder="Ej. Sí. Puedes pagar con Yape o Plin desde la app, con tarjeta o en efectivo en el taller."
          value={datos.respuesta}
          aria-invalid={!!errores.respuesta}
          onChange={cambiar("respuesta")}
        />
        <span className="campo-ayuda">
          {datos.respuesta.length}/{MAX_RESPUESTA}
        </span>
        {errores.respuesta && <p className="campo-error">{errores.respuesta}</p>}
      </div>

      <div className="campo">
        <label htmlFor="faq-palabras">
          Palabras clave <span className="opcional">(separadas por comas)</span>
        </label>
        <input
          id="faq-palabras"
          maxLength={MAX_PALABRAS + 40}
          placeholder="Ej. yape, plin, pago, pagar"
          value={datos.palabrasClave}
          aria-invalid={!!errores.palabrasClave}
          onChange={cambiar("palabrasClave")}
        />
        <span className="campo-ayuda">
          El chatbot da esta respuesta cuando el mensaje del cliente contiene alguna de estas palabras.
        </span>
        {palabras.length > 0 && (
          <ul className="faq-chips" aria-label="Palabras clave">
            {palabras.map((p) => (
              <li key={p}>{p}</li>
            ))}
          </ul>
        )}
        {errores.palabrasClave && <p className="campo-error">{errores.palabrasClave}</p>}
      </div>

      {datos.respuesta.trim() && (
        <div className="faq-previa" aria-label="Vista previa">
          <span>Así responderá el chatbot</span>
          {datos.pregunta.trim() && <p className="der-msg usuario">{datos.pregunta.trim()}</p>}
          <p className="der-msg bot">{datos.respuesta.trim()}</p>
        </div>
      )}

      {errorGeneral && (
        <p className="form-error-general" role="alert">
          {errorGeneral}
        </p>
      )}
      <div className="form-acciones">
        <button type="button" className="btn btn-sec" onClick={alCerrar} disabled={enviando}>
          Cancelar
        </button>
        <button type="submit" className="btn btn-pri" disabled={enviando}>
          {enviando && <span className="btn-spinner" aria-hidden="true" />}
          {enviando ? "Guardando..." : faq ? "Guardar cambios" : "Crear pregunta"}
        </button>
      </div>
    </form>
  );
}

function ProbarChatbot() {
  const [mensaje, setMensaje] = useState("");
  const [estado, setEstado] = useState({ cargando: false, respuesta: null, error: "" });

  const probar = async (e) => {
    e.preventDefault();
    if (!mensaje.trim()) return;
    setEstado({ cargando: true, respuesta: null, error: "" });
    try {
      setEstado({ cargando: false, respuesta: { enviado: mensaje.trim(), ...(await probarChatbot(mensaje)) }, error: "" });
    } catch (err) {
      setEstado({ cargando: false, respuesta: null, error: err.message });
    }
  };

  return (
    <section className="faq-probar" aria-labelledby="faq-probar-titulo">
      <h3 id="faq-probar-titulo">Probar el chatbot</h3>
      <p>Escribe un mensaje como lo haría un cliente y comprueba qué responde.</p>
      <form onSubmit={probar} className="faq-probar-form">
        <label htmlFor="faq-probar" className="sr-only">
          Mensaje de prueba
        </label>
        <input
          id="faq-probar"
          maxLength={500}
          placeholder="Ej. ¿puedo pagar con yape?"
          value={mensaje}
          onChange={(e) => setMensaje(e.target.value)}
        />
        <button type="submit" className="btn btn-sec" disabled={estado.cargando || !mensaje.trim()}>
          {estado.cargando ? "Enviando..." : "Enviar"}
        </button>
      </form>
      {estado.error && <p className="form-error-general">{estado.error}</p>}
      {estado.respuesta && (
        <div className="der-chat faq-probar-chat" role="status">
          <p className="der-msg usuario">{estado.respuesta.enviado}</p>
          <p className="der-msg bot">{estado.respuesta.respuesta}</p>
          <span className="faq-intencion">
            Intención detectada: <strong>{estado.respuesta.intencion || "—"}</strong>
            {estado.respuesta.idFaq && ` · pregunta N.º ${estado.respuesta.idFaq}`}
          </span>
        </div>
      )}
    </section>
  );
}

function PreguntasChatbot() {
  const [vista, setVista] = useState("activas");
  const [busqueda, setBusqueda] = useState("");
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, lista: [], error: "" });
  const [formulario, setFormulario] = useState(null); // { faq } editar · { copia } usar de nuevo · {} nueva
  const [porDesactivar, setPorDesactivar] = useState(null);
  const [desactivando, setDesactivando] = useState(false);
  const [errorDesactivar, setErrorDesactivar] = useState("");
  const [aviso, setAviso] = useState(null);

  const contadorAvisos = useRef(0);
  const avisar = (tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  };
  const cerrarAviso = useCallback(() => setAviso(null), []);

  const cargando = resultado.clave !== recarga;
  const primeraCarga = resultado.clave === null;

  useEffect(() => {
    let vigente = true;
    listarFaq()
      .then((lista) => vigente && setResultado({ clave: recarga, lista, error: "" }))
      .catch((e) => vigente && setResultado({ clave: recarga, lista: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [recarga]);

  const alGuardar = (guardada, nueva) => {
    setResultado((r) => ({
      ...r,
      lista: nueva ? [...r.lista, guardada] : r.lista.map((f) => (f.id === guardada.id ? guardada : f)),
    }));
    setFormulario(null);
    avisar("ok", nueva ? "Se creó la pregunta. El chatbot ya puede usarla." : "Se guardaron los cambios.");
  };

  const desactivar = async () => {
    setDesactivando(true);
    setErrorDesactivar("");
    try {
      const f = await desactivarFaq(porDesactivar.id);
      setResultado((r) => ({ ...r, lista: r.lista.map((x) => (x.id === f.id ? f : x)) }));
      setPorDesactivar(null);
      avisar("ok", "La pregunta se desactivó: el chatbot ya no la usará.");
    } catch (e) {
      setErrorDesactivar(e.message);
      if (e.status === 409) setRecarga((n) => n + 1);
    } finally {
      setDesactivando(false);
    }
  };

  const { lista } = resultado;
  const activas = lista.filter((f) => f.activa).length;
  const texto = sinTildes(busqueda.trim());
  const visibles = lista
    .filter((f) => (vista === "activas" ? f.activa : vista === "inactivas" ? !f.activa : true))
    .filter((f) => !texto || sinTildes(`${f.pregunta} ${f.respuesta} ${f.palabrasClave}`).includes(texto));

  return (
    <section className="gestion faq der">
      <div className="gestion-cabecera">
        <div>
          <h2>Preguntas del chatbot</h2>
          <p>
            Base de conocimiento con la que el chatbot responde en la web y en la app.
            {!primeraCarga && !resultado.error && (
              <>
                {" "}
                <strong>{activas}</strong> {activas === 1 ? "pregunta activa" : "preguntas activas"}.
              </>
            )}
          </p>
        </div>
        <button type="button" className="btn btn-pri" onClick={() => setFormulario({})}>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <path d="M12 5v14M5 12h14" />
          </svg>
          Nueva pregunta
        </button>
      </div>

      <div className="faq-grid">
        <div>
          <div className="filtros">
            <div className="segmentos" role="group" aria-label="Mostrar preguntas">
              {VISTAS.map((v) => (
                <button
                  key={v.valor}
                  type="button"
                  className={vista === v.valor ? "activo" : ""}
                  aria-pressed={vista === v.valor}
                  onClick={() => setVista(v.valor)}
                >
                  {v.texto}
                </button>
              ))}
            </div>
            <label className="buscador faq-buscador">
              <span className="sr-only">Buscar en preguntas, respuestas o palabras clave</span>
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
                <circle cx="11" cy="11" r="7" />
                <path d="m20 20-3.5-3.5" />
              </svg>
              <input type="search" placeholder="Buscar" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
            </label>
          </div>

          <div className={cargando && !primeraCarga ? "tabla-caja actualizando" : "tabla-caja"} aria-busy={cargando}>
            {resultado.error ? (
              <div className="tabla-estado">
                <p className="tabla-estado-titulo">No se pudieron cargar las preguntas</p>
                <p>{resultado.error}</p>
                <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
                  Reintentar
                </button>
              </div>
            ) : primeraCarga ? (
              <div className="tabla-estado">
                <p>Cargando preguntas...</p>
              </div>
            ) : visibles.length === 0 ? (
              <div className="tabla-estado">
                <p className="tabla-estado-titulo">{texto ? "Sin resultados" : vista === "inactivas" ? "No hay preguntas desactivadas" : "Aún no hay preguntas"}</p>
                <p>{texto ? `Ninguna pregunta coincide con «${busqueda.trim()}».` : "Crea la primera con el botón «Nueva pregunta»."}</p>
              </div>
            ) : (
              <ul className="faq-lista">
                {visibles.map((f) => (
                  <li key={f.id} className={f.activa ? "faq-item" : "faq-item inactiva"}>
                    <div className="faq-cab">
                      <strong>{f.pregunta}</strong>
                      {!f.activa && <span className="insignia faq-insignia">Desactivada</span>}
                    </div>
                    <p className="faq-respuesta">{f.respuesta}</p>
                    {separarPalabras(f.palabrasClave).length > 0 ? (
                      <ul className="faq-chips" aria-label="Palabras clave">
                        {separarPalabras(f.palabrasClave).map((p) => (
                          <li key={p}>{p}</li>
                        ))}
                      </ul>
                    ) : (
                      <p className="faq-sin-palabras">Sin palabras clave: solo responde si escriben la pregunta exacta.</p>
                    )}
                    <div className="faq-pie">
                      <span>Actualizada {fechaHora(f.fechaActualizacion || f.fechaCreacion)}</span>
                      <div>
                        {f.activa ? (
                          <>
                            <button type="button" className="btn-icono" onClick={() => setFormulario({ faq: f })}>
                              Editar
                            </button>
                            <button
                              type="button"
                              className="btn-icono faq-desactivar"
                              onClick={() => {
                                setPorDesactivar(f);
                                setErrorDesactivar("");
                              }}
                            >
                              Desactivar
                            </button>
                          </>
                        ) : (
                          <>
                            <button type="button" className="btn-icono" onClick={() => setFormulario({ faq: f })}>
                              Editar
                            </button>
                            <button type="button" className="btn-icono" onClick={() => setFormulario({ copia: f })}>
                              Usar de nuevo
                            </button>
                          </>
                        )}
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>

        <ProbarChatbot />
      </div>

      {/* ---------- Crear / editar ---------- */}
      <Modal
        abierto={!!formulario}
        alCerrar={() => setFormulario(null)}
        titulo={formulario?.faq ? "Editar pregunta" : formulario?.copia ? "Usar de nuevo como pregunta nueva" : "Nueva pregunta"}
      >
        {formulario && (
          <FormFaq faq={formulario.faq} copia={formulario.copia} alGuardar={alGuardar} alCerrar={() => setFormulario(null)} />
        )}
      </Modal>

      {/* ---------- Desactivar ---------- */}
      <Modal abierto={!!porDesactivar} alCerrar={() => !desactivando && setPorDesactivar(null)} titulo="¿Desactivar la pregunta?" pequeno>
        {porDesactivar && (
          <div className="confirmar">
            <p>
              El chatbot dejará de responder <strong>«{porDesactivar.pregunta}»</strong>. No se puede reactivar, pero luego podrás
              usarla de nuevo como una pregunta nueva.
            </p>
            {errorDesactivar && (
              <p className="form-error-general" role="alert">
                {errorDesactivar}
              </p>
            )}
            <div className="form-acciones">
              <button type="button" className="btn btn-sec" onClick={() => setPorDesactivar(null)} disabled={desactivando}>
                Cancelar
              </button>
              <button type="button" className="btn btn-peligro" onClick={desactivar} disabled={desactivando}>
                {desactivando && <span className="btn-spinner" aria-hidden="true" />}
                {desactivando ? "Desactivando..." : "Desactivar"}
              </button>
            </div>
          </div>
        )}
      </Modal>

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default PreguntasChatbot;
