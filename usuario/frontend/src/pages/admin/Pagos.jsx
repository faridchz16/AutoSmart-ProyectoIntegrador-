import { useCallback, useEffect, useRef, useState } from "react";
import {
  listarPagos,
  listarMetodosPago,
  descargarComprobante,
  resumirPagos,
  ESTADOS_PAGO,
} from "../../services/pagosService";
import { nombreMetodo, formatoSoles } from "../../services/indicadoresService";
import { hoyLima, sumarDias, inicioDeMes, finDelMesAnterior, fechaHora, fechaCorta, validarRango } from "../../utils/fechas";
import Paginacion from "../../components/admin/Paginacion";
import Aviso from "../../components/admin/Aviso";
import "../../components/admin/admin-ui.css";
import "./Reservas.css";
import "./Pagos.css";

/* T70.2 – Supervisión de pagos del administrador (API de T70.1).
   - Filtros por periodo (este mes, mes anterior, 7 días o un rango propio), método y estado
   - Total aprobado del periodo, cuántos pagos se aprobaron o rechazaron y cuánto entró por cada método
   - Tabla con código, fecha, cliente, placa, método, estado y monto; búsqueda por código, ticket, cliente o placa
   - Descarga del comprobante PDF de los pagos aprobados */

const TAMANO = 10;

const PERIODOS = [
  { valor: "mes", texto: "Este mes", rango: (hoy) => ({ desde: inicioDeMes(hoy), hasta: hoy }) },
  { valor: "anterior", texto: "Mes anterior", rango: (hoy) => ({ desde: inicioDeMes(hoy, 1), hasta: finDelMesAnterior(hoy) }) },
  { valor: "7dias", texto: "Últimos 7 días", rango: (hoy) => ({ desde: sumarDias(hoy, -6), hasta: hoy }) },
  { valor: "rango", texto: "Rango de fechas", rango: null },
];

const ESTADOS = [
  { valor: "", texto: "Todos" },
  { valor: "APROBADO", texto: "Aprobados" },
  { valor: "RECHAZADO", texto: "Rechazados" },
];

const sinTildes = (s) => s.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();

