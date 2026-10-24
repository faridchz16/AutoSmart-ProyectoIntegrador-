import { useEffect, useState } from "react";
import { listarAgenda, ESTADOS_CITA } from "../../services/agendaService";
import { listarBloqueos, hoyLima, isoDe, aFecha } from "../../services/bloqueosService";
import { listarHorarios } from "../../services/horariosService";
import "../../components/admin/admin-ui.css";
import "./Agenda.css";
import DetalleCita from "./DetalleCita";

/* T23.2 – Agenda semanal del mecánico: citas de lunes a domingo,
   con navegación entre semanas (usa /mechanics/me/appointments, T23.1).
   También marca los días bloqueados y los días sin horario de atención.
   T35.2: al hacer clic en una cita se abre su detalle. */

const DIAS = ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"];
const ACTIVAS = ["PENDIENTE", "CONFIRMADA"];

const Flecha = ({ izquierda }) => (
  <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    <path d={izquierda ? "m15 6-6 6 6 6" : "m9 6 6 6-6 6"} />
  </svg>
);

// Lunes de la semana que contiene la fecha (AAAA-MM-DD)
function lunesDe(iso) {
  const d = aFecha(iso);
  d.setDate(d.getDate() - ((d.getDay() + 6) % 7));
  return isoDe(d.getFullYear(), d.getMonth(), d.getDate());
}

function sumarDias(iso, dias) {
  const d = aFecha(iso);
  d.setDate(d.getDate() + dias);
  return isoDe(d.getFullYear(), d.getMonth(), d.getDate());
}

const corta = (iso, opciones) => aFecha(iso).toLocaleDateString("es-PE", opciones);

function rangoTexto(lunes, domingo) {
  const mismoMes = lunes.slice(0, 7) === domingo.slice(0, 7);
  const inicio = corta(lunes, mismoMes ? { day: "numeric" } : { day: "numeric", month: "short" });
  const fin = corta(domingo, { day: "numeric", month: "short", year: "numeric" });
  return `${inicio} – ${fin}`;
}

