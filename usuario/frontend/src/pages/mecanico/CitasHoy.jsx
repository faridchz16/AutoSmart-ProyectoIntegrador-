import { Fragment, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listarCitasHoy, ESTADOS_CITA } from "../../services/agendaService";
import { listarBloqueos, hoyLima, horaLima, aFecha, diaSemanaDe } from "../../services/bloqueosService";
import { listarHorarios, duracionMinutos, formatoHoras } from "../../services/horariosService";
import "../../components/admin/admin-ui.css";
import "./Agenda.css";
import DetalleCita from "./DetalleCita";
import "./CitasHoy.css";

/* T34.1 – Citas de hoy: las citas del día ordenadas por hora, con la cita en curso
   (o la siguiente) destacada y una marca de la hora actual.
   Usa GET /mechanics/me/appointments sin parámetros (el backend devuelve las de hoy, hora de Lima).
   Se actualiza sola cada 2 minutos y al volver a la pestaña.
   T35.2: al hacer clic en una cita se abre su detalle. Más adelante: aceptar / rechazar (T36.2). */

const ACTIVAS = ["PENDIENTE", "CONFIRMADA"];
const CADA_RELOJ = 30 * 1000;
const CADA_ACTUALIZACION = 2 * 60 * 1000;

const leerReloj = () => ({ hoy: hoyLima(), ahora: horaLima() });

const fechaDeHoy = (iso) =>
  aFecha(iso).toLocaleDateString("es-PE", { weekday: "long", day: "numeric", month: "long" });

const plural = (n, uno, varios) => `${n} ${n === 1 ? uno : varios}`;

const IconoActualizar = ({ girando }) => (
  <svg className={girando ? "ch-girando" : undefined} viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d="M20 11a8 8 0 1 0-2.3 5.7" />
    <path d="M20 4v7h-7" />
  </svg>
);

const IconoPersona = () => (
  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <circle cx="12" cy="8" r="3.5" />
    <path d="M5 20a7 7 0 0 1 14 0" />
  </svg>
);

/* ---------- Tarjeta destacada: cita en curso, la siguiente o día terminado ---------- */
function Foco({ enCurso, siguiente, ahora, total, alAbrir }) {
  if (enCurso) {
    const duracion = duracionMinutos(enCurso.horaInicio, enCurso.horaFin);
    const pasados = duracionMinutos(enCurso.horaInicio, ahora);
    const avance = Math.min(100, Math.max(0, Math.round((pasados / duracion) * 100)));
    return (
      <section className="ch-foco en-curso" aria-label="Cita en curso">
        <div className="ch-foco-cab">
          <span className="ch-pulso" aria-hidden="true" />
          <span>En curso</span>
          <span className="ch-foco-tiempo">Termina en {formatoHoras(duracion - pasados)}</span>
        </div>
        <h3>{enCurso.servicio}</h3>
        <p className="ch-foco-datos">
          <span>{enCurso.horaInicio} – {enCurso.horaFin}</span>
          <span>{enCurso.cliente}</span>
          <span className="ag-placa">{enCurso.placa}</span>
        </p>
        <div
          className="ch-barra"
          role="progressbar"
          aria-label="Avance de la cita"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={avance}
        >
          <span style={{ width: `${avance}%` }} />
        </div>
        {siguiente && (
          <p className="ch-despues">
            Después: <strong>{siguiente.horaInicio}</strong> · {siguiente.servicio}
          </p>
        )}
        <button type="button" className="ch-foco-ver" onClick={() => alAbrir(enCurso)}>
          Ver detalle
        </button>
      </section>
    );
  }

  if (siguiente) {
    const falta = duracionMinutos(ahora, siguiente.horaInicio);
    return (
      <section className="ch-foco" aria-label="Siguiente cita">
        <div className="ch-foco-cab">
          <span>Siguiente cita</span>
          <span className="ch-foco-tiempo">Empieza en {formatoHoras(falta)}</span>
        </div>
        <h3>{siguiente.servicio}</h3>
        <p className="ch-foco-datos">
          <span>{siguiente.horaInicio} – {siguiente.horaFin}</span>
          <span>{siguiente.cliente}</span>
          <span className="ag-placa">{siguiente.placa}</span>
        </p>
        <button type="button" className="ch-foco-ver" onClick={() => alAbrir(siguiente)}>
          Ver detalle
        </button>
      </section>
    );
  }

  return (
    <section className="ch-foco listo" aria-label="Resumen del día">
      <div className="ch-foco-cab">
        <span>Día completado</span>
      </div>
      <h3>Ya pasaron todas tus citas de hoy</h3>
      <p className="ch-foco-texto">
        Tenías {plural(total, "cita programada", "citas programadas")}. Revisa tu agenda para ver las próximas.
      </p>
      <Link to="/mecanico/agenda" className="btn btn-sec ch-foco-enlace">Ver agenda semanal</Link>
    </section>
  );
}

