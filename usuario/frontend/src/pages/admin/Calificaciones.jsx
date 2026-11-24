import { useCallback, useEffect, useRef, useState } from "react";
import {
  listarCalificacionesAdmin,
  ocultarComentario,
  formatoPromedio,
  MAX_MOTIVO,
  MOTIVOS_FRECUENTES,
} from "../../services/calificacionesService";
import { fechaHora } from "../../utils/fechas";
import Estrellas from "../../components/Estrellas";
import Modal from "../../components/admin/Modal";
import Aviso from "../../components/admin/Aviso";
import Paginacion from "../../components/admin/Paginacion";
import "../../components/admin/admin-ui.css";
import "./Reservas.css";
import "./Calificaciones.css";

/* T71.2 – Moderación de las calificaciones (API de T71.1).
   - Todas, solo visibles o solo ocultas; filtro por mecánico y por nota; búsqueda en cliente, ticket o comentario
   - "Ocultar" pide el motivo (obligatorio) y oculta solo el comentario: la nota sigue en el promedio del mecánico
   - Las ocultas muestran quién las ocultó, cuándo y por qué (queda registrado en el backend) */

const TAMANO = 8;
const VISTAS = [
  { valor: "", texto: "Todas" },
  { valor: "false", texto: "Visibles" },
  { valor: "true", texto: "Ocultas" },
];

const sinTildes = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();

