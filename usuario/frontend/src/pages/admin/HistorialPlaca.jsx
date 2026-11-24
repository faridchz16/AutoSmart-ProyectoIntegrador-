import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { obtenerHistorialPlaca, normalizarPlaca } from "../../services/historialPlacaService";
import { ESTADOS_CITA } from "../../services/agendaService";
import { minutosEntre } from "../../services/ordenesService";
import { textoDuracion } from "../../services/historialMecanicoService";
import { formatoSoles } from "../../services/indicadoresService";
import { fechaHora, fechaCorta } from "../../utils/fechas";
import "../../components/admin/admin-ui.css";
import "./Reservas.css";
import "./HistorialPlaca.css";

/* T72.2 – Buscador del historial de mantenimiento por placa (API de T72.1, solo ADMIN).
   - Busca cualquier vehículo por su placa (sin distinguir mayúsculas) para atender reclamos y auditorías
   - Ficha del vehículo y su propietario, con el total de mantenimientos y de repuestos
   - Línea de tiempo de las órdenes: fechas, servicio, mecánico, duración, estado, diagnóstico y repuestos
   - La placa queda en la dirección (?placa=ABC-123) para compartir o volver a la búsqueda */

const MAX_PLACA = 10;

function Buscador({ inicial, cargando, alBuscar }) {
  const [texto, setTexto] = useState(inicial);
  const [errorTexto, setErrorTexto] = useState("");

  const buscar = (e) => {
    e.preventDefault();
    const placa = normalizarPlaca(texto);
    if (!placa) return setErrorTexto("Escribe la placa del vehículo.");
    if (placa.length < 5 || placa.length > MAX_PLACA || !/^[A-Z0-9-]+$/.test(placa)) {
      return setErrorTexto("Escribe una placa válida, por ejemplo ABC-123.");
    }
    setErrorTexto("");
    setTexto(placa);
    alBuscar(placa);
  };

  return (
    <form className="hp-busqueda" onSubmit={buscar} noValidate role="search">
      <label htmlFor="hp-placa" className="sr-only">
        Placa del vehículo
      </label>
      <div className="hp-campo">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M5 17h14l-1.5-6.5A2 2 0 0 0 15.6 9H8.4a2 2 0 0 0-1.9 1.5Z" />
          <path d="M3.5 17h17v2.5h-17ZM7 19.5V21M17 19.5V21M6.5 13.5h2M15.5 13.5h2" />
        </svg>
        <input
          id="hp-placa"
          autoFocus
          autoComplete="off"
          maxLength={MAX_PLACA}
          placeholder="ABC-123"
          value={texto}
          aria-invalid={!!errorTexto}
          aria-describedby={errorTexto ? "hp-placa-error" : undefined}
          onChange={(e) => {
            setTexto(e.target.value.toUpperCase());
            setErrorTexto("");
          }}
        />
      </div>
      <button type="submit" className="btn btn-pri" disabled={cargando}>
        {cargando && <span className="btn-spinner" aria-hidden="true" />}
        {cargando ? "Buscando..." : "Buscar"}
      </button>
      {errorTexto && (
        <p className="campo-error hp-error" id="hp-placa-error">
          {errorTexto}
        </p>
      )}
    </form>
  );
}

