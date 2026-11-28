import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  listarHistorial,
  resumirHistorial,
  textoDuracion,
  hoyLima,
  PERIODOS,
  ESTADOS_HISTORIAL,
  fechaHora,
  fechaCorta,
} from "../../services/historialMecanicoService";
import { recordarOrden } from "../../services/ordenesService";
import { formatoPromedio } from "../../services/calificacionesService";
import Estrellas from "../../components/Estrellas";
import Paginacion from "../../components/admin/Paginacion";
import "../../components/admin/admin-ui.css";
import "./Historial.css";

/* T68.2 – Historial de servicios del mecánico (API de T68.1).
   - Órdenes terminadas (finalizadas o ya pagadas) filtradas por la fecha de fin
   - Periodos rápidos (este mes, 3 meses, este año, todo) o un rango de fechas propio
   - Resumen del periodo: servicios, tiempo promedio, calificación promedio y cobrados
   - Búsqueda por placa, cliente, servicio o ticket, y acceso a la orden en solo lectura */

const TAMANO = 10;
const PERIODO_INICIAL = "3meses";

const sinTildes = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();

function validarRango({ desde, hasta }, hoy) {
  if (desde && hasta && desde > hasta) return "La fecha «desde» no puede ser posterior a «hasta».";
  if (hasta && hasta > hoy) return "La fecha «hasta» no puede ser futura.";
  if (desde && desde > hoy) return "La fecha «desde» no puede ser futura.";
  return "";
}

