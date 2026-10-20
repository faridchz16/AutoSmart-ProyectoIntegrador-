import { useCallback, useEffect, useRef, useState } from "react";
import {
  listarBloqueos,
  bloquearFecha,
  desbloquearFecha,
  hoyLima,
  isoDe,
  aFecha,
  fechaLarga,
  fechaCorta,
  diaSemanaDe,
} from "../../services/bloqueosService";
import { listarHorarios } from "../../services/horariosService";
import Modal from "../../components/admin/Modal";
import Aviso from "../../components/admin/Aviso";
import "../../components/admin/admin-ui.css";
import "./Bloqueos.css";

/* T22.2 – Selector de fechas bloqueadas: el mecánico marca en el calendario
   los días en que no atenderá (usa /mechanics/me/blocked-dates, T22.1). */

const DIAS_CORTOS = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"];
const MAX_MOTIVO = 200;

const Flecha = ({ izquierda }) => (
  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d={izquierda ? "m15 6-6 6 6 6" : "m9 6 6 6-6 6"} />
  </svg>
);

const Candado = () => (
  <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <rect x="5" y="11" width="14" height="9" rx="2" />
    <path d="M8 11V8a4 4 0 0 1 8 0v3" />
  </svg>
);

// Celdas del mes: espacios vacíos hasta el primer lunes y luego los días
function celdasDelMes(anio, mes) {
  const desfase = (new Date(anio, mes, 1).getDay() + 6) % 7;
  const total = new Date(anio, mes + 1, 0).getDate();
  return [...Array(desfase).fill(null), ...Array.from({ length: total }, (_, i) => i + 1)];
}

const tituloMes = (anio, mes) => {
  const t = new Date(anio, mes, 1).toLocaleDateString("es-PE", { month: "long", year: "numeric" });
  return t.charAt(0).toUpperCase() + t.slice(1);
};

