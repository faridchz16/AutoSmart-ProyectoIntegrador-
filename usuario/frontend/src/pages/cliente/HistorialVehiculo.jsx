import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { obtenerHistorial, formatoKm } from "../../services/vehiculosService";
import { ESTADOS_CITA_CLIENTE } from "../../services/citasClienteService";
import { minutosEntre } from "../../services/ordenesService";
import { diaSemana, mesCorto } from "../../services/reservaService";
import { formatoPrecio } from "../../services/serviciosService";
import { fechaHora, hoyLima } from "../../utils/fechas";
import "../../components/admin/admin-ui.css";
import "./MisCitas.css";
import "./Seguimiento.css";
import "./Vehiculos.css";
import "./HistorialVehiculo.css";

/* CLI-08 – Historial de mantenimiento de un vehículo (API de T39.1: GET /vehicles/{id}/history).
   - Resumen: cuántos mantenimientos tiene, el último servicio y hace cuánto fue
   - Línea de tiempo por año (del más reciente al más antiguo) con estado, mecánico, fechas, diagnóstico y repuestos
   - Cada atención abre su detalle (CLI-07) y, si está pagada, su comprobante (CLI-06)
   - Se puede imprimir (p. ej., para mostrarlo al vender el auto) */

const TERMINADOS = ["FINALIZADA", "PAGADO"];

const diasDesde = (f) => Math.round((new Date(`${hoyLima()}T12:00:00Z`) - new Date(`${f.slice(0, 10)}T12:00:00Z`)) / 864e5);
function haceCuanto(f) {
  const d = diasDesde(f);
  if (d <= 0) return "hoy";
  if (d === 1) return "ayer";
  if (d < 60) return `hace ${d} días`;
  const meses = Math.round(d / 30);
  return meses < 24 ? `hace ${meses} meses` : `hace ${Math.round(d / 365)} años`;
}
function duracion(desde, hasta) {
  const min = minutosEntre(desde, hasta);
  if (!Number.isFinite(min) || min < 0) return "";
  const h = Math.floor(min / 60);
  return h ? `${h} h ${min % 60} min` : `${min} min`;
}

