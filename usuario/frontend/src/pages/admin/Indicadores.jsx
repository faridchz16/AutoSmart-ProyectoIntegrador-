import { useEffect, useState } from "react";
import {
  obtenerMetricas,
  mesActual,
  sumarMeses,
  nombreMes,
  nombreMetodo,
  formatoSoles,
  formatoSolesCorto,
  variacion,
} from "../../services/indicadoresService";
import { ESTADOS_CITA } from "../../services/agendaService";
import { textoDuracion } from "../../services/historialMecanicoService";
import { formatoPromedio } from "../../services/calificacionesService";
import Estrellas from "../../components/Estrellas";
import "../../components/admin/admin-ui.css";
import "./Indicadores.css";

/* T73.2 – Dashboard de indicadores del taller (API de T73.1).
   - Selector de mes (por defecto el actual) con flechas para ir al anterior o al siguiente
   - Citas, ingresos, tiempo promedio de atención y satisfacción, comparados con el mes anterior
   - Ingresos de los últimos 6 meses (al hacer clic en una columna se abre ese mes),
     ingresos por método de pago y citas por estado */

const MESES_TENDENCIA = 6;
// Orden del flujo de una cita: así se leen los estados en el gráfico
const ORDEN_ESTADOS = ["PENDIENTE", "CONFIRMADA", "EN_PROCESO", "ESPERA_APROBACION", "FINALIZADA", "PAGADO", "CANCELADA", "RECHAZADA"];

function cargarPanel(mes) {
  const meses = Array.from({ length: MESES_TENDENCIA }, (_, i) => sumarMeses(mes, i - MESES_TENDENCIA + 1));
  return Promise.all(meses.map((m) => obtenerMetricas(m).then((d) => ({ ...d, ok: true }), (e) => ({ mes: m, ok: false, error: e }))));
}

function Variacion({ actual, anterior, mesAnterior, menosEsMejor = false }) {
  const pct = variacion(actual, anterior);
  if (pct === null) return <span className="ind-var">Sin datos de {nombreMes(mesAnterior, true)} para comparar</span>;
  const signo = pct > 0 ? "sube" : pct < 0 ? "baja" : "igual";
  const bueno = pct === 0 ? null : (pct < 0) === menosEsMejor;
  return (
    <span className={`ind-var ind-var-${signo}${bueno === null ? "" : bueno ? " bueno" : " malo"}`}>
      <span aria-hidden="true">{pct > 0 ? "▲" : pct < 0 ? "▼" : "="}</span> {pct === 0 ? "Igual" : `${Math.abs(pct)} %`} vs.{" "}
      {nombreMes(mesAnterior, true)}
    </span>
  );
}