function Calificaciones() {
  const [vista, setVista] = useState("");
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, lista: [], error: "" });
  const [mecanico, setMecanico] = useState("");
  const [nota, setNota] = useState("");
  const [busqueda, setBusqueda] = useState("");
  const [pagina, setPagina] = useState(0);
  const [porOcultar, setPorOcultar] = useState(null);
  const [motivo, setMotivo] = useState("");
  const [errorMotivo, setErrorMotivo] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [aviso, setAviso] = useState(null);

  const contadorAvisos = useRef(0);
  const avisar = (tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  };
  const cerrarAviso = useCallback(() => setAviso(null), []);

  const clave = `${vista}|${recarga}`;
  const cargando = resultado.clave !== clave;
  const primeraCarga = resultado.clave === null;

  useEffect(() => {
    let vigente = true;
    listarCalificacionesAdmin(vista)
      .then((lista) => vigente && setResultado({ clave, lista, error: "" }))
      .catch((e) => vigente && setResultado({ clave, lista: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [clave, vista]);

  const cambiar = (setter) => (valor) => {
    setter(valor);
    setPagina(0);
  };

  const abrirOcultar = (c) => {
    setPorOcultar(c);
    setMotivo("");
    setErrorMotivo("");
  };

  const confirmarOcultar = async (e) => {
    e.preventDefault();
    if (!motivo.trim()) return setErrorMotivo("Escribe o elige el motivo: quedará registrado.");
    if (motivo.trim().length > MAX_MOTIVO) return setErrorMotivo(`Máximo ${MAX_MOTIVO} caracteres.`);
    setEnviando(true);
    setErrorMotivo("");
    try {
      const actualizada = await ocultarComentario(porOcultar.id, motivo);
      setResultado((r) => ({
        ...r,
        lista:
          vista === "false" ? r.lista.filter((x) => x.id !== actualizada.id) : r.lista.map((x) => (x.id === actualizada.id ? actualizada : x)),
      }));
      setPorOcultar(null);
      avisar("ok", `Se ocultó el comentario de ${actualizada.cliente}. Su nota sigue contando en el promedio de ${actualizada.mecanico}.`);
    } catch (err) {
      setErrorMotivo(err.message);
      if (err.status === 409) setRecarga((n) => n + 1);
    } finally {
      setEnviando(false);
    }
  };

  const { lista } = resultado;
  const mecanicos = [...new Set(lista.map((c) => c.mecanico).filter(Boolean))].sort((a, b) => a.localeCompare(b, "es"));
  const texto = sinTildes(busqueda.trim());
  const filtradas = lista.filter(
    (c) =>
      (!mecanico || c.mecanico === mecanico) &&
      (!nota || c.estrellas === Number(nota)) &&
      (!texto || sinTildes(`${c.cliente} ${c.ticket} ${c.comentario} ${c.mecanico}`).includes(texto))
  );
  const paginas = Math.max(1, Math.ceil(filtradas.length / TAMANO));
  const paginaActual = Math.min(pagina, paginas - 1);
  const visibles = filtradas.slice(paginaActual * TAMANO, (paginaActual + 1) * TAMANO);
  const promedio = filtradas.length ? filtradas.reduce((s, c) => s + c.estrellas, 0) / filtradas.length : null;
  const ocultas = filtradas.filter((c) => c.oculto).length;
  const hayFiltros = mecanico || nota || texto;

  return (
    <section className="gestion mod">
      <div className="gestion-cabecera">
        <div>
          <h2>Calificaciones</h2>
          <p>Revisa lo que opinan los clientes y oculta los comentarios ofensivos o inapropiados.</p>
        </div>
        <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)} disabled={cargando}>
          Actualizar
        </button>
      </div>

      {/* ---------- Filtros ---------- */}
      <div className="filtros">
        <div className="segmentos" role="group" aria-label="Mostrar calificaciones">
          {VISTAS.map((v) => (
            <button
              key={v.valor || "todas"}
              type="button"
              className={vista === v.valor ? "activo" : ""}
              aria-pressed={vista === v.valor}
              onClick={() => cambiar(setVista)(v.valor)}
            >
              {v.texto}
            </button>
          ))}
        </div>
        <label className="rs-select">
          <span className="sr-only">Mecánico</span>
          <select value={mecanico} onChange={(e) => cambiar(setMecanico)(e.target.value)}>
            <option value="">Todos los mecánicos</option>
            {mecanicos.map((m) => (
              <option key={m} value={m}>
                {m}
              </option>
            ))}
          </select>
        </label>
        <label className="rs-select mod-nota">
          <span className="sr-only">Nota</span>
          <select value={nota} onChange={(e) => cambiar(setNota)(e.target.value)}>
            <option value="">Todas las notas</option>
            {[5, 4, 3, 2, 1].map((n) => (
              <option key={n} value={n}>
                {n} {n === 1 ? "estrella" : "estrellas"}
              </option>
            ))}
          </select>
        </label>
        <label className="buscador mod-buscador">
          <span className="sr-only">Buscar en cliente, ticket o comentario</span>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input
            type="search"
            placeholder="Buscar en cliente, ticket o comentario"
            value={busqueda}
            onChange={(e) => cambiar(setBusqueda)(e.target.value)}
          />
        </label>
      </div>

      {!primeraCarga && !resultado.error && (
        <p className="mod-resumen" role="status">
          <strong>{filtradas.length}</strong> {filtradas.length === 1 ? "calificación" : "calificaciones"}
          {promedio !== null && (
            <>
              {" · promedio "}
              <strong>{formatoPromedio(Math.round(promedio * 10) / 10)}</strong> <Estrellas valor={promedio} tamano={13} />
            </>
          )}
          {vista === "" && ` · ${ocultas} ${ocultas === 1 ? "oculta" : "ocultas"}`}
          {hayFiltros && (
            <button
              type="button"
              className="rs-limpiar"
              onClick={() => {
                setMecanico("");
                setNota("");
                setBusqueda("");
                setPagina(0);
              }}
            >
              Quitar filtros
            </button>
          )}
        </p>
      )}

      {/* ---------- Lista ---------- */}
      <div className={cargando && !primeraCarga ? "tabla-caja actualizando" : "tabla-caja"} aria-busy={cargando}>
        {resultado.error ? (
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudieron cargar las calificaciones</p>
            <p>{resultado.error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        ) : primeraCarga ? (
          <div className="tabla-estado">
            <p>Cargando calificaciones...</p>
          </div>
        ) : filtradas.length === 0 ? (
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">
              {lista.length ? "Sin resultados" : vista === "true" ? "No hay comentarios ocultos" : "Aún no hay calificaciones"}
            </p>
            <p>
              {lista.length
                ? "Ninguna calificación coincide con los filtros."
                : "Cuando los clientes califiquen un servicio desde la app, aparecerán aquí."}
            </p>
          </div>
        ) : (
          <ul className="mod-lista">
            {visibles.map((c) => (
              <li key={c.id} className={`mod-item nota-${c.estrellas}${c.oculto ? " oculto" : ""}`}>
                <div className="mod-cab">
                  <Estrellas valor={c.estrellas} tamano={15} />
                  <strong>{c.cliente}</strong>
                  <span className="mod-meta">
                    para {c.mecanico || "—"} · {c.ticket} · {fechaHora(c.fecha)}
                  </span>
                  {c.oculto ? (
                    <span className="insignia mod-insignia">Comentario oculto</span>
                  ) : (
                    c.comentario && (
                      <button type="button" className="btn-icono mod-ocultar" onClick={() => abrirOcultar(c)}>
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                          <path d="M3 3l18 18M10.6 6.1A9.8 9.8 0 0 1 12 6c6 0 9.5 6 9.5 6a17 17 0 0 1-3 3.6M6.4 7.6A16.7 16.7 0 0 0 2.5 12S6 18 12 18a9.4 9.4 0 0 0 4.2-1" />
                          <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
                        </svg>
                        <span>Ocultar comentario</span>
                      </button>
                    )
                  )}
                </div>
                {c.comentario ? (
                  <p className="mod-comentario">«{c.comentario}»</p>
                ) : (
                  <p className="mod-sin-comentario">Sin comentario</p>
                )}
                {c.oculto ? (
                  <p className="mod-registro">
                    Oculto por {c.ocultadoPor || "—"} el {fechaHora(c.fechaOcultado)} · Motivo: {c.motivoOcultado || "—"}
                  </p>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </div>

      {!resultado.error && !primeraCarga && (
        <Paginacion pagina={paginaActual} paginas={paginas} total={filtradas.length} tamano={TAMANO} alCambiar={setPagina} />
      )}

      {/* ---------- Ocultar comentario ---------- */}
      <Modal abierto={!!porOcultar} alCerrar={() => !enviando && setPorOcultar(null)} titulo="¿Ocultar el comentario?" pequeno>
        {porOcultar && (
          <form className="confirmar" onSubmit={confirmarOcultar} noValidate>
            <p>
              El comentario de <strong>{porOcultar.cliente}</strong> dejará de verse en la app y en el panel del mecánico. Su nota de{" "}
              {porOcultar.estrellas} {porOcultar.estrellas === 1 ? "estrella" : "estrellas"} seguirá contando en el promedio.
            </p>
            <blockquote className="mod-cita">«{porOcultar.comentario}»</blockquote>
            <div className="campo">
              <label htmlFor="mod-motivo">Motivo</label>
              <div className="mod-motivos" role="group" aria-label="Motivos frecuentes">
                {MOTIVOS_FRECUENTES.map((m) => (
                  <button
                    key={m}
                    type="button"
                    className={motivo === m ? "activo" : ""}
                    aria-pressed={motivo === m}
                    onClick={() => {
                      setMotivo(m);
                      setErrorMotivo("");
                    }}
                  >
                    {m}
                  </button>
                ))}
              </div>
              <textarea
                id="mod-motivo"
                rows={2}
                maxLength={MAX_MOTIVO}
                placeholder="O escribe el motivo"
                value={motivo}
                aria-invalid={!!errorMotivo}
                onChange={(e) => {
                  setMotivo(e.target.value);
                  setErrorMotivo("");
                }}
              />
            </div>
            {errorMotivo && (
              <p className="form-error-general" role="alert">
                {errorMotivo}
              </p>
            )}
            <div className="form-acciones">
              <button type="button" className="btn btn-sec" onClick={() => setPorOcultar(null)} disabled={enviando}>
                Cancelar
              </button>
              <button type="submit" className="btn btn-peligro" disabled={enviando}>
                {enviando && <span className="btn-spinner" aria-hidden="true" />}
                {enviando ? "Ocultando..." : "Ocultar comentario"}
              </button>
            </div>
          </form>
        )}
      </Modal>

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Calificaciones;