function Bloqueos() {
  const [hoy] = useState(hoyLima);
  const [vista, setVista] = useState(() => {
    const d = aFecha(hoy);
    return { anio: d.getFullYear(), mes: d.getMonth() };
  });
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, bloqueos: [], laborables: null, error: "" });
  const [seleccion, setSeleccion] = useState(null); // { fecha, bloqueo }
  const [motivo, setMotivo] = useState("");
  const [enviando, setEnviando] = useState(false);
  const [errorModal, setErrorModal] = useState("");
  const [aviso, setAviso] = useState(null);

  const primeraCarga = resultado.clave === null;

  const contadorAvisos = useRef(0);
  const avisar = (tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  };
  const cerrarAviso = useCallback(() => setAviso(null), []);

  // Fechas bloqueadas desde hoy + días de la semana con horario (para orientar al mecánico)
  useEffect(() => {
    let vigente = true;
    Promise.all([listarBloqueos(hoy), listarHorarios().catch(() => null)])
      .then(([bloqueos, horarios]) => {
        if (!vigente) return;
        const laborables = horarios ? new Set(horarios.map((h) => h.diaSemana)) : null;
        setResultado({ clave: recarga, bloqueos, laborables, error: "" });
      })
      .catch((e) => vigente && setResultado({ clave: recarga, bloqueos: [], laborables: null, error: e.message }));
    return () => {
      vigente = false;
    };
  }, [recarga, hoy]);

  const { bloqueos, laborables, error } = resultado;
  const bloqueoDe = (iso) => bloqueos.find((b) => b.fecha === iso);
  const atiende = (iso) => !laborables || laborables.has(diaSemanaDe(iso));

  const mesActual = aFecha(hoy);
  const enMesActual = vista.anio === mesActual.getFullYear() && vista.mes === mesActual.getMonth();
  const moverMes = (paso) =>
    setVista((v) => {
      const d = new Date(v.anio, v.mes + paso, 1);
      return { anio: d.getFullYear(), mes: d.getMonth() };
    });

  const abrir = (fecha) => {
    setSeleccion({ fecha, bloqueo: bloqueoDe(fecha) ?? null });
    setMotivo("");
    setErrorModal("");
  };
  const cerrar = () => !enviando && setSeleccion(null);

  const confirmar = async () => {
    const { fecha, bloqueo } = seleccion;
    setEnviando(true);
    setErrorModal("");
    try {
      if (bloqueo) {
        await desbloquearFecha(bloqueo.id);
        setResultado((r) => ({ ...r, bloqueos: r.bloqueos.filter((b) => b.id !== bloqueo.id) }));
        avisar("ok", `Desbloqueaste el ${fechaLarga(fecha)}.`);
      } else {
        const nuevo = await bloquearFecha(fecha, motivo);
        setResultado((r) => ({
          ...r,
          bloqueos: [...r.bloqueos, nuevo].sort((a, b) => a.fecha.localeCompare(b.fecha)),
        }));
        avisar("ok", `Bloqueaste el ${fechaLarga(fecha)}.`);
      }
      setSeleccion(null);
    } catch (e) {
      setErrorModal(e.message);
    } finally {
      setEnviando(false);
    }
  };

  return (
    <section className="gestion">
      <div className="gestion-cabecera">
        <div>
          <h2>Fechas bloqueadas</h2>
          <p>Marca los días en que no atenderás. Esas fechas no aparecerán disponibles para reservar.</p>
        </div>
      </div>

      {error ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudieron cargar tus fechas</p>
            <p>{error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        </div>
      ) : (
        <div className="blq">
          {/* ---------- Calendario ---------- */}
          <div className="cal" aria-busy={primeraCarga}>
            <div className="cal-cab">
              <button
                type="button"
                className="cal-nav"
                onClick={() => moverMes(-1)}
                disabled={enMesActual}
                aria-label="Mes anterior"
              >
                <Flecha izquierda />
              </button>
              <h3 aria-live="polite">{tituloMes(vista.anio, vista.mes)}</h3>
              <button type="button" className="cal-nav" onClick={() => moverMes(1)} aria-label="Mes siguiente">
                <Flecha />
              </button>
            </div>

            <div className="cal-grid" role="grid" aria-label={`Calendario de ${tituloMes(vista.anio, vista.mes)}`}>
              {DIAS_CORTOS.map((d) => (
                <span key={d} className="cal-dia-nombre" role="columnheader">{d}</span>
              ))}
              {celdasDelMes(vista.anio, vista.mes).map((dia, i) => {
                if (!dia) return <span key={`v${i}`} className="cal-vacio" aria-hidden="true" />;
                const iso = isoDe(vista.anio, vista.mes, dia);
                const pasado = iso < hoy;
                const bloqueado = !!bloqueoDe(iso);
                const conHorario = atiende(iso);
                const clases = [
                  "cal-dia",
                  pasado && "pasado",
                  iso === hoy && "hoy",
                  bloqueado && "bloqueado",
                  !bloqueado && !pasado && conHorario && "laborable",
                ]
                  .filter(Boolean)
                  .join(" ");
                return (
                  <button
                    key={iso}
                    type="button"
                    role="gridcell"
                    className={clases}
                    disabled={pasado || primeraCarga}
                    onClick={() => abrir(iso)}
                    aria-label={`${bloqueado ? "Desbloquear" : "Bloquear"} ${fechaLarga(iso)}`}
                    aria-pressed={bloqueado}
                  >
                    <span>{dia}</span>
                    {bloqueado && <Candado />}
                  </button>
                );
              })}
            </div>

            <ul className="cal-leyenda">
              <li><i className="ley laborable" />Atiendes ese día</li>
              <li><i className="ley libre" />Sin horario</li>
              <li><i className="ley bloqueado" />Bloqueada</li>
              <li><i className="ley hoy" />Hoy</li>
            </ul>
            {laborables && laborables.size === 0 && (
              <p className="aviso-fijo cal-nota">
                Aún no registraste horarios. Agrégalos en «Mis horarios» para que los clientes puedan reservarte.
              </p>
            )}
          </div>

          {/* ---------- Lista de próximas fechas ---------- */}
          <aside className="blq-lista" aria-label="Próximas fechas bloqueadas">
            <h3>
              Próximas fechas bloqueadas
              {!primeraCarga && <span className="blq-cuenta">{bloqueos.length}</span>}
            </h3>
            {primeraCarga ? (
              <span className="esq esq-largo" aria-hidden="true" />
            ) : bloqueos.length === 0 ? (
              <p className="blq-vacio">No tienes fechas bloqueadas. Haz clic en un día del calendario para bloquearlo.</p>
            ) : (
              <ul>
                {bloqueos.map((b) => (
                  <li key={b.id} className="blq-item">
                    <div>
                      <strong>{fechaCorta(b.fecha)}</strong>
                      <span>{b.motivo || "Sin motivo"}</span>
                    </div>
                    <button type="button" className="btn-icono" onClick={() => abrir(b.fecha)}>
                      Desbloquear
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </aside>
        </div>
      )}

      {/* ---------- Bloquear / desbloquear ---------- */}
      <Modal
        abierto={!!seleccion}
        alCerrar={cerrar}
        titulo={seleccion?.bloqueo ? "Desbloquear fecha" : "Bloquear fecha"}
        pequeno
      >
        {seleccion && (
          <div className="adm-form">
            {errorModal && <p className="form-error-general" role="alert">{errorModal}</p>}
            <p className="blq-fecha">{fechaLarga(seleccion.fecha)}</p>

            {seleccion.bloqueo ? (
              <>
                <p className="blq-texto">
                  Motivo: <strong>{seleccion.bloqueo.motivo || "sin motivo"}</strong>. Al desbloquearla, los clientes
                  podrán volver a reservar ese día dentro de tus horarios.
                </p>
                <div className="form-acciones">
                  <button type="button" className="btn btn-sec" onClick={cerrar} disabled={enviando}>Cancelar</button>
                  <button type="button" className="btn btn-pri" onClick={confirmar} disabled={enviando}>
                    {enviando ? "Desbloqueando..." : "Desbloquear"}
                  </button>
                </div>
              </>
            ) : (
              <>
                {!atiende(seleccion.fecha) && (
                  <p className="blq-texto">Ese día no tienes horario de atención; igual puedes bloquearlo.</p>
                )}
                <div className="campo">
                  <div className="campo-fila">
                    <label htmlFor="blq-motivo">
                      Motivo <span className="opcional">(opcional)</span>
                    </label>
                    <span className={motivo.length > MAX_MOTIVO ? "contador excedido" : "contador"}>
                      {motivo.length}/{MAX_MOTIVO}
                    </span>
                  </div>
                  <textarea
                    id="blq-motivo"
                    rows={2}
                    maxLength={MAX_MOTIVO}
                    autoFocus
                    placeholder="Ej. Capacitación, vacaciones, trámite personal"
                    value={motivo}
                    onChange={(e) => setMotivo(e.target.value)}
                  />
                </div>
                <div className="form-acciones">
                  <button type="button" className="btn btn-sec" onClick={cerrar} disabled={enviando}>Cancelar</button>
                  <button type="button" className="btn btn-peligro" onClick={confirmar} disabled={enviando}>
                    {enviando ? "Bloqueando..." : "Bloquear fecha"}
                  </button>
                </div>
              </>
            )}
          </div>
        )}
      </Modal>

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Bloqueos;
