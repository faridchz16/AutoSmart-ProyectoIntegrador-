import { useCallback, useEffect, useRef, useState } from "react";
import {
  obtenerHorarioTaller,
  guardarHorarioTaller,
  listarFeriados,
  crearFeriado,
  eliminarFeriado,
  minutosEntre,
  MAX_DESCRIPCION_FERIADO,
} from "../../services/tallerService";
import { DIAS, formatoHoras } from "../../services/horariosService";
import { hoyLima, aFecha, fechaLarga } from "../../services/bloqueosService";
import Switch from "../../components/admin/Switch";
import Modal from "../../components/admin/Modal";
import Aviso from "../../components/admin/Aviso";
import "../../components/admin/admin-ui.css";
import "./Taller.css";

/* T40.2 – Horario del taller y feriados (usa /admin/shop-hours y /admin/holidays, T40.1).
   Los clientes solo pueden reservar los días que el taller abre, dentro de su horario
   y fuera de los feriados (además del horario de cada mecánico). */

const claveHorario = (dias) =>
  dias.map((d) => `${d.diaSemana}${d.abierto ? "A" : "C"}${d.horaApertura}-${d.horaCierre}`).join("|");

const errorDia = (d) =>
  d.abierto && (!d.horaApertura || !d.horaCierre || minutosEntre(d.horaApertura, d.horaCierre) <= 0)
    ? "La apertura debe ser antes del cierre."
    : "";