function Agenda() {
  const [hoy] = useState(hoyLima);
  const [lunes, setLunes] = useState(() => lunesDe(hoy));
  const [verTodas, setVerTodas] = useState(false);
  const [recarga, setRecarga] = useState(0);
  const [abierta, setAbierta] = useState(null);
  const [resultado, setResultado] = useState({ clave: null, citas: [], bloqueadas: [], laborables: null, error: "" });

  const domingo = sumarDias(lunes, 6);
  const clave = `${lunes}|${recarga}`;
  const cargando = resultado.clave !== clave;
  const primeraCarga = resultado.clave === null;

  useEffect(() => {
    let vigente = true;
    Promise.all([
      listarAgenda(lunes, domingo),
      listarBloqueos(lunes).catch(() => []),
      listarHorarios().catch(() => null),
    ])
      .then(([citas, bloqueos, horarios]) => {
        if (!vigente) return;
        setResultado({
          clave,
          citas,
          bloqueadas: bloqueos.filter((b) => b.fecha <= domingo).map((b) => b.fecha),
          laborables: horarios ? new Set(horarios.map((h) => h.diaSemana)) : null,
          error: "",
        });
      })
      .catch((e) => vigente && setResultado({ clave, citas: [], bloqueadas: [], laborables: null, error: e.message }));
    return () => {
      vigente = false;
    };
  }, [clave, lunes, domingo]);

  const { citas, bloqueadas, laborables, error } = resultado;
  const visibles = verTodas ? citas : citas.filter((c) => ACTIVAS.includes(c.estado));
  const ocultas = citas.length - visibles.length;
  const cuenta = (estado) => citas.filter((c) => c.estado === estado).length;
  const esSemanaActual = lunes === lunesDe(hoy);

  return (
    <section className="gestion">
      <div className="gestion-cabecera">
        <div>
          <h2>Agenda semanal</h2>
          <p>Tus citas de la semana, de lunes a domingo.</p>
        </div>
        <div className="ag-nav" role="group" aria-label="Cambiar de semana">
          <button type="button" className="cal-nav-btn" onClick={() => setLunes((l) => sumarDias(l, -7))} aria-label="Semana anterior">
            <Flecha izquierda />
          </button>
          <button type="button" className="btn btn-sec" onClick={() => setLunes(lunesDe(hoy))} disabled={esSemanaActual}>
            Hoy
          </button>
          <button type="button" className="cal-nav-btn" onClick={() => setLunes((l) => sumarDias(l, 7))} aria-label="Semana siguiente">
            <Flecha />
          </button>
        </div>
      </div>

      <div className="ag-barra">
        <p className="ag-rango" aria-live="polite">
          {rangoTexto(lunes, domingo)}
          {esSemanaActual && <span className="ag-esta">Esta semana</span>}
        </p>
        {!primeraCarga && !error && (
          <div className="ag-resumen">
            <span><strong>{cuenta("PENDIENTE") + cuenta("CONFIRMADA")}</strong> por atender</span>
            <span className="est-PENDIENTE"><strong>{cuenta("PENDIENTE")}</strong> pendientes</span>
            <span className="est-CONFIRMADA"><strong>{cuenta("CONFIRMADA")}</strong> confirmadas</span>
            <label className="ag-toggle">
              <input type="checkbox" checked={verTodas} onChange={(e) => setVerTodas(e.target.checked)} />
              Mostrar canceladas y rechazadas
              {!verTodas && ocultas > 0 && <em> ({ocultas})</em>}
            </label>
          </div>
        )}
      </div>

      {error ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudo cargar tu agenda</p>
            <p>{error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        </div>
      ) : (
        <div className={cargando && !primeraCarga ? "ag-semana actualizando" : "ag-semana"} aria-busy={cargando}>
          {DIAS.map((nombre, i) => {
            const fecha = sumarDias(lunes, i);
            const delDia = visibles.filter((c) => c.fecha === fecha);
            const bloqueado = bloqueadas.includes(fecha);
            const sinHorario = laborables && !laborables.has(i + 1);
            const clases = ["ag-dia", fecha === hoy && "hoy", fecha < hoy && "pasado"].filter(Boolean).join(" ");
            return (
              <article key={fecha} className={clases} aria-label={`${nombre} ${corta(fecha, { day: "numeric", month: "long" })}`}>
                <header className="ag-dia-cab">
                  <span className="ag-dia-nombre">{nombre.slice(0, 3)}</span>
                  <span className="ag-dia-num">{aFecha(fecha).getDate()}</span>
                  {!primeraCarga && delDia.length > 0 && <span className="ag-dia-total">{delDia.length}</span>}
                </header>

                <div className="ag-dia-cuerpo">
                  {primeraCarga ? (
                    <span className="esq" aria-hidden="true" />
                  ) : (
                    <>
                      {bloqueado && <p className="ag-marca bloqueado">Día bloqueado</p>}
                      {!bloqueado && sinHorario && delDia.length === 0 && <p className="ag-marca">Sin horario</p>}
                      {delDia.map((c) => (
                        <button
                          key={c.id}
                          type="button"
                          className={`ag-cita est-${c.estado}`}
                          title={`Ticket ${c.ticket}`}
                          onClick={() => setAbierta(c)}
                        >
                          <span className="ag-cita-hora">
                            {c.horaInicio} – {c.horaFin}
                          </span>
                          <strong>{c.servicio}</strong>
                          <span className="ag-cita-cliente">{c.cliente}</span>
                          <span className="ag-cita-pie">
                            <span className="ag-placa">{c.placa}</span>
                            <span className={`insignia-estado est-${c.estado}`}>{ESTADOS_CITA[c.estado] ?? c.estado}</span>
                          </span>
                        </button>
                      ))}
                      {delDia.length === 0 && !bloqueado && !sinHorario && <p className="ag-libre">Sin citas</p>}
                    </>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}

      {!primeraCarga && !error && citas.length === 0 && (
        <p className="ag-nota">
          No tienes citas esta semana. Las reservas que hagan los clientes desde la app aparecerán aquí.
        </p>
      )}

      <DetalleCita cita={abierta} alCerrar={() => setAbierta(null)} />
    </section>
  );
}

export default Agenda;