function HistorialPlaca() {
  const [parametros, setParametros] = useSearchParams();
  const placaBuscada = normalizarPlaca(parametros.get("placa"));
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, datos: null, error: "", noExiste: false });

  const clave = `${placaBuscada}|${recarga}`;
  const cargando = !!placaBuscada && resultado.clave !== clave;

  useEffect(() => {
    if (!placaBuscada) return undefined;
    let vigente = true;
    obtenerHistorialPlaca(placaBuscada)
      .then((datos) => vigente && setResultado({ clave, datos, error: "", noExiste: false }))
      .catch((e) => vigente && setResultado({ clave, datos: null, error: e.message, noExiste: e.status === 404 }));
    return () => {
      vigente = false;
    };
  }, [clave, placaBuscada]);

  const buscar = (placa) => {
    if (placa === placaBuscada) setRecarga((n) => n + 1);
    else setParametros({ placa });
  };

  const datos = resultado.clave === clave ? resultado.datos : null;
  const mantenimientos = datos?.mantenimientos ?? [];
  const totalRepuestos = mantenimientos.reduce((s, m) => s + m.costoRepuestos, 0);
  const ultimo = mantenimientos[0];

  return (
    <section className="gestion hp">
      <div className="gestion-cabecera">
        <div>
          <h2>Historial por placa</h2>
          <p>Consulta los mantenimientos de cualquier vehículo para atender reclamos y auditorías.</p>
        </div>
      </div>

      {/* ---------- Búsqueda (se reinicia cuando cambia la placa de la dirección) ---------- */}
      <Buscador key={placaBuscada} inicial={placaBuscada} cargando={cargando} alBuscar={buscar} />

      {/* ---------- Resultado ---------- */}
      {!placaBuscada ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">Busca un vehículo</p>
            <p>Escribe la placa tal como figura en la tarjeta de propiedad, con o sin guion.</p>
          </div>
        </div>
      ) : resultado.clave === clave && resultado.error ? (
        <div className="tabla-caja">
          <div className="tabla-estado">
            <p className="tabla-estado-titulo">{resultado.noExiste ? "Placa no registrada" : "No se pudo consultar el historial"}</p>
            <p>{resultado.error}</p>
            {!resultado.noExiste && (
              <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
                Reintentar
              </button>
            )}
          </div>
        </div>
      ) : !datos ? (
        <div className="tabla-caja" aria-busy="true">
          <div className="tabla-estado">
            <p>Buscando el historial de {placaBuscada}...</p>
          </div>
        </div>
      ) : (
        <div className="hp-resultado">
          {/* Ficha del vehículo */}
          <section className="hp-ficha" aria-label={`Vehículo ${datos.placa}`}>
            <div className="hp-ficha-cab">
              <span className="hp-placa">{datos.placa}</span>
              <div>
                <h3>
                  {[datos.marca, datos.modelo].filter(Boolean).join(" ") || "Vehículo"}
                  {datos.anio && <span> · {datos.anio}</span>}
                </h3>
                {datos.kilometraje !== null && datos.kilometraje !== undefined && (
                  <p>{Number(datos.kilometraje).toLocaleString("es-PE")} km registrados</p>
                )}
              </div>
            </div>
            <dl className="hp-datos">
              <div>
                <dt>Propietario</dt>
                <dd>
                  {datos.propietario.nombre || "—"}
                  {datos.propietario.correo && <span>{datos.propietario.correo}</span>}
                </dd>
              </div>
              <div>
                <dt>Mantenimientos</dt>
                <dd>{datos.totalMantenimientos}</dd>
              </div>
              <div>
                <dt>Repuestos (total)</dt>
                <dd>{formatoSoles(totalRepuestos)}</dd>
              </div>
              <div>
                <dt>Último servicio</dt>
                <dd>{ultimo ? fechaCorta(ultimo.fechaFin || ultimo.fechaInicio) : "—"}</dd>
              </div>
            </dl>
          </section>

          {/* Línea de tiempo */}
          {mantenimientos.length === 0 ? (
            <div className="tabla-caja">
              <div className="tabla-estado">
                <p className="tabla-estado-titulo">Sin mantenimientos</p>
                <p>Este vehículo está registrado, pero aún no tiene órdenes de trabajo en el taller.</p>
              </div>
            </div>
          ) : (
            <ol className="hp-linea" aria-label="Mantenimientos, del más reciente al más antiguo">
              {mantenimientos.map((m) => {
                const minutos = m.fechaInicio && m.fechaFin ? minutosEntre(m.fechaInicio, m.fechaFin) : null;
                return (
                  <li key={m.idOrden} className={`hp-item est-${m.estado}`}>
                    <span className="hp-punto" aria-hidden="true" />
                    <div className="hp-item-caja">
                      <div className="hp-item-cab">
                        <div>
                          <strong>{m.servicio}</strong>
                          <span>
                            {m.ticket} · Orden N.º {m.idOrden}
                          </span>
                        </div>
                        <span className={`insignia rs-est-${m.estado}`}>{ESTADOS_CITA[m.estado] ?? m.estado}</span>
                      </div>
                      <dl className="hp-item-datos">
                        <div>
                          <dt>Inicio</dt>
                          <dd>{fechaHora(m.fechaInicio)}</dd>
                        </div>
                        <div>
                          <dt>Fin</dt>
                          <dd>{m.fechaFin ? fechaHora(m.fechaFin) : "En curso"}</dd>
                        </div>
                        <div>
                          <dt>Duración</dt>
                          <dd>{textoDuracion(minutos)}</dd>
                        </div>
                        <div>
                          <dt>Mecánico</dt>
                          <dd>{m.mecanico || "—"}</dd>
                        </div>
                        <div>
                          <dt>Repuestos</dt>
                          <dd>{formatoSoles(m.costoRepuestos)}</dd>
                        </div>
                      </dl>
                      {m.diagnostico ? (
                        <p className="hp-diagnostico">
                          <span>Diagnóstico</span>
                          {m.diagnostico}
                        </p>
                      ) : (
                        <p className="hp-sin-diagnostico">Sin diagnóstico registrado.</p>
                      )}
                    </div>
                  </li>
                );
              })}
            </ol>
          )}
        </div>
      )}
    </section>
  );
}

export default HistorialPlaca;