function Indicadores() {
  const hoy = mesActual();
  const [mes, setMes] = useState(hoy);
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, meses: [], error: "" });

  const clave = `${mes}|${recarga}`;
  const cargando = resultado.clave !== clave;
  const primeraCarga = resultado.clave === null;

  useEffect(() => {
    let vigente = true;
    cargarPanel(mes).then((meses) => {
      if (!vigente) return;
      const ultimo = meses[meses.length - 1];
      setResultado({ clave, meses, error: ultimo.ok ? "" : ultimo.error?.message || "No se pudieron cargar los indicadores." });
    });
    return () => {
      vigente = false;
    };
  }, [clave, mes]);

  const elegirMes = (m) => {
    if (m && m <= hoy) setMes(m);
  };

  const meses = resultado.meses;
  const m = meses[meses.length - 1];
  const anterior = meses[meses.length - 2];
  const prev = anterior?.ok ? anterior : null;
  const listo = !primeraCarga && !resultado.error && m?.ok;

  const atendidas = listo
    ? m.citasPorEstado.filter((e) => ["FINALIZADA", "PAGADO"].includes(e.estado)).reduce((s, e) => s + e.total, 0)
    : 0;
  const maxTendencia = Math.max(1, ...meses.filter((x) => x.ok).map((x) => x.ingresos));
  const maxMetodo = listo ? Math.max(1, ...m.ingresosPorMetodo.map((x) => x.monto)) : 1;
  const estados = listo
    ? ORDEN_ESTADOS.map((estado) => ({ estado, total: m.citasPorEstado.find((e) => e.estado === estado)?.total ?? 0 })).concat(
        m.citasPorEstado.filter((e) => !ORDEN_ESTADOS.includes(e.estado))
      )
    : [];
  const maxEstado = Math.max(1, ...estados.map((e) => e.total));

  return (
    <section className="gestion ind">
      <div className="gestion-cabecera">
        <div>
          <h2>Indicadores del taller</h2>
          <p>
            Citas, ingresos, tiempos y satisfacción de <strong>{nombreMes(mes)}</strong>.
          </p>
        </div>
        <div className="ind-mes">
          <button type="button" className="btn-icono" onClick={() => elegirMes(sumarMeses(mes, -1))} aria-label="Mes anterior">
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
              <path d="m15 6-6 6 6 6" />
            </svg>
          </button>
          <label>
            <span className="sr-only">Mes</span>
            <input type="month" value={mes} max={hoy} onChange={(e) => elegirMes(e.target.value)} />
          </label>
          <button
            type="button"
            className="btn-icono"
            onClick={() => elegirMes(sumarMeses(mes, 1))}
            disabled={mes >= hoy}
            aria-label="Mes siguiente"
          >
            <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
              <path d="m9 6 6 6-6 6" />
            </svg>
          </button>
          <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)} disabled={cargando}>
            Actualizar
          </button>
        </div>
      </div>

      {resultado.error ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">No se pudieron cargar los indicadores</p>
            <p>{resultado.error}</p>
            <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
              Reintentar
            </button>
          </div>
        </div>
      ) : (
        <div className={cargando && !primeraCarga ? "ind-panel actualizando" : "ind-panel"} aria-busy={cargando}>
          {/* ---------- Indicadores principales ---------- */}
          <ul className="ind-kpis">
            <li>
              <span className="ind-kpi-titulo">Citas del mes</span>
              <strong className="ind-kpi-valor">{listo ? m.totalCitas : "—"}</strong>
              <span className="ind-kpi-sub">{listo ? `${atendidas} ${atendidas === 1 ? "atendida" : "atendidas"}` : " "}</span>
              {listo && <Variacion actual={m.totalCitas} anterior={prev?.totalCitas} mesAnterior={sumarMeses(mes, -1)} />}
            </li>
            <li>
              <span className="ind-kpi-titulo">Ingresos</span>
              <strong className="ind-kpi-valor">{listo ? formatoSoles(m.ingresos) : "—"}</strong>
              <span className="ind-kpi-sub">Pagos aprobados</span>
              {listo && <Variacion actual={m.ingresos} anterior={prev?.ingresos} mesAnterior={sumarMeses(mes, -1)} />}
            </li>
            <li>
              <span className="ind-kpi-titulo">Tiempo promedio de atención</span>
              <strong className="ind-kpi-valor">{listo ? (m.tiempoPromedioMinutos ? textoDuracion(m.tiempoPromedioMinutos) : "—") : "—"}</strong>
              <span className="ind-kpi-sub">Del inicio al fin de la orden</span>
              {listo && m.tiempoPromedioMinutos > 0 && (
                <Variacion
                  actual={m.tiempoPromedioMinutos}
                  anterior={prev?.tiempoPromedioMinutos}
                  mesAnterior={sumarMeses(mes, -1)}
                  menosEsMejor
                />
              )}
            </li>
            <li>
              <span className="ind-kpi-titulo">Satisfacción</span>
              <strong className="ind-kpi-valor ind-kpi-estrellas">
                {listo ? formatoPromedio(m.calificacionPromedio) : "—"}
                {listo && m.calificacionPromedio !== null && <Estrellas valor={m.calificacionPromedio} tamano={16} />}
              </strong>
              <span className="ind-kpi-sub">
                {listo
                  ? m.totalCalificaciones
                    ? `${m.totalCalificaciones} ${m.totalCalificaciones === 1 ? "calificación" : "calificaciones"}`
                    : "Sin calificaciones este mes"
                  : " "}
              </span>
            </li>
          </ul>

          <div className="ind-fila">
            {/* ---------- Ingresos de los últimos meses ---------- */}
            <section className="ind-tarjeta" aria-labelledby="ind-tendencia">
              <h3 id="ind-tendencia">Ingresos de los últimos {MESES_TENDENCIA} meses</h3>
              <p className="ind-ayuda">Haz clic en una columna para ver ese mes.</p>
              <div className="ind-columnas" role="list">
                {(primeraCarga ? Array.from({ length: MESES_TENDENCIA }, (_, i) => ({ mes: sumarMeses(mes, i - MESES_TENDENCIA + 1), ok: false })) : meses).map(
                  (x) => {
                    const alto = x.ok && x.ingresos > 0 ? Math.max(2, (x.ingresos / maxTendencia) * 100) : 0;
                    const actual = x.mes === mes;
                    return (
                      <div key={x.mes} className="ind-col" role="listitem">
                        <button
                          type="button"
                          className={actual ? "ind-col-btn actual" : "ind-col-btn"}
                          onClick={() => elegirMes(x.mes)}
                          aria-current={actual ? "true" : undefined}
                          aria-label={
                            x.ok
                              ? `${nombreMes(x.mes)}: ${formatoSoles(x.ingresos)} en ${x.totalCitas} citas`
                              : `${nombreMes(x.mes)}: sin datos`
                          }
                        >
                          <span className="ind-col-area">
                            {x.ok && actual && <span className="ind-col-valor">{formatoSolesCorto(x.ingresos)}</span>}
                            <span className="ind-col-barra" style={{ height: `${alto}%` }} />
                          </span>
                          <span className="ind-col-mes">{nombreMes(x.mes, true)}</span>
                          {x.ok && (
                            <span className="ind-tip" role="presentation">
                              <strong>{nombreMes(x.mes)}</strong>
                              {formatoSoles(x.ingresos)} · {x.totalCitas} {x.totalCitas === 1 ? "cita" : "citas"}
                            </span>
                          )}
                        </button>
                      </div>
                    );
                  }
                )}
              </div>
            </section>

            {/* ---------- Ingresos por método de pago ---------- */}
            <section className="ind-tarjeta" aria-labelledby="ind-metodos">
              <h3 id="ind-metodos">Ingresos por método de pago</h3>
              <p className="ind-ayuda">Pagos aprobados de {nombreMes(mes)}.</p>
              {listo && m.ingresos === 0 ? (
                <p className="ind-vacio">Aún no hay pagos aprobados este mes.</p>
              ) : (
                <ul className="ind-barras">
                  {(listo ? m.ingresosPorMetodo : []).map((x) => {
                    const pct = m.ingresos ? Math.round((x.monto / m.ingresos) * 100) : 0;
                    return (
                      <li key={x.metodo}>
                        <span className="ind-barra-nombre">{nombreMetodo(x.metodo)}</span>
                        <span className="ind-barra-pista" aria-hidden="true">
                          <span style={{ width: `${(x.monto / maxMetodo) * 100}%` }} />
                        </span>
                        <span className="ind-barra-valor">
                          {formatoSoles(x.monto)} <small>{pct} %</small>
                        </span>
                      </li>
                    );
                  })}
                  {listo && (
                    <li className="ind-total">
                      <span className="ind-barra-nombre">Total</span>
                      <span />
                      <span className="ind-barra-valor">{formatoSoles(m.ingresos)}</span>
                    </li>
                  )}
                </ul>
              )}
            </section>
          </div>

          {/* ---------- Citas por estado ---------- */}
          <section className="ind-tarjeta" aria-labelledby="ind-estados">
            <h3 id="ind-estados">Citas por estado</h3>
            <p className="ind-ayuda">Citas programadas en {nombreMes(mes)}, en el orden del flujo de atención.</p>
            {listo && m.totalCitas === 0 ? (
              <p className="ind-vacio">No hay citas programadas en este mes.</p>
            ) : (
              <ul className="ind-barras ind-estados">
                {estados.map((e) => (
                  <li key={e.estado} className={e.total ? "" : "cero"}>
                    <span className="ind-barra-nombre">{ESTADOS_CITA[e.estado] ?? e.estado}</span>
                    <span className="ind-barra-pista" aria-hidden="true">
                      <span style={{ width: `${(e.total / maxEstado) * 100}%` }} />
                    </span>
                    <span className="ind-barra-valor">
                      {e.total} <small>{m?.totalCitas ? Math.round((e.total / m.totalCitas) * 100) : 0} %</small>
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      )}
    </section>
  );
}

export default Indicadores;