/* ---------- Horario semanal ---------- */
function HorarioSemanal({ avisar }) {
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, guardado: [], error: "" });
  const [dias, setDias] = useState([]);
  const [guardando, setGuardando] = useState(false);
  const [errorGuardar, setErrorGuardar] = useState("");
  const primeraCarga = resultado.clave === null;

  useEffect(() => {
    let vigente = true;
    obtenerHorarioTaller()
      .then((lista) => {
        if (!vigente) return;
        setResultado({ clave: recarga, guardado: lista, error: "" });
        setDias(lista);
      })
      .catch((e) => vigente && setResultado({ clave: recarga, guardado: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [recarga]);

  const cambiar = (diaSemana, cambios) => {
    setDias((lista) => lista.map((d) => (d.diaSemana === diaSemana ? { ...d, ...cambios } : d)));
    setErrorGuardar("");
  };

  const hayCambios = dias.length === 7 && claveHorario(dias) !== claveHorario(resultado.guardado);
  const hayErrores = dias.some((d) => errorDia(d));
  const abiertos = dias.filter((d) => d.abierto && !errorDia(d));
  const minutosSemana = abiertos.reduce((total, d) => total + minutosEntre(d.horaApertura, d.horaCierre), 0);
  const lunes = dias.find((d) => d.diaSemana === 1);

  const copiarLunes = () => {
    setDias((lista) =>
      lista.map((d) =>
        d.diaSemana >= 2 && d.diaSemana <= 5
          ? { ...d, abierto: lunes.abierto, horaApertura: lunes.horaApertura, horaCierre: lunes.horaCierre }
          : d
      )
    );
    setErrorGuardar("");
  };

  const guardar = async () => {
    if (!hayCambios || hayErrores || guardando) return;
    setGuardando(true);
    setErrorGuardar("");
    try {
      const lista = await guardarHorarioTaller(dias);
      setResultado((r) => ({ ...r, guardado: lista }));
      setDias(lista);
      avisar("ok", "Se guardó el horario del taller.");
    } catch (e) {
      setErrorGuardar(e.message);
    } finally {
      setGuardando(false);
    }
  };

  return (
    <section className="tl-caja" aria-labelledby="tl-horario-titulo">
      <div className="tl-caja-cab">
        <div>
          <h3 id="tl-horario-titulo">Horario semanal</h3>
          {!primeraCarga && !resultado.error && (
            <p className="tl-resumen">
              Abre <strong>{abiertos.length}</strong> {abiertos.length === 1 ? "día" : "días"} ·{" "}
              <strong>{formatoHoras(minutosSemana)}</strong> a la semana
            </p>
          )}
        </div>
        {!primeraCarga && !resultado.error && (
          <button type="button" className="tl-enlace" onClick={copiarLunes} disabled={guardando}>
            Copiar el lunes a martes–viernes
          </button>
        )}
      </div>

      {resultado.error ? (
        <div className="tl-error" role="alert">
          <p>No se pudo cargar el horario: {resultado.error}</p>
          <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
            Reintentar
          </button>
        </div>
      ) : primeraCarga ? (
        <div className="tl-esqueleto" aria-busy="true">
          {DIAS.map((d) => (
            <span key={d} className="esq esq-largo" />
          ))}
        </div>
      ) : (
        <>
          <ul className="tl-dias">
            {dias.map((d) => {
              const nombre = DIAS[d.diaSemana - 1];
              const error = errorDia(d);
              const minutos = d.abierto && !error ? minutosEntre(d.horaApertura, d.horaCierre) : 0;
              return (
                <li key={d.diaSemana} className={d.abierto ? "tl-dia" : "tl-dia cerrado"}>
                  <span className="tl-dia-nombre">{nombre}</span>
                  <Switch
                    activo={d.abierto}
                    textoSi="Abierto"
                    textoNo="Cerrado"
                    etiqueta={`${nombre}: el taller ${d.abierto ? "abre" : "está cerrado"}`}
                    deshabilitado={guardando}
                    alCambiar={(abierto) => cambiar(d.diaSemana, { abierto })}
                  />
                  <div className="tl-horas">
                    <label>
                      <span className="sr-only">{nombre}: hora de apertura</span>
                      <input
                        type="time"
                        value={d.horaApertura}
                        disabled={!d.abierto || guardando}
                        aria-invalid={!!error}
                        onChange={(e) => cambiar(d.diaSemana, { horaApertura: e.target.value })}
                      />
                    </label>
                    <span aria-hidden="true">a</span>
                    <label>
                      <span className="sr-only">{nombre}: hora de cierre</span>
                      <input
                        type="time"
                        value={d.horaCierre}
                        disabled={!d.abierto || guardando}
                        aria-invalid={!!error}
                        onChange={(e) => cambiar(d.diaSemana, { horaCierre: e.target.value })}
                      />
                    </label>
                  </div>
                  <span className={error ? "tl-dia-info con-error" : "tl-dia-info"}>
                    {error || (d.abierto ? formatoHoras(minutos) : "No se reciben reservas")}
                  </span>
                </li>
              );
            })}
          </ul>

          {errorGuardar && <p className="form-error-general" role="alert">{errorGuardar}</p>}

          <div className="form-acciones">
            <button
              type="button"
              className="btn btn-sec"
              onClick={() => {
                setDias(resultado.guardado);
                setErrorGuardar("");
              }}
              disabled={!hayCambios || guardando}
            >
              Descartar cambios
            </button>
            <button type="button" className="btn btn-pri" onClick={guardar} disabled={!hayCambios || hayErrores || guardando}>
              {guardando && <span className="btn-spinner" aria-hidden="true" />}
              {guardando ? "Guardando..." : "Guardar horario"}
            </button>
          </div>
        </>
      )}
    </section>
  );
}

/* ---------- Feriados ---------- */
function Feriados({ avisar }) {
  const [hoy] = useState(hoyLima);
  const [anio, setAnio] = useState(() => Number(hoyLima().slice(0, 4)));
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, feriados: [], error: "" });
  const [fecha, setFecha] = useState("");
  const [descripcion, setDescripcion] = useState("");
  const [errorForm, setErrorForm] = useState("");
  const [agregando, setAgregando] = useState(false);
  const [porEliminar, setPorEliminar] = useState(null);
  const [eliminando, setEliminando] = useState(false);

  const clave = `${anio}|${recarga}`;
  const cargando = resultado.clave !== clave;
  const primeraCarga = resultado.clave === null;

  useEffect(() => {
    let vigente = true;
    listarFeriados(anio)
      .then((feriados) => vigente && setResultado({ clave, feriados, error: "" }))
      .catch((e) => vigente && setResultado({ clave, feriados: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [clave, anio]);

  const { feriados, error } = resultado;

  const agregar = async (e) => {
    e.preventDefault();
    const texto = descripcion.trim();
    if (!fecha) return setErrorForm("Elige la fecha del feriado.");
    if (!texto) return setErrorForm("Escribe una descripción, por ejemplo «Navidad».");
    if (feriados.some((f) => f.fecha === fecha)) return setErrorForm("Ya registraste un feriado en esa fecha.");
    setAgregando(true);
    setErrorForm("");
    try {
      const nuevo = await crearFeriado({ fecha, descripcion: texto });
      const anioNuevo = Number(nuevo.fecha.slice(0, 4));
      if (anioNuevo === anio) {
        setResultado((r) => ({ ...r, feriados: [...r.feriados, nuevo].sort((a, b) => a.fecha.localeCompare(b.fecha)) }));
      } else {
        setAnio(anioNuevo);
      }
      setFecha("");
      setDescripcion("");
      avisar("ok", `Se agregó el feriado del ${fechaLarga(nuevo.fecha)}.`);
    } catch (err) {
      setErrorForm(err.message);
    } finally {
      setAgregando(false);
    }
  };

  const confirmarEliminar = async () => {
    setEliminando(true);
    try {
      await eliminarFeriado(porEliminar.id);
      setResultado((r) => ({ ...r, feriados: r.feriados.filter((f) => f.id !== porEliminar.id) }));
      avisar("ok", `Se eliminó el feriado «${porEliminar.descripcion}».`);
      setPorEliminar(null);
    } catch (err) {
      avisar("error", err.message);
      setPorEliminar(null);
      setRecarga((n) => n + 1);
    } finally {
      setEliminando(false);
    }
  };

  return (
    <section className="tl-caja" aria-labelledby="tl-feriados-titulo">
      <div className="tl-caja-cab">
        <div>
          <h3 id="tl-feriados-titulo">Feriados</h3>
          <p className="tl-resumen">Esos días el taller no recibe reservas.</p>
        </div>
        <div className="tl-anio" role="group" aria-label="Cambiar de año">
          <button type="button" className="tl-anio-btn" onClick={() => setAnio((a) => a - 1)} aria-label="Año anterior">
            ‹
          </button>
          <strong aria-live="polite">{anio}</strong>
          <button type="button" className="tl-anio-btn" onClick={() => setAnio((a) => a + 1)} aria-label="Año siguiente">
            ›
          </button>
        </div>
      </div>

      {/* ---------- Agregar ---------- */}
      <form className="tl-form" onSubmit={agregar} noValidate>
        <label className="tl-campo">
          <span>Fecha</span>
          <input
            type="date"
            value={fecha}
            onChange={(e) => {
              setFecha(e.target.value);
              setErrorForm("");
            }}
          />
        </label>
        <label className="tl-campo tl-campo-largo">
          <span>
            Descripción
            <small className={descripcion.length > MAX_DESCRIPCION_FERIADO ? "contador excedido" : "contador"}>
              {descripcion.length}/{MAX_DESCRIPCION_FERIADO}
            </small>
          </span>
          <input
            type="text"
            maxLength={MAX_DESCRIPCION_FERIADO}
            placeholder="Ej. Fiestas Patrias"
            value={descripcion}
            onChange={(e) => {
              setDescripcion(e.target.value);
              setErrorForm("");
            }}
          />
        </label>
        <button type="submit" className="btn btn-pri" disabled={agregando}>
          {agregando && <span className="btn-spinner" aria-hidden="true" />}
          {agregando ? "Agregando..." : "Agregar"}
        </button>
      </form>
      {errorForm && <p className="campo-error tl-form-error" role="alert">{errorForm}</p>}

      {/* ---------- Lista ---------- */}
      {error ? (
        <div className="tl-error" role="alert">
          <p>No se pudieron cargar los feriados: {error}</p>
          <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
            Reintentar
          </button>
        </div>
      ) : primeraCarga ? (
        <div className="tl-esqueleto" aria-busy="true">
          <span className="esq esq-largo" />
          <span className="esq esq-largo" />
        </div>
      ) : feriados.length === 0 ? (
        <p className="tl-vacio">No hay feriados registrados en {anio}.</p>
      ) : (
        <ul className={cargando ? "tl-feriados actualizando" : "tl-feriados"} aria-busy={cargando}>
          {feriados.map((f) => {
            const d = aFecha(f.fecha);
            const pasado = f.fecha < hoy;
            return (
              <li key={f.id} className={pasado ? "tl-feriado pasado" : "tl-feriado"}>
                <span className="tl-fecha" aria-hidden="true">
                  <strong>{d.getDate()}</strong>
                  <small>{d.toLocaleDateString("es-PE", { month: "short" }).replace(".", "")}</small>
                </span>
                <span className="tl-feriado-texto">
                  <strong>{f.descripcion}</strong>
                  <small>
                    {fechaLarga(f.fecha)}
                    {pasado && " · ya pasó"}
                  </small>
                </span>
                <button
                  type="button"
                  className="btn-icono tl-quitar"
                  onClick={() => setPorEliminar(f)}
                  aria-label={`Eliminar el feriado ${f.descripcion}`}
                >
                  Eliminar
                </button>
              </li>
            );
          })}
        </ul>
      )}

      <Modal abierto={!!porEliminar} alCerrar={() => !eliminando && setPorEliminar(null)} titulo="¿Eliminar feriado?" pequeno>
        {porEliminar && (
          <div className="confirmar">
            <p>
              <strong>{porEliminar.descripcion}</strong> ({fechaLarga(porEliminar.fecha)}). Ese día el taller volverá a
              recibir reservas según su horario.
            </p>
            <div className="form-acciones">
              <button type="button" className="btn btn-sec" onClick={() => setPorEliminar(null)} disabled={eliminando}>
                Cancelar
              </button>
              <button type="button" className="btn btn-peligro" onClick={confirmarEliminar} disabled={eliminando}>
                {eliminando ? "Eliminando..." : "Eliminar"}
              </button>
            </div>
          </div>
        )}
      </Modal>
    </section>
  );
}

function Taller() {
  const [aviso, setAviso] = useState(null);
  const contadorAvisos = useRef(0);
  const avisar = useCallback((tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  }, []);
  const cerrarAviso = useCallback(() => setAviso(null), []);

  return (
    <section className="gestion">
      <div className="gestion-cabecera">
        <div>
          <h2>Horario del taller</h2>
          <p>Define cuándo atiende el taller. Los clientes solo pueden reservar dentro de este horario.</p>
        </div>
      </div>

      <div className="tl">
        <HorarioSemanal avisar={avisar} />
        <Feriados avisar={avisar} />
      </div>

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Taller;