/* ---------- Sin citas para mostrar ---------- */
function Vacio({ bloqueado, sinHorario, ocultas, hoy }) {
  let titulo = "No tienes citas para hoy";
  let texto = "Las reservas que hagan los clientes desde la app aparecerán aquí.";
  let enlace = { a: "/mecanico/agenda", texto: "Ver agenda semanal" };

  if (bloqueado) {
    titulo = "Hoy es un día bloqueado";
    texto = "No recibirás reservas hoy.";
    enlace = { a: "/mecanico/bloqueos", texto: "Ver fechas bloqueadas" };
  } else if (sinHorario) {
    const dia = aFecha(hoy).toLocaleDateString("es-PE", { weekday: "long" });
    titulo = "Hoy no atiendes";
    texto = `No tienes horario de atención los ${dia}.`;
    enlace = { a: "/mecanico/horarios", texto: "Revisar mis horarios" };
  }

  return (
    <div className="tabla-caja">
      <div className="tabla-estado">
        <p className="tabla-estado-titulo">{titulo}</p>
        <p>
          {texto}
          {ocultas > 0 && ` Hay ${plural(ocultas, "cita cancelada o rechazada", "citas canceladas o rechazadas")}.`}
        </p>
        <Link to={enlace.a} className="btn btn-sec">{enlace.texto}</Link>
      </div>
    </div>
  );
}

const MarcaAhora = ({ ahora }) => (
  <li className="ch-ahora" aria-label={`Hora actual: ${ahora}`}>
    <time>{ahora}</time>
    <span className="ch-ahora-punto" aria-hidden="true" />
    <span className="ch-ahora-linea">Ahora</span>
  </li>
);