function Historial() {
  const navigate = useNavigate();
  const hoy = hoyLima();
  const [periodo, setPeriodo] = useState(PERIODO_INICIAL);
  const [rango, setRango] = useState(() => PERIODOS.find((p) => p.valor === PERIODO_INICIAL).rango(hoy));
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, lista: [], error: "" });
  const [busqueda, setBusqueda] = useState("");
  const [pagina, setPagina] = useState(0);

  const errorRango = validarRango(rango, hoy);
  const clave = `${rango.desde}|${rango.hasta}|${recarga}`;
  const cargando = !errorRango && resultado.clave !== clave;
  const primeraCarga = resultado.clave === null;

  useEffect(() => {
    if (errorRango) return undefined;
    let vigente = true;
    listarHistorial(rango)
      .then((lista) => vigente && setResultado({ clave, lista, error: "" }))
      .catch((e) => vigente && setResultado({ clave, lista: [], error: e.message }));
    return () => {
      vigente = false;
    };
  }, [clave, rango, errorRango]);

  const elegirPeriodo = (p) => {
    setPeriodo(p.valor);
    setRango(p.rango(hoy));
    setPagina(0);
  };

  const cambiarFecha = (campo) => (e) => {
    setPeriodo("rango");
    setRango((r) => ({ ...r, [campo]: e.target.value }));
    setPagina(0);
  };

  const { lista } = resultado;
  const texto = sinTildes(busqueda.trim());
  const filtrados = texto
    ? lista.filter((o) => sinTildes(`${o.placa} ${o.cliente} ${o.servicio} ${o.ticket}`).includes(texto))
    : lista;
  const paginas = Math.max(1, Math.ceil(filtrados.length / TAMANO));
  const paginaActual = Math.min(pagina, paginas - 1);
  const visibles = filtrados.slice(paginaActual * TAMANO, (paginaActual + 1) * TAMANO);
  const resumen = resumirHistorial(lista);

  const verOrden = (o) => {
    recordarOrden(String(o.idCita), o.idOrden);
    navigate(`/mecanico/citas/${o.idCita}/orden`);
  };

  const textoPeriodo =
    periodo === "todo" || (!rango.desde && !rango.hasta)
      ? "desde el inicio"
      : `del ${fechaCorta(rango.desde) || "inicio"} al ${fechaCorta(rango.hasta || hoy)}`;

  return (
    <section className="gestion hm">
      <div className="gestion-cabecera">
        <div>
          <h2>Mi historial de servicios</h2>
          <p>Los mantenimientos que terminaste, {textoPeriodo}.</p>
        </div>
        <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)} disabled={cargando}>
          Actualizar
        </button>
      </div>

      {/* ---------- Periodo ---------- */}
      <div className="hm-periodo">
        <div className="segmentos" role="group" aria-label="Periodo">
          {PERIODOS.map((p) => (
            <button
              key={p.valor}
              type="button"
              className={periodo === p.valor ? "activo" : ""}
              aria-pressed={periodo === p.valor}
              onClick={() => elegirPeriodo(p)}
            >
              {p.texto}
            </button>
          ))}
        </div>
        <div className="hm-rango">
          <label>
            <span>Desde</span>
            <input type="date" value={rango.desde} max={rango.hasta || hoy} onChange={cambiarFecha("desde")} />
          </label>
          <label>
            <span>Hasta</span>
            <input type="date" value={rango.hasta} min={rango.desde || undefined} max={hoy} onChange={cambiarFecha("hasta")} />
          </label>
        </div>
      </div>
      {errorRango && (
        <p className="form-error-general hm-error" role="alert">
          {errorRango}
        </p>
      )}

      {/* ---------- Resumen del periodo ---------- */}
      {!errorRango && !resultado.error && (
        <ul className={cargando && !primeraCarga ? "hm-resumen actualizando" : "hm-resumen"} aria-label="Resumen del periodo">
          <li>
            <span>Servicios</span>
            <strong>{primeraCarga ? "—" : resumen.servicios}</strong>
          </li>
          <li>
            <span>Tiempo promedio</span>
            <strong>{primeraCarga ? "—" : textoDuracion(resumen.minutosPromedio)}</strong>
          </li>
          <li>
            <span>Calificación</span>
            <strong className="hm-resumen-estrellas">
              {formatoPromedio(primeraCarga ? null : resumen.promedioEstrellas)}
              {resumen.promedioEstrellas !== null && <Estrellas valor={resumen.promedioEstrellas} tamano={14} />}
            </strong>
            <small>{primeraCarga ? "" : `${resumen.calificados} de ${resumen.servicios} calificados`}</small>
          </li>
          <li>
            <span>Cobrados</span>
            <strong>{primeraCarga ? "—" : `${resumen.pagados} de ${resumen.servicios}`}</strong>
          </li>
        </ul>
      )}

      {/* ---------- Búsqueda ---------- */}
      <div className="filtros">
        <label className="buscador">
          <span className="sr-only">Buscar por placa, cliente, servicio o ticket</span>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input
            type="search"
            placeholder="Buscar por placa, cliente, servicio o ticket"
            value={busqueda}
            onChange={(e) => {
              setBusqueda(e.target.value);
              setPagina(0);
            }}
          />
        </label>
        {texto && !primeraCarga && !resultado.error && (
          <span className="hm-encontrados" role="status">
            {filtrados.length} de {lista.length}
          </span>
        )}
      </div>

      {/* ---------- Tabla ---------- */}
      {!errorRango && (
        <div className={cargando && !primeraCarga ? "tabla-caja actualizando" : "tabla-caja"} aria-busy={cargando}>
          {resultado.error ? (
            <div className="tabla-estado">
              <p className="tabla-estado-titulo">No se pudo cargar tu historial</p>
              <p>{resultado.error}</p>
              <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
                Reintentar
              </button>
            </div>
          ) : !primeraCarga && filtrados.length === 0 ? (
            <div className="tabla-estado">
              <p className="tabla-estado-titulo">{lista.length ? "Sin resultados" : "No hay servicios en este periodo"}</p>
              <p>
                {lista.length
                  ? `Ningún servicio coincide con «${busqueda.trim()}».`
                  : "Cuando completes un mantenimiento aparecerá aquí. Prueba con otro periodo."}
              </p>
            </div>
          ) : (
            <table className="tabla hm-tabla">
              <thead>
                <tr>
                  <th scope="col">Terminado</th>
                  <th scope="col">Servicio</th>
                  <th scope="col">Vehículo</th>
                  <th scope="col">Cliente</th>
                  <th scope="col">Duración</th>
                  <th scope="col">Estado</th>
                  <th scope="col">Calificación</th>
                  <th scope="col"><span className="sr-only">Acciones</span></th>
                </tr>
              </thead>
              <tbody>
                {primeraCarga
                  ? Array.from({ length: 5 }, (_, i) => (
                      <tr key={i} className="fila-esqueleto" aria-hidden="true">
                        <td><span className="esq" /></td>
                        <td><span className="esq esq-largo" /></td>
                        <td><span className="esq" /></td>
                        <td><span className="esq" /></td>
                        <td><span className="esq" /></td>
                        <td><span className="esq" /></td>
                        <td><span className="esq" /></td>
                        <td />
                      </tr>
                    ))
                  : visibles.map((o) => (
                      <tr key={o.idOrden}>
                        <td data-label="Terminado" className="hm-fecha">{fechaHora(o.fechaFin)}</td>
                        <td data-label="Servicio">
                          <div className="servicio-info">
                            <strong>{o.servicio}</strong>
                            <span>{o.ticket}</span>
                          </div>
                        </td>
                        <td data-label="Vehículo">
                          <span className="hm-placa">{o.placa || "—"}</span>
                        </td>
                        <td data-label="Cliente">{o.cliente || "—"}</td>
                        <td data-label="Duración" className="hm-duracion">{textoDuracion(o.minutos)}</td>
                        <td data-label="Estado">
                          <span className={`insignia hm-est-${o.estado}`}>{ESTADOS_HISTORIAL[o.estado]}</span>
                        </td>
                        <td data-label="Calificación">
                          {o.estrellas !== null ? (
                            <Estrellas valor={o.estrellas} tamano={14} />
                          ) : (
                            <span className="hm-sin-calificar">Sin calificar</span>
                          )}
                        </td>
                        <td data-label="Acciones" className="col-acciones">
                          <button
                            type="button"
                            className="btn-icono"
                            onClick={() => verOrden(o)}
                            aria-label={`Ver la orden de ${o.servicio} del ${fechaCorta(o.fechaFin)} (${o.placa})`}
                          >
                            <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                              <path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12Z" />
                              <circle cx="12" cy="12" r="2.8" />
                            </svg>
                            <span>Ver orden</span>
                          </button>
                        </td>
                      </tr>
                    ))}
              </tbody>
            </table>
          )}
        </div>
      )}

      {!resultado.error && !errorRango && (
        <Paginacion pagina={paginaActual} paginas={paginas} total={filtrados.length} tamano={TAMANO} alCambiar={setPagina} />
      )}
    </section>
  );
}

export default Historial;