function HistorialVehiculo() {
  const { id } = useParams();
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, datos: null, error: "", status: 0 });
  const [anio, setAnio] = useState("");

  const clave = `${id}|${recarga}`;
  useEffect(() => {
    let vigente = true;
    obtenerHistorial(id)
      .then((datos) => vigente && setResultado({ clave, datos, error: "", status: 0 }))
      .catch((e) => vigente && setResultado({ clave, datos: null, error: e.message, status: e.status ?? 0 }));
    return () => {
      vigente = false;
    };
  }, [id, clave]);

  const volver = (
    <Link to="/cliente/vehiculos" className="seg-volver">
      ← Mis vehículos
    </Link>
  );

  if (resultado.error || !resultado.datos) {
    const noEsTuyo = resultado.status === 403 || resultado.status === 404;
    return (
      <section className="gestion hist">
        {volver}
        <div className="tabla-caja">
          <div className="tabla-estado">
            {resultado.error ? (
              <>
                <p className="tabla-estado-titulo">{noEsTuyo ? "No encontramos este vehículo" : "No se pudo cargar el historial"}</p>
                <p>{noEsTuyo ? "Puede que el enlace esté mal o que el vehículo no sea de tu cuenta." : resultado.error}</p>
                {!noEsTuyo && (
                  <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
                    Reintentar
                  </button>
                )}
              </>
            ) : (
              <p>Cargando el historial...</p>
            )}
          </div>
        </div>
      </section>
    );
  }

  const { vehiculo, mantenimientos } = resultado.datos;
  const terminados = mantenimientos.filter((m) => TERMINADOS.includes(m.estado));
  const ultimo = terminados[0] ?? null;
  const enTaller = mantenimientos.find((m) => !TERMINADOS.includes(m.estado)) ?? null;
  const anios = [...new Set(mantenimientos.map((m) => m.inicio.slice(0, 4)))];
  const anioVisible = anios.includes(anio) ? anio : "";
  const visibles = mantenimientos.filter((m) => !anioVisible || m.inicio.startsWith(anioVisible));
  const porAnio = visibles.reduce((g, m) => {
    const a = m.inicio.slice(0, 4);
    (g[a] ??= []).push(m);
    return g;
  }, {});

  return (
    <section className="gestion hist">
      {volver}

      <div className="hist-cabecera">
        <span className="veh-placa">
          <span className="veh-placa-pais" aria-hidden="true">
            PERÚ
          </span>
          <span className="sr-only">Placa </span>
          {vehiculo.placa}
        </span>
        <div className="hist-vehiculo">
          <h2>
            {vehiculo.marca} {vehiculo.modelo}
          </h2>
          <p>
            {vehiculo.anio ? `Año ${vehiculo.anio} · ` : ""}
            {formatoKm(vehiculo.kilometraje)}
          </p>
        </div>
        <div className="hist-acciones">
          <button type="button" className="btn btn-sec" onClick={() => window.print()} disabled={!mantenimientos.length}>
            Imprimir historial
          </button>
          <Link to={`/cliente/reservar?vehiculo=${vehiculo.id}`} className="btn btn-pri">
            Reservar cita
          </Link>
        </div>
      </div>

      <h2 className="hist-solo-impresion">Historial de mantenimiento · AutoSmart</h2>

      <dl className="hist-resumen">
        <div>
          <dt>Mantenimientos</dt>
          <dd>{terminados.length}</dd>
        </div>
        <div>
          <dt>Último servicio</dt>
          <dd>{ultimo ? `${ultimo.servicio}` : "—"}</dd>
          {ultimo && <span>{`${fechaHora(ultimo.fin || ultimo.inicio).slice(0, 10)} (${haceCuanto(ultimo.fin || ultimo.inicio)})`}</span>}
        </div>
        <div>
          <dt>Ahora</dt>
          <dd>{enTaller ? "En el taller" : "Sin atenciones en curso"}</dd>
          {enTaller && (
            <Link to={`/cliente/citas/${enTaller.idCita}`} className="hist-ver">
              {ESTADOS_CITA_CLIENTE[enTaller.estado]?.texto ?? "En atención"} · ver avance →
            </Link>
          )}
        </div>
      </dl>

      {!mantenimientos.length ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">Este vehículo aún no tiene mantenimientos en AutoSmart</p>
            <p>Cuando el taller atienda tu {vehiculo.marca}, aquí quedará cada servicio con su diagnóstico y los trabajos realizados.</p>
            <Link to={`/cliente/reservar?vehiculo=${vehiculo.id}`} className="btn btn-pri">
              Reservar su primera cita
            </Link>
          </div>
        </div>
      ) : (
        <>
          {anios.length > 1 && (
            <div className="filtros">
              <div className="segmentos" role="group" aria-label="Filtrar por año">
                {["", ...anios].map((a) => (
                  <button key={a || "todos"} type="button" className={anioVisible === a ? "activo" : ""} aria-pressed={anioVisible === a} onClick={() => setAnio(a)}>
                    {a || "Todos"}
                  </button>
                ))}
              </div>
            </div>
          )}

          {Object.entries(porAnio)
            .sort(([a], [b]) => b.localeCompare(a))
            .map(([a, lista]) => (
              <section key={a} className="hist-anio" aria-labelledby={`hist-${a}`}>
                <h3 id={`hist-${a}`}>
                  {a} <span>{lista.length === 1 ? "1 atención" : `${lista.length} atenciones`}</span>
                </h3>
                <ol className="hist-lista">
                  {lista.map((m) => {
                    const estado = ESTADOS_CITA_CLIENTE[m.estado];
                    return (
                      <li key={m.idOrden} className="hist-item">
                        <div className="cit-fecha" aria-hidden="true">
                          <span>{diaSemana(m.inicio.slice(0, 10)).slice(0, 3)}</span>
                          <strong>{Number(m.inicio.slice(8, 10))}</strong>
                          <span>{mesCorto(m.inicio.slice(0, 10))}</span>
                        </div>
                        <div className="hist-cuerpo">
                          <div className="cit-cab">
                            <h4>{m.servicio}</h4>
                            <span className={`cit-estado est-${m.estado}`}>{estado?.texto ?? m.estado}</span>
                          </div>
                          <dl className="cit-datos">
                            <div>
                              <dt>Mecánico</dt>
                              <dd>{m.mecanico}</dd>
                            </div>
                            <div>
                              <dt>Recibido</dt>
                              <dd>{fechaHora(m.inicio)}</dd>
                            </div>
                            <div>
                              <dt>Entregado</dt>
                              <dd>{m.fin ? `${fechaHora(m.fin).slice(11)} · ${duracion(m.inicio, m.fin)}` : "En curso"}</dd>
                            </div>
                            <div>
                              <dt>Código</dt>
                              <dd>{m.ticket}</dd>
                            </div>
                            {m.costoRepuestos > 0 && (
                              <div>
                                <dt>Repuestos</dt>
                                <dd>{formatoPrecio(m.costoRepuestos)}</dd>
                              </div>
                            )}
                          </dl>
                          {m.diagnostico && <p className="hist-diag">{m.diagnostico}</p>}
                          <div className="cit-acciones hist-botones">
                            <Link to={`/cliente/citas/${m.idCita}/detalle`} className="btn btn-sec">
                              Ver detalle
                            </Link>
                            {m.estado === "PAGADO" && (
                              <Link to={`/cliente/citas/${m.idCita}/pago`} className="btn btn-sec">
                                Comprobante
                              </Link>
                            )}
                            {m.estado === "FINALIZADA" && (
                              <Link to={`/cliente/citas/${m.idCita}/pago`} className="btn btn-pri">
                                Pagar servicio
                              </Link>
                            )}
                          </div>
                        </div>
                      </li>
                    );
                  })}
                </ol>
              </section>
            ))}
        </>
      )}
    </section>
  );
}

export default HistorialVehiculo;