function Pagos() {
  const hoy = hoyLima();
  const [periodo, setPeriodo] = useState("mes");
  const [rango, setRango] = useState(() => PERIODOS[0].rango(hoy));
  const [metodo, setMetodo] = useState("");
  const [estado, setEstado] = useState("");
  const [metodos, setMetodos] = useState([]);
  const [recarga, setRecarga] = useState(0);
  const [resultado, setResultado] = useState({ clave: null, datos: null, error: "" });
  const [busqueda, setBusqueda] = useState("");
  const [pagina, setPagina] = useState(0);
  const [descargando, setDescargando] = useState(null);
  const [aviso, setAviso] = useState(null);

  const contadorAvisos = useRef(0);
  const avisar = (tipo, texto) => {
    contadorAvisos.current += 1;
    setAviso({ tipo, texto, id: contadorAvisos.current });
  };
  const cerrarAviso = useCallback(() => setAviso(null), []);

  const errorRango = validarRango(rango, hoy);
  const clave = `${rango.desde}|${rango.hasta}|${metodo}|${estado}|${recarga}`;
  const cargando = !errorRango && resultado.clave !== clave;
  const primeraCarga = resultado.clave === null;

  useEffect(() => {
    let vigente = true;
    listarMetodosPago().then((m) => vigente && setMetodos(m));
    return () => {
      vigente = false;
    };
  }, []);

  useEffect(() => {
    if (errorRango) return undefined;
    let vigente = true;
    listarPagos({ ...rango, metodo, estado })
      .then((datos) => vigente && setResultado({ clave, datos, error: "" }))
      .catch((e) => vigente && setResultado({ clave, datos: null, error: e.message }));
    return () => {
      vigente = false;
    };
  }, [clave, rango, metodo, estado, errorRango]);

  const elegirPeriodo = (p) => {
    setPeriodo(p.valor);
    if (p.rango) setRango(p.rango(hoy));
    setPagina(0);
  };

  const cambiarFecha = (campo) => (e) => {
    setRango((r) => ({ ...r, [campo]: e.target.value }));
    setPagina(0);
  };

  const filtrar = (setter) => (valor) => {
    setter(valor);
    setPagina(0);
  };

  const bajarComprobante = async (pago) => {
    setDescargando(pago.id);
    try {
      await descargarComprobante(pago);
    } catch (e) {
      avisar("error", `No se pudo descargar el comprobante ${pago.codigo}: ${e.message}`);
    } finally {
      setDescargando(null);
    }
  };

  const datos = resultado.datos;
  const pagos = datos?.pagos ?? [];
  const texto = sinTildes(busqueda.trim());
  const filtrados = texto
    ? pagos.filter((p) => sinTildes(`${p.codigo} ${p.ticket} ${p.cliente} ${p.placa}`).includes(texto))
    : pagos;
  const paginas = Math.max(1, Math.ceil(filtrados.length / TAMANO));
  const paginaActual = Math.min(pagina, paginas - 1);
  const visibles = filtrados.slice(paginaActual * TAMANO, (paginaActual + 1) * TAMANO);
  const resumen = resumirPagos(pagos);
  const hayFiltros = metodo || estado;

  return (
    <section className="gestion pg">
      <div className="gestion-cabecera">
        <div>
          <h2>Pagos</h2>
          <p>
            Pagos de los servicios del {fechaCorta(datos?.desde || rango.desde)} al {fechaCorta(datos?.hasta || rango.hasta || hoy)}
            {hayFiltros && " (con filtros)"}.
          </p>
        </div>
        <div className="pg-acciones">
          <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)} disabled={cargando}>
            Actualizar
          </button>
        </div>
      </div>

      {/* ---------- Filtros ---------- */}
      <div className="filtros">
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
        {periodo === "rango" && (
          <div className="rs-fechas">
            <label>
              <span>Desde</span>
              <input type="date" value={rango.desde} max={rango.hasta || hoy} onChange={cambiarFecha("desde")} />
            </label>
            <label>
              <span>Hasta</span>
              <input type="date" value={rango.hasta} min={rango.desde || undefined} max={hoy} onChange={cambiarFecha("hasta")} />
            </label>
          </div>
        )}
      </div>

      <div className="filtros">
        <label className="rs-select">
          <span className="sr-only">Método de pago</span>
          <select value={metodo} onChange={(e) => filtrar(setMetodo)(e.target.value)}>
            <option value="">Todos los métodos</option>
            {metodos.map((m) => (
              <option key={m.codigo} value={m.codigo}>
                {m.nombre}
              </option>
            ))}
          </select>
        </label>
        <div className="segmentos" role="group" aria-label="Estado del pago">
          {ESTADOS.map((e) => (
            <button
              key={e.valor || "todos"}
              type="button"
              className={estado === e.valor ? "activo" : ""}
              aria-pressed={estado === e.valor}
              onClick={() => filtrar(setEstado)(e.valor)}
            >
              {e.texto}
            </button>
          ))}
        </div>
        <label className="buscador pg-buscador">
          <span className="sr-only">Buscar por código, ticket, cliente o placa</span>
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.5-3.5" />
          </svg>
          <input
            type="search"
            placeholder="Buscar por código, ticket, cliente o placa"
            value={busqueda}
            onChange={(e) => filtrar(setBusqueda)(e.target.value)}
          />
        </label>
      </div>

      {errorRango && (
        <p className="form-error-general pg-error" role="alert">
          {errorRango}
        </p>
      )}

      {/* ---------- Resumen ---------- */}
      {!errorRango && !resultado.error && (
        <div className={cargando && !primeraCarga ? "pg-resumen actualizando" : "pg-resumen"}>
          <div className="pg-total">
            <span>Total aprobado</span>
            <strong>{primeraCarga ? "—" : formatoSoles(datos.totalAprobado)}</strong>
            <small>
              {primeraCarga
                ? " "
                : `${datos.cantidad} ${datos.cantidad === 1 ? "pago" : "pagos"} · ${resumen.aprobados} ${
                    resumen.aprobados === 1 ? "aprobado" : "aprobados"
                  } · ${resumen.rechazados} ${resumen.rechazados === 1 ? "rechazado" : "rechazados"}`}
            </small>
          </div>
          <ul className="pg-metodos" aria-label="Aprobado por método">
            {(primeraCarga ? [] : resumen.porMetodo).map((m) => (
              <li key={m.metodo}>
                <span>{nombreMetodo(m.metodo)}</span>
                <strong>{formatoSoles(m.monto)}</strong>
              </li>
            ))}
            {!primeraCarga && resumen.porMetodo.length === 0 && <li className="pg-sin-metodos">Sin pagos aprobados</li>}
          </ul>
        </div>
      )}

      {/* ---------- Tabla ---------- */}
      {!errorRango && (
        <div className={cargando && !primeraCarga ? "tabla-caja actualizando" : "tabla-caja"} aria-busy={cargando}>
          {resultado.error ? (
            <div className="tabla-estado">
              <p className="tabla-estado-titulo">No se pudieron cargar los pagos</p>
              <p>{resultado.error}</p>
              <button type="button" className="btn btn-sec" onClick={() => setRecarga((n) => n + 1)}>
                Reintentar
              </button>
            </div>
          ) : !primeraCarga && filtrados.length === 0 ? (
            <div className="tabla-estado">
              <p className="tabla-estado-titulo">{pagos.length ? "Sin resultados" : "No hay pagos en este periodo"}</p>
              <p>
                {pagos.length
                  ? `Ningún pago coincide con «${busqueda.trim()}».`
                  : hayFiltros
                    ? "Prueba con otro método, estado o periodo."
                    : "Cuando los clientes paguen sus servicios, aparecerán aquí."}
              </p>
            </div>
          ) : (
            <table className="tabla pg-tabla">
              <thead>
                <tr>
                  <th scope="col">Pago</th>
                  <th scope="col">Cliente</th>
                  <th scope="col">Método</th>
                  <th scope="col">Estado</th>
                  <th scope="col" className="col-num">Monto</th>
                  <th scope="col"><span className="sr-only">Acciones</span></th>
                </tr>
              </thead>
              <tbody>
                {primeraCarga
                  ? Array.from({ length: 6 }, (_, i) => (
                      <tr key={i} className="fila-esqueleto" aria-hidden="true">
                        <td><span className="esq esq-largo" /></td>
                        <td><span className="esq esq-largo" /></td>
                        <td><span className="esq" /></td>
                        <td><span className="esq" /></td>
                        <td><span className="esq" /></td>
                        <td />
                      </tr>
                    ))
                  : visibles.map((p) => (
                      <tr key={p.id} className={p.estado === "RECHAZADO" ? "pg-rechazado" : undefined}>
                        <td data-label="Pago">
                          <div className="servicio-info">
                            <strong className="pg-codigo">{p.codigo}</strong>
                            <span>{fechaHora(p.fecha)}</span>
                          </div>
                        </td>
                        <td data-label="Cliente">
                          <div className="servicio-info">
                            <strong>{p.cliente || "—"}</strong>
                            <span className="pg-ticket">
                              {p.placa && <span className="pg-placa">{p.placa}</span>}
                              {p.ticket}
                            </span>
                          </div>
                        </td>
                        <td data-label="Método">
                          <div className="servicio-info">
                            <strong className="pg-metodo">{nombreMetodo(p.metodo)}</strong>
                            <span>{p.registradoPor ? `Cobró ${p.registradoPor}` : "En línea"}</span>
                          </div>
                        </td>
                        <td data-label="Estado">
                          <span className={`insignia pg-est-${p.estado}`}>{ESTADOS_PAGO[p.estado]}</span>
                        </td>
                        <td data-label="Monto" className="col-num">
                          <span className="precio pg-monto">{formatoSoles(p.monto)}</span>
                        </td>
                        <td data-label="Acciones" className="col-acciones">
                          {p.estado === "APROBADO" && (
                            <button
                              type="button"
                              className="btn-icono"
                              onClick={() => bajarComprobante(p)}
                              disabled={descargando === p.id}
                              aria-label={`Descargar el comprobante ${p.codigo}`}
                            >
                              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                                <path d="M12 4v11M7 10l5 5 5-5M5 20h14" />
                              </svg>
                              <span>{descargando === p.id ? "Descargando..." : "Comprobante"}</span>
                            </button>
                          )}
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

      <Aviso aviso={aviso} alCerrar={cerrarAviso} />
    </section>
  );
}

export default Pagos;