function CitasHoy() {
  const [reloj, setReloj] = useState(leerReloj);
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({
    clave: null,
    datosDe: "",
    citas: [],
    bloqueado: false,
    sinHorario: false,
    error: "",
    fallo: "",
    actualizado: "",
  });
  const [verTodas, setVerTodas] = useState(false);
  const [abierta, setAbierta] = useState(null);

  const { hoy, ahora } = reloj;
  const clave = `${hoy}|${recarga}`;
  const cargando = resultado.clave !== clave;
  const primeraCarga = resultado.clave === null;

  // Reloj de Lima: mueve la marca "Ahora" y la cita en curso; si cambia el día, se recarga
  useEffect(() => {
    const id = setInterval(() => {
      setReloj((r) => {
        const nuevo = leerReloj();
        return nuevo.hoy === r.hoy && nuevo.ahora === r.ahora ? r : nuevo;
      });
    }, CADA_RELOJ);
    return () => clearInterval(id);
  }, []);

  // Actualización automática: cada 2 minutos y al volver a la pestaña
  useEffect(() => {
    const actualizar = () => setRecarga((n) => n + 1);
    const id = setInterval(actualizar, CADA_ACTUALIZACION);
    const alVolver = () => {
      if (document.visibilityState === "visible") actualizar();
    };
    document.addEventListener("visibilitychange", alVolver);
    return () => {
      clearInterval(id);
      document.removeEventListener("visibilitychange", alVolver);
    };
  }, []);

  useEffect(() => {
    let vigente = true;
    Promise.all([
      listarCitasHoy(hoy),
      listarBloqueos(hoy).catch(() => []),
      listarHorarios().catch(() => null),
    ])
      .then(([citas, bloqueos, horarios]) => {
        if (!vigente) return;
        setResultado({
          clave,
          datosDe: hoy,
          citas,
          bloqueado: bloqueos.some((b) => b.fecha === hoy),
          sinHorario: !!horarios && !horarios.some((h) => h.diaSemana === diaSemanaDe(hoy)),
          error: "",
          fallo: "",
          actualizado: horaLima(),
        });
      })
      .catch((e) => {
        if (!vigente) return;
        // Si ya se mostraban las citas de hoy, se mantienen y solo se avisa del fallo
        setResultado((r) =>
          r.datosDe === hoy && !r.error ? { ...r, clave, fallo: e.message } : { ...r, clave, error: e.message }
        );
      });
    return () => {
      vigente = false;
    };
  }, [clave, hoy]);

  const { citas, bloqueado, sinHorario, error, fallo, actualizado } = resultado;
  const activas = citas.filter((c) => ACTIVAS.includes(c.estado));
  const visibles = verTodas ? citas : activas;
  const ocultas = citas.length - activas.length;
  const cuenta = (estado) => citas.filter((c) => c.estado === estado).length;

  const enCurso = activas.find((c) => c.horaInicio <= ahora && ahora < c.horaFin);
  const siguiente = activas.find((c) => c.horaInicio > ahora);

  // Posición de la marca "Ahora" en la lista (no se muestra si hay una cita en curso)
  let posicionAhora = -1;
  if (!enCurso && visibles.length) {
    const i = visibles.findIndex((c) => c.horaInicio > ahora);
    posicionAhora = i === -1 ? visibles.length : i;
  }

  return (
    <section className="gestion">
      <div className="gestion-cabecera">
        <div>
          <h2>Citas de hoy</h2>
          <p className="ch-fecha">{fechaDeHoy(hoy)}</p>
        </div>
        <div className="ch-actualizar">
          {actualizado && <span>Actualizado a las {actualizado}</span>}
          <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)} disabled={cargando}>
            <IconoActualizar girando={cargando && !primeraCarga} />
            Actualizar
          </button>
        </div>
      </div>

      {error ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudieron cargar tus citas</p>
            <p>{error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        </div>
      ) : primeraCarga ? (
        <ol className="ch-lista" aria-busy="true">
          {[0, 1, 2].map((i) => (
            <li key={i} className="ch-item">
              <div className="ch-hora"><span className="esq" /></div>
              <span className="ch-punto" aria-hidden="true" />
              <div className="ch-cita"><span className="esq esq-largo" /></div>
            </li>
          ))}
        </ol>
      ) : (
        <>
          {fallo && (
            <p className="aviso-fijo" role="alert">
              No se pudo actualizar: {fallo} Se muestran las citas cargadas a las {actualizado}.
            </p>
          )}
          {bloqueado && activas.length > 0 && (
            <p className="aviso-fijo">Bloqueaste el día de hoy, pero aún tienes citas registradas.</p>
          )}

          {citas.length > 0 && (
            <div className="ag-resumen ch-resumen">
              <span><strong>{activas.length}</strong> {activas.length === 1 ? "cita" : "citas"}</span>
              <span className="est-PENDIENTE"><strong>{cuenta("PENDIENTE")}</strong> por confirmar</span>
              <span className="est-CONFIRMADA">
                <strong>{cuenta("CONFIRMADA")}</strong> {cuenta("CONFIRMADA") === 1 ? "confirmada" : "confirmadas"}
              </span>
              {ocultas > 0 && (
                <label className="ag-toggle">
                  <input type="checkbox" checked={verTodas} onChange={(e) => setVerTodas(e.target.checked)} />
                  Mostrar canceladas y rechazadas
                  {!verTodas && <em> ({ocultas})</em>}
                </label>
              )}
            </div>
          )}

          {activas.length > 0 && <Foco enCurso={enCurso} siguiente={siguiente} ahora={ahora} total={activas.length} alAbrir={setAbierta} />}

          {visibles.length === 0 ? (
            <Vacio bloqueado={bloqueado} sinHorario={sinHorario} ocultas={ocultas} hoy={hoy} />
          ) : (
            <ol className="ch-lista" aria-label="Citas de hoy por hora">
              {visibles.map((c, i) => {
                const clases = [
                  "ch-item",
                  `est-${c.estado}`,
                  c === enCurso && "en-curso",
                  c !== enCurso && c.horaFin <= ahora && "pasada",
                ]
                  .filter(Boolean)
                  .join(" ");
                return (
                  <Fragment key={c.id}>
                    {i === posicionAhora && <MarcaAhora ahora={ahora} />}
                    <li className={clases}>
                      <div className="ch-hora">
                        <time>{c.horaInicio}</time>
                        <span>{c.horaFin}</span>
                      </div>
                      <span className="ch-punto" aria-hidden="true" />
                      <article
                        className="ch-cita"
                        data-abrible
                        onClick={() => setAbierta(c)}
                        aria-label={`${c.horaInicio} ${c.servicio}, ${c.cliente}`}
                      >
                        <div className="ch-cita-cab">
                          <strong>{c.servicio}</strong>
                          <span className={`insignia-estado est-${c.estado}`}>{ESTADOS_CITA[c.estado] ?? c.estado}</span>
                        </div>
                        <p className="ch-cliente">
                          <IconoPersona />
                          {c.cliente}
                        </p>
                        <div className="ch-cita-pie">
                          <span className="ag-placa">{c.placa}</span>
                          <span>{formatoHoras(duracionMinutos(c.horaInicio, c.horaFin))}</span>
                          {c.ticket && <span className="ch-ticket">Ticket {c.ticket}</span>}
                          {c === enCurso && <span className="ch-etiqueta ahora">En curso</span>}
                          {c === siguiente && <span className="ch-etiqueta">Siguiente</span>}
                          <button
                            type="button"
                            className="ch-ver"
                            onClick={(e) => {
                              e.stopPropagation();
                              setAbierta(c);
                            }}
                            aria-label={`Ver detalle: ${c.horaInicio} ${c.servicio}`}
                          >
                            Ver detalle
                          </button>
                        </div>
                      </article>
                    </li>
                  </Fragment>
                );
              })}
              {posicionAhora === visibles.length && <MarcaAhora ahora={ahora} />}
            </ol>
          )}
        </>
      )}

      <DetalleCita cita={abierta} alCerrar={() => setAbierta(null)} />
    </section>
  );
}

export default CitasHoy;
